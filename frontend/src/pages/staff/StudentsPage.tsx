import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
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
  Td,
} from '../../components/ui'
import { academicPath, blankToNull, byId, studentName } from '../../lib/format'
import { useAcademics, useDeleteStudent, useSaveStudent, useStudents, type StudentInput } from '../../lib/queries'
import type { Student } from '../../lib/types'

export function StudentsPage() {
  const user = useCurrentUser()
  const isAdmin = user.role === 'ADMIN'
  const students = useStudents()
  const academics = byId(useAcademics().data)
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState<Student | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Student | null>(null)
  const remove = useDeleteStudent()

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    const list = students.data ?? []
    if (!q) return list
    return list.filter((s) =>
      [s.studentId, s.firstName, s.lastName, `${s.firstName} ${s.lastName}`, s.email ?? '']
        .some((v) => v?.toLowerCase().includes(q)),
    )
  }, [students.data, search])

  return (
    <>
      <PageHeader
        title="Students"
        subtitle="Look up any student's transcript and clearance progress."
        actions={isAdmin && <Button onClick={() => setEditing('new')}>New student</Button>}
      />
      <Card>
        <div className="mb-4 max-w-sm">
          <Input
            type="search"
            placeholder="Search by name, registration number or email"
            aria-label="Search students"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        {students.isLoading && <Loading />}
        {students.error && <ErrorNotice error={students.error} onRetry={() => void students.refetch()} />}
        {students.data &&
          (filtered.length === 0 ? (
            <EmptyState>{search ? 'No students match your search.' : 'No students yet.'}</EmptyState>
          ) : (
            <Table head={['Name', 'Reg. number', 'Department', 'Email', ...(isAdmin ? [''] : [])]}>
              {filtered.map((s) => (
                <tr key={s.id} className="hover:bg-sky-50/50">
                  <Td>
                    <Link to={`/students/${s.id}`} className="font-semibold text-brand-700 hover:underline">
                      {studentName(s)}
                    </Link>
                  </Td>
                  <Td className="text-slate-600">{s.studentId}</Td>
                  <Td className="text-slate-600">{academicPath(s.academicId, academics)}</Td>
                  <Td className="text-slate-600">{s.email ?? '—'}</Td>
                  {isAdmin && (
                    <Td className="text-right whitespace-nowrap">
                      <Button size="sm" variant="ghost" onClick={() => setEditing(s)}>
                        Edit
                      </Button>
                      <Button size="sm" variant="ghost" className="text-red-700" onClick={() => setDeleting(s)}>
                        Delete
                      </Button>
                    </Td>
                  )}
                </tr>
              ))}
            </Table>
          ))}
      </Card>

      {editing && <StudentFormModal student={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
      {deleting && (
        <ConfirmDelete
          what={`student ${studentName(deleting)}`}
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

/** ADMIN-only create / edit. */
export function StudentFormModal({ student, onClose }: { student: Student | null; onClose: () => void }) {
  const save = useSaveStudent()
  const academics = useAcademics()
  const academicMap = byId(academics.data)
  // the backend only accepts students linked to a DEPARTMENT-level entry
  const departments = (academics.data ?? []).filter((a) => a.level === 'DEPARTMENT')

  const { register, handleSubmit, formState } = useForm<StudentInput>({
    defaultValues: student ?? { studentId: '', firstName: '', lastName: '', email: '', phoneNumber: '', academicId: '' },
  })

  const onSubmit = handleSubmit((values) =>
    save.mutate(
      {
        id: student?.id,
        body: { ...values, studentId: values.studentId.trim(), email: blankToNull(values.email), phoneNumber: blankToNull(values.phoneNumber) },
      },
      { onSuccess: onClose },
    ),
  )
  const err = formState.errors

  return (
    <Modal title={student ? 'Edit student' : 'New student'} onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <Field label="Registration number" error={err.studentId?.message} hint="The student signs in with this.">
          {(id) => <Input id={id} {...register('studentId', { required: 'Required' })} />}
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="First name" error={err.firstName?.message}>
            {(id) => <Input id={id} {...register('firstName', { required: 'Required' })} />}
          </Field>
          <Field label="Last name" error={err.lastName?.message}>
            {(id) => <Input id={id} {...register('lastName', { required: 'Required' })} />}
          </Field>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Email" error={err.email?.message} hint="Also lets them use Google sign-in.">
            {(id) => <Input id={id} type="email" {...register('email')} />}
          </Field>
          <Field label="Phone" error={err.phoneNumber?.message}>
            {(id) => <Input id={id} {...register('phoneNumber')} />}
          </Field>
        </div>
        <Field label="Academic department" error={err.academicId?.message}>
          {(id) => (
            <Select id={id} {...register('academicId', { required: 'Choose a department' })}>
              <option value="">Select…</option>
              {departments.map((a) => (
                <option key={a.id} value={a.id}>
                  {academicPath(a.id, academicMap)}
                </option>
              ))}
            </Select>
          )}
        </Field>
        <ErrorNotice error={save.error} />
        <FormActions onCancel={onClose} pending={save.isPending} />
      </form>
    </Modal>
  )
}
