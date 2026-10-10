import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useCurrentUser } from '../../auth/AuthContext'
import {
  Button,
  Card,
  ConfirmDelete,
  EmptyState,
  ErrorNotice,
  Field,
  FormActions,
  Input,
  Loading,
  Modal,
  PageHeader,
  Select,
  Table,
  Tag,
  Td,
} from '../../components/ui'
import { academicPath, byId } from '../../lib/format'
import { useAcademics, useCourses, useDeleteCourse, useSaveCourse, type CourseInput } from '../../lib/queries'
import type { Course, CourseScope } from '../../lib/types'

export function CoursesPage() {
  const user = useCurrentUser()
  const isAdmin = user.role === 'ADMIN'
  const courses = useCourses()
  const academics = byId(useAcademics().data)
  const remove = useDeleteCourse()
  const [scope, setScope] = useState<CourseScope | ''>('')
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState<Course | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Course | null>(null)

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    return (courses.data ?? [])
      .filter((c) => !scope || c.scope === scope)
      .filter((c) => !q || c.courseCode.toLowerCase().includes(q) || c.courseName.toLowerCase().includes(q))
      .sort((a, b) => a.courseCode.localeCompare(b.courseCode))
  }, [courses.data, scope, search])

  return (
    <>
      <PageHeader
        title="Courses"
        subtitle="GENERAL courses apply to everyone. MAJOR courses belong to specific academic entries."
        actions={<Button onClick={() => setEditing('new')}>New course</Button>}
      />
      <Card>
        <div className="mb-4 flex flex-wrap gap-3">
          <Input
            type="search"
            className="max-w-xs"
            placeholder="Search code or name"
            aria-label="Search courses"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <Select className="max-w-40" aria-label="Filter by scope" value={scope} onChange={(e) => setScope(e.target.value as CourseScope | '')}>
            <option value="">All scopes</option>
            <option value="GENERAL">General</option>
            <option value="MAJOR">Major</option>
          </Select>
        </div>
        {courses.isLoading && <Loading />}
        {courses.error && <ErrorNotice error={courses.error} onRetry={() => void courses.refetch()} />}
        {courses.data &&
          (filtered.length === 0 ? (
            <EmptyState>No courses found.</EmptyState>
          ) : (
            <Table head={['Code', 'Name', 'Credits', 'Scope', 'Offered to', '']}>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <Td className="font-semibold whitespace-nowrap text-slate-900">{c.courseCode}</Td>
                  <Td>{c.courseName}</Td>
                  <Td>{c.creditHours}</Td>
                  <Td>
                    <Tag>{c.scope === 'GENERAL' ? 'General' : 'Major'}</Tag>
                  </Td>
                  <Td className="text-slate-600">
                    {c.scope === 'GENERAL' ? 'Everyone' : c.academicIds.map((id) => academics.get(id)?.name ?? '?').join(', ')}
                  </Td>
                  <Td className="text-right whitespace-nowrap">
                    <Button size="sm" variant="ghost" onClick={() => setEditing(c)}>
                      Edit
                    </Button>
                    {isAdmin && (
                      <Button size="sm" variant="ghost" className="text-red-700" onClick={() => setDeleting(c)}>
                        Delete
                      </Button>
                    )}
                  </Td>
                </tr>
              ))}
            </Table>
          ))}
      </Card>

      {editing && <CourseFormModal course={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
      {deleting && (
        <ConfirmDelete
          what={`course ${deleting.courseCode}`}
          pending={remove.isPending}
          error={remove.error}
          onClose={() => {
            remove.reset()
            setDeleting(null)
          }}
          onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        />
      )}
    </>
  )
}

function CourseFormModal({ course, onClose }: { course: Course | null; onClose: () => void }) {
  const save = useSaveCourse()
  const academics = useAcademics()
  const academicMap = byId(academics.data)
  const courses = useCourses()
  const { register, handleSubmit, watch, formState } = useForm<CourseInput>({
    defaultValues: course ?? { courseCode: '', courseName: '', creditHours: 3, scope: 'GENERAL', academicIds: [] },
  })
  const scope = watch('scope')
  const err = formState.errors

  const onSubmit = handleSubmit((values) =>
    save.mutate(
      {
        id: course?.id,
        body: {
          ...values,
          courseCode: values.courseCode.trim(),
          creditHours: Number(values.creditHours),
          // the backend rejects GENERAL courses linked to academic entries
          academicIds: values.scope === 'GENERAL' ? [] : (values.academicIds ?? []),
        },
      },
      { onSuccess: onClose },
    ),
  )

  return (
    <Modal title={course ? 'Edit course' : 'New course'} onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div className="grid gap-4 sm:grid-cols-5">
          <div className="sm:col-span-2">
            <Field label="Code" error={err.courseCode?.message}>
              {(id) => (
                <Input
                  id={id}
                  {...register('courseCode', {
                    required: 'Required',
                    // same rule as the server (case-insensitive); the server
                    // still has the final say and answers 409 if this misses one
                    validate: (code) => {
                      const clash = (courses.data ?? []).find(
                        (c) => c.id !== course?.id && c.courseCode.trim().toLowerCase() === code.trim().toLowerCase(),
                      )
                      return !clash || `Course ${clash.courseCode} already exists (${clash.courseName})`
                    },
                  })}
                />
              )}
            </Field>
          </div>
          <div className="sm:col-span-3">
            <Field label="Name" error={err.courseName?.message}>
              {(id) => <Input id={id} {...register('courseName', { required: 'Required' })} />}
            </Field>
          </div>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Credit hours" error={err.creditHours?.message}>
            {(id) => <Input id={id} type="number" min={1} {...register('creditHours', { required: 'Required', min: { value: 1, message: 'At least 1' } })} />}
          </Field>
          <Field label="Scope">
            {(id) => (
              <Select id={id} {...register('scope')}>
                <option value="GENERAL">General (everyone)</option>
                <option value="MAJOR">Major (specific academic entries)</option>
              </Select>
            )}
          </Field>
        </div>
        {scope === 'MAJOR' && (
          <fieldset>
            <legend className="mb-1 text-sm font-semibold text-slate-800">Offered to</legend>
            <div className="max-h-48 space-y-1 overflow-y-auto rounded-md p-2 ring-1 ring-slate-300">
              {(academics.data ?? []).map((a) => (
                <label key={a.id} className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    value={a.id}
                    className="accent-brand-700"
                    {...register('academicIds', { validate: (v) => scope !== 'MAJOR' || (v?.length ?? 0) > 0 || 'Pick at least one' })}
                  />
                  {academicPath(a.id, academicMap)}
                </label>
              ))}
            </div>
            {err.academicIds && <p className="mt-1 text-xs text-red-700">{err.academicIds.message}</p>}
          </fieldset>
        )}
        <ErrorNotice error={save.error} />
        <FormActions onCancel={onClose} pending={save.isPending} />
      </form>
    </Modal>
  )
}
