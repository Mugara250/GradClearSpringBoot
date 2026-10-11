import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router'
import { canRecordResults, useCurrentUser } from '../../auth/AuthContext'
import {
  Button,
  ConfirmDelete,
  ErrorNotice,
  Field,
  FormActions,
  Input,
  Modal,
  PageHeader,
  Select,
} from '../../components/ui'
import { StudentProgressView } from '../../features/StudentProgress'
import { useStudentProgress } from '../../features/useStudentProgress'
import { courseLabel, studentName } from '../../lib/format'
import {
  useCourses,
  useDeleteStudent,
  useDeleteTranscript,
  useSaveTranscript,
  useStaffMember,
  type TranscriptInput,
} from '../../lib/queries'
import type { ClearanceRequest, ClearanceStatus, CourseStatus, Transcript, UUID } from '../../lib/types'
import { StudentFormModal } from './StudentsPage'
import { ReviewDialog } from './ReviewQueuePage'

export function StudentDetailPage() {
  const { id } = useParams<{ id: UUID }>()
  const user = useCurrentUser()
  const isAdmin = user.role === 'ADMIN'
  const recordsResults = canRecordResults(user)
  const navigate = useNavigate()
  const progress = useStudentProgress(id!)
  const officer = useStaffMember(user.role === 'DEPARTMENT_OFFICER' ? user.staffId : null)

  const [editingStudent, setEditingStudent] = useState(false)
  const [deletingStudent, setDeletingStudent] = useState(false)
  const [transcript, setTranscript] = useState<Transcript | 'new' | null>(null)
  const [deletingTranscript, setDeletingTranscript] = useState<Transcript | null>(null)
  const [review, setReview] = useState<{ request: ClearanceRequest; decision: ClearanceStatus } | null>(null)
  const removeStudent = useDeleteStudent()
  const removeTranscript = useDeleteTranscript()

  const { student } = progress
  // mirrors the backend rule: officers review only their own departments' rows
  const canReview = (r: ClearanceRequest) => isAdmin || !!officer.data?.departmentIds.includes(r.departmentId)

  return (
    <>
      <Link to="/students" className="mb-3 inline-block text-sm font-semibold text-brand-700 hover:underline">
        ← All students
      </Link>
      <PageHeader
        title={student ? studentName(student) : 'Student'}
        subtitle={
          student && (
            <>
              {student.studentId}
              {student.email && <> · {student.email}</>}
              {student.phoneNumber && <> · {student.phoneNumber}</>}
            </>
          )
        }
        actions={
          isAdmin &&
          student && (
            <>
              <Button variant="secondary" onClick={() => setEditingStudent(true)}>
                Edit student
              </Button>
              <Button variant="danger" onClick={() => setDeletingStudent(true)}>
                Delete
              </Button>
            </>
          )
        }
      />

      <StudentProgressView
        progress={progress}
        transcriptToolbar={
          recordsResults && (
            <>
              <Button size="sm" variant="secondary" onClick={() => setTranscript('new')}>
                Add one entry
              </Button>
              <Button size="sm" onClick={() => navigate(`/results?student=${id}`)}>
                Record results
              </Button>
            </>
          )
        }
        // only the registrar (and admins) edit results; others just read them
        transcriptActions={
          recordsResults
            ? (t) => (
                <>
                  <Button size="sm" variant="ghost" onClick={() => setTranscript(t)}>
                    Edit
                  </Button>
                  {isAdmin && (
                    <Button size="sm" variant="ghost" className="text-red-700" onClick={() => setDeletingTranscript(t)}>
                      Delete
                    </Button>
                  )}
                </>
              )
            : undefined
        }
        clearanceActions={(r) => {
          // APPROVED is final on the server, so it never gets an action here
          if (!canReview(r) || r.status === 'APPROVED') return null
          return r.status === 'PENDING' ? (
            <div className="flex justify-end gap-2">
              <Button size="sm" variant="success" onClick={() => setReview({ request: r, decision: 'APPROVED' })}>
                Approve
              </Button>
              <Button size="sm" variant="danger" onClick={() => setReview({ request: r, decision: 'REJECTED' })}>
                Reject
              </Button>
            </div>
          ) : (
            <Button size="sm" variant="secondary" onClick={() => setReview({ request: r, decision: 'PENDING' })}>
              Reopen
            </Button>
          )
        }}
      />

      {editingStudent && student && <StudentFormModal student={student} onClose={() => setEditingStudent(false)} />}
      {deletingStudent && student && (
        <ConfirmDelete
          what={`student ${studentName(student)}`}
          pending={removeStudent.isPending}
          error={removeStudent.error}
          onClose={() => setDeletingStudent(false)}
          onConfirm={() => removeStudent.mutate(student.id, { onSuccess: () => navigate('/students') })}
        />
      )}
      {transcript && (
        <TranscriptFormModal studentId={id!} transcript={transcript === 'new' ? null : transcript} onClose={() => setTranscript(null)} />
      )}
      {deletingTranscript && (
        <ConfirmDelete
          what="this transcript entry"
          pending={removeTranscript.isPending}
          error={removeTranscript.error}
          onClose={() => setDeletingTranscript(null)}
          onConfirm={() => removeTranscript.mutate(deletingTranscript.id, { onSuccess: () => setDeletingTranscript(null) })}
        />
      )}
      {review && (
        <ReviewDialog
          request={review.request}
          decision={review.decision}
          studentLabel={studentName(student)}
          onClose={() => setReview(null)}
        />
      )}
      {progress.error === null && officer.error && <ErrorNotice error={officer.error} />}
    </>
  )
}

const COURSE_STATUSES: CourseStatus[] = ['PASSED', 'FAILED', 'INCOMPLETE']

function TranscriptFormModal({ studentId, transcript, onClose }: { studentId: UUID; transcript: Transcript | null; onClose: () => void }) {
  const save = useSaveTranscript()
  const courses = useCourses()
  const { register, handleSubmit, formState } = useForm<TranscriptInput>({
    defaultValues: transcript ?? { studentId, courseId: '', grade: undefined, status: 'PASSED', academicYear: '' },
  })
  const err = formState.errors

  const onSubmit = handleSubmit((values) =>
    save.mutate({ id: transcript?.id, body: { ...values, studentId, grade: Number(values.grade) } }, { onSuccess: onClose }),
  )

  return (
    <Modal title={transcript ? 'Edit transcript entry' : 'Add transcript entry'} onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <Field label="Course" error={err.courseId?.message}>
          {(id) => (
            <Select id={id} {...register('courseId', { required: 'Choose a course' })}>
              <option value="">Select…</option>
              {(courses.data ?? []).map((c) => (
                <option key={c.id} value={c.id}>
                  {courseLabel(c)} ({c.creditHours} cr)
                </option>
              ))}
            </Select>
          )}
        </Field>
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Grade" error={err.grade?.message}>
            {(id) => (
              <Input
                id={id}
                type="number"
                step="0.01"
                min={0}
                {...register('grade', { required: 'Required', min: { value: 0, message: 'Must be ≥ 0' } })}
              />
            )}
          </Field>
          <Field label="Status" error={err.status?.message}>
            {(id) => (
              <Select id={id} {...register('status', { required: true })}>
                {COURSE_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s.charAt(0) + s.slice(1).toLowerCase()}
                  </option>
                ))}
              </Select>
            )}
          </Field>
          <Field label="Academic year" error={err.academicYear?.message}>
            {(id) => <Input id={id} placeholder="2025-2026" {...register('academicYear', { required: 'Required' })} />}
          </Field>
        </div>
        <ErrorNotice error={save.error} />
        <FormActions onCancel={onClose} pending={save.isPending} />
      </form>
    </Modal>
  )
}
