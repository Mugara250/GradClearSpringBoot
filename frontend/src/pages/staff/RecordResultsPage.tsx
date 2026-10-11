import { useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { canRecordResults, useCurrentUser } from '../../auth/AuthContext'
import { Button, Card, EmptyState, ErrorNotice, Input, Loading, PageHeader, Select, Table, Tag, Td } from '../../components/ui'
import { api, errorMessage } from '../../lib/api'
import { academicPath, byId, studentName } from '../../lib/format'
import { keys, useAcademics, useCourses, useStudent, useStudents, useTranscriptsOf } from '../../lib/queries'
import type { Academic, Course, CourseStatus, Student, Transcript, UUID } from '../../lib/types'
import { AccessDeniedPage } from '../AccessDeniedPage'

/** Grades are percentages; at or above this a result defaults to PASSED. */
const PASS_MARK = 50

/** "2025-2026" style year the current date falls in (years start in September). */
function currentAcademicYear(): string {
  const now = new Date()
  const start = now.getMonth() >= 8 ? now.getFullYear() : now.getFullYear() - 1
  return `${start}-${start + 1}`
}

/**
 * The registrar's results screen. In a full deployment transcripts would
 * come from the student portal / course registration system. Here the
 * registrar enters final results for every course offered to a student in
 * one sheet, which is also how test data is prepared.
 */
export function RecordResultsPage() {
  const user = useCurrentUser()
  const [params, setParams] = useSearchParams()
  const studentId = params.get('student')

  if (!canRecordResults(user)) return <AccessDeniedPage />

  return (
    <>
      <PageHeader
        title="Record results"
        subtitle="Enter final course results for a student, as the Academic Registrar does at the end of each semester. Passed credits count towards graduation clearance."
      />
      <div className="space-y-6">
        <Card title="Student">
          <StudentPicker value={studentId} onChange={(id) => setParams(id ? { student: id } : {})} />
        </Card>
        {studentId && <ResultsLoader key={studentId} studentId={studentId} />}
      </div>
    </>
  )
}

function StudentPicker({ value, onChange }: { value: UUID | null; onChange: (id: UUID | null) => void }) {
  const students = useStudents()
  const [search, setSearch] = useState('')

  const options = useMemo(() => {
    const q = search.trim().toLowerCase()
    return [...(students.data ?? [])]
      .filter((s) => !q || `${s.firstName} ${s.lastName} ${s.studentId}`.toLowerCase().includes(q) || s.id === value)
      .sort((a, b) => studentName(a).localeCompare(studentName(b)))
  }, [students.data, search, value])

  if (students.isLoading) return <Loading />
  if (students.error) return <ErrorNotice error={students.error} />
  if (!students.data?.length) return <EmptyState>No students yet. Create one on the Students page first.</EmptyState>

  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <Input
        type="search"
        placeholder="Search by name or registration number"
        aria-label="Search students"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      <Select aria-label="Student" value={value ?? ''} onChange={(e) => onChange(e.target.value || null)}>
        <option value="">Choose a student…</option>
        {options.map((s) => (
          <option key={s.id} value={s.id}>
            {studentName(s)} ({s.studentId})
          </option>
        ))}
      </Select>
    </div>
  )
}

/** Loads everything the sheet needs, then mounts it with that data as its starting point. */
function ResultsLoader({ studentId }: { studentId: UUID }) {
  const student = useStudent(studentId)
  const academics = useAcademics()
  const courses = useCourses()
  const transcripts = useTranscriptsOf(studentId)
  const [savedNote, setSavedNote] = useState<string | null>(null)
  // bumped after each save, so the sheet restarts from what is now stored;
  // background refetches don't touch it, so unsaved grades are never lost
  const [version, setVersion] = useState(0)

  const queries = [student, academics, courses, transcripts]
  const error = queries.find((q) => q.error)?.error
  if (error) return <ErrorNotice error={error} />
  if (!student.data || !academics.data || !courses.data || !transcripts.data) return <Loading />

  return (
    <>
      {savedNote && (
        <p className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
          {savedNote}{' '}
          <Link to={`/students/${studentId}`} className="font-bold underline">
            View student
          </Link>
        </p>
      )}
      <ResultsSheet
        key={version}
        student={student.data}
        academics={academics.data}
        courses={courses.data}
        transcripts={transcripts.data}
        onSaved={(note) => {
          setSavedNote(note)
          setVersion((v) => v + 1)
        }}
      />
    </>
  )
}

interface Row {
  grade: string
  status: CourseStatus
}

function ResultsSheet({
  student,
  academics,
  courses,
  transcripts,
  onSaved,
}: {
  student: Student
  academics: Academic[]
  courses: Course[]
  transcripts: Transcript[]
  onSaved: (note: string) => void
}) {
  const queryClient = useQueryClient()
  const academicMap = byId(academics)
  const department = academicMap.get(student.academicId)
  const requiredCredits = department?.requiredCredits ?? 0

  // the student's department and the faculty/program above it: MAJOR courses
  // linked to any of these are offered to them
  const lineage = new Set<UUID>()
  for (let a = department; a && !lineage.has(a.id); a = a.parentId ? academicMap.get(a.parentId) : undefined) lineage.add(a.id)

  // latest recorded result per course (a retake replaces an earlier attempt here)
  const recorded = new Map<UUID, Transcript>()
  for (const t of [...transcripts].sort((a, b) => a.academicYear.localeCompare(b.academicYear))) recorded.set(t.courseId, t)

  const offered = courses
    .filter((c) => c.scope === 'GENERAL' || c.academicIds.some((id) => lineage.has(id)) || recorded.has(c.id))
    .sort((a, b) => a.scope.localeCompare(b.scope) || a.courseCode.localeCompare(b.courseCode))

  const initial = (): Record<UUID, Row> =>
    Object.fromEntries(
      offered.map((c) => {
        const t = recorded.get(c.id)
        return [c.id, t ? { grade: String(t.grade), status: t.status } : { grade: '', status: 'PASSED' as CourseStatus }]
      }),
    )

  const [rows, setRows] = useState<Record<UUID, Row>>(initial)
  // a course created while the sheet is open has no row yet
  const rowOf = (courseId: UUID): Row => rows[courseId] ?? { grade: '', status: 'PASSED' }
  const [year, setYear] = useState(currentAcademicYear)
  const [saving, setSaving] = useState<{ done: number; total: number } | null>(null)
  const [failures, setFailures] = useState<string[]>([])

  const gradeError = (g: string) => {
    if (g.trim() === '') return null
    const n = Number(g)
    return Number.isFinite(n) && n >= 0 && n <= 100 ? null : 'Enter 0–100'
  }

  const isChanged = (c: Course) => {
    const row = rowOf(c.id)
    const t = recorded.get(c.id)
    if (!t) return row.grade.trim() !== ''
    return Number(row.grade) !== t.grade || row.status !== t.status
  }

  const changed = offered.filter(isChanged)
  const invalid = offered.some((c) => gradeError(rowOf(c.id).grade))
  const yearValid = /^\d{4}-\d{4}$/.test(year.trim())

  const projectedCredits = offered
    .filter((c) => rowOf(c.id).grade.trim() !== '' && rowOf(c.id).status === 'PASSED')
    .reduce((sum, c) => sum + c.creditHours, 0)

  const setGrade = (courseId: UUID, grade: string) =>
    setRows((prev) => {
      const n = Number(grade)
      const current = prev[courseId]?.status
      // typing a grade sets the status from the pass mark; it can still be changed after
      const status: CourseStatus = grade.trim() === '' || !Number.isFinite(n) ? (current ?? 'PASSED') : n >= PASS_MARK ? 'PASSED' : 'FAILED'
      return { ...prev, [courseId]: { grade, status: status ?? 'PASSED' } }
    })

  const setStatus = (courseId: UUID, status: CourseStatus) =>
    setRows((prev) => ({ ...prev, [courseId]: { grade: prev[courseId]?.grade ?? '', status } }))

  // simulation helper: give every course without a result a passing grade
  const fillPassing = () =>
    setRows((prev) => {
      const next = { ...prev }
      for (const c of offered) {
        if ((next[c.id]?.grade ?? '').trim() === '') next[c.id] = { grade: String(60 + Math.floor(Math.random() * 36)), status: 'PASSED' }
      }
      return next
    })

  const save = async () => {
    setFailures([])
    setSaving({ done: 0, total: changed.length })
    const errors: string[] = []
    let saved = 0
    for (const c of changed) {
      const row = rowOf(c.id)
      const existing = recorded.get(c.id)
      const body = {
        grade: Number(row.grade),
        status: row.status,
        // a corrected result keeps the year it was originally recorded in
        academicYear: existing?.academicYear ?? year.trim(),
        studentId: student.id,
        courseId: c.id,
      }
      try {
        if (existing) await api.put(`/api/v1/transcript/${existing.id}`, body)
        else await api.post('/api/v1/transcript', body)
        saved++
      } catch (e) {
        errors.push(`${c.courseCode}: ${errorMessage(e)}`)
      }
      setSaving({ done: saved + errors.length, total: changed.length })
    }
    setSaving(null)
    setFailures(errors)
    if (saved > 0) {
      // refetch first, so the remounted sheet starts from the saved results
      await queryClient.invalidateQueries({ queryKey: keys.transcripts })
      onSaved(`Saved ${saved} result${saved === 1 ? '' : 's'} for ${studentName(student)}.`)
    }
  }

  const general = offered.filter((c) => c.scope === 'GENERAL')
  const major = offered.filter((c) => c.scope !== 'GENERAL')

  const renderRows = (list: Course[]) =>
    list.map((c) => {
      const row = rowOf(c.id)
      const t = recorded.get(c.id)
      const err = gradeError(row.grade)
      return (
        <tr key={c.id} className={isChanged(c) ? 'bg-sky-50/70' : undefined}>
          <Td>
            <span className="font-bold whitespace-nowrap text-slate-900">{c.courseCode}</span>
            <span className="ml-2 text-slate-700">{c.courseName}</span>
          </Td>
          <Td>{c.creditHours}</Td>
          <Td>
            <Input
              type="number"
              min={0}
              max={100}
              step="0.5"
              inputMode="decimal"
              aria-label={`Grade for ${c.courseCode}`}
              aria-invalid={!!err}
              className={`w-24 ${err ? 'ring-red-400' : ''}`}
              placeholder="—"
              value={row.grade}
              onChange={(e) => setGrade(c.id, e.target.value)}
            />
            {err && <p className="mt-1 text-xs text-red-700">{err}</p>}
          </Td>
          <Td>
            <Select
              aria-label={`Status for ${c.courseCode}`}
              className="w-36"
              value={row.status}
              disabled={row.grade.trim() === ''}
              onChange={(e) => setStatus(c.id, e.target.value as CourseStatus)}
            >
              <option value="PASSED">Passed</option>
              <option value="FAILED">Failed</option>
              <option value="INCOMPLETE">Incomplete</option>
            </Select>
          </Td>
          <Td className="whitespace-nowrap text-slate-600">
            {t ? `${t.grade} · ${t.status.charAt(0) + t.status.slice(1).toLowerCase()} · ${t.academicYear}` : 'Not recorded'}
          </Td>
        </tr>
      )
    })

  return (
    <Card
      title={`${studentName(student)} · ${academicPath(student.academicId, academicMap)}`}
      actions={
        <>
          <Button size="sm" variant="secondary" onClick={fillPassing} title="Simulation helper: fills every empty grade with a random passing mark (60–95)">
            Fill blanks with passing grades
          </Button>
          <Button size="sm" variant="ghost" onClick={() => setRows(initial())} disabled={changed.length === 0}>
            Discard changes
          </Button>
        </>
      }
    >
      <div className="mb-4 flex flex-wrap items-end gap-4">
        <div>
          <label htmlFor="academic-year" className="mb-1 block text-sm font-semibold text-slate-800">
            Academic year for new results
          </label>
          <Input id="academic-year" className="w-40" value={year} onChange={(e) => setYear(e.target.value)} placeholder="2025-2026" />
          {!yearValid && <p className="mt-1 text-xs text-red-700">Use the form 2025-2026</p>}
        </div>
        <p className="text-sm text-slate-600">
          Grades are out of 100. {PASS_MARK} or more is set to <em>Passed</em>, below is <em>Failed</em>. Leave a grade empty to skip that course.
        </p>
      </div>

      {offered.length === 0 ? (
        <EmptyState>No courses are offered to this student's department yet. Add courses on the Courses page.</EmptyState>
      ) : (
        <div className="space-y-6">
          {general.length > 0 && (
            <div>
              <h3 className="mb-2 flex items-center gap-2 text-sm font-bold">
                <Tag>General</Tag> Courses every student takes
              </h3>
              <Table head={['Course', 'Credits', 'Grade', 'Status', 'Currently recorded']}>{renderRows(general)}</Table>
            </div>
          )}
          {major.length > 0 && (
            <div>
              <h3 className="mb-2 flex items-center gap-2 text-sm font-bold">
                <Tag>Major</Tag> Courses for {department?.name ?? 'their department'}
              </h3>
              <Table head={['Course', 'Credits', 'Grade', 'Status', 'Currently recorded']}>{renderRows(major)}</Table>
            </div>
          )}
        </div>
      )}

      {failures.length > 0 && (
        <div className="mt-4">
          <ErrorNotice error={new Error(`${failures.length} result(s) could not be saved: ${failures.join('; ')}`)} />
        </div>
      )}

      <div className="sticky bottom-0 -mx-5 -mb-5 mt-6 flex flex-wrap items-center justify-between gap-3 rounded-b-xl border-t border-slate-200 bg-white/95 px-5 py-3 backdrop-blur">
        <p className="text-sm">
          Passed credits after saving:{' '}
          <span className={`text-base font-bold ${projectedCredits >= requiredCredits ? 'text-emerald-700' : 'text-amber-700'}`}>
            {projectedCredits} / {requiredCredits}
          </span>
          {projectedCredits >= requiredCredits && requiredCredits > 0 && <span className="ml-2 text-emerald-700">meets the requirement</span>}
        </p>
        <Button onClick={save} loading={saving !== null} disabled={changed.length === 0 || invalid || !yearValid}>
          {saving
            ? `Saving ${saving.done}/${saving.total}…`
            : changed.length === 0
              ? 'No changes to save'
              : `Save ${changed.length} result${changed.length === 1 ? '' : 's'}`}
        </Button>
      </div>
    </Card>
  )
}
