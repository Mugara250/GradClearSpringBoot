import { useState } from 'react'
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
  PasswordInput,
  Select,
  Table,
  Tag,
  Td,
} from '../../components/ui'
import { blankToNull, byId, staffName, studentName } from '../../lib/format'
import { useDeleteUser, useSaveUser, useStaffList, useStudents, useUsers } from '../../lib/queries'
import { ROLE_LABEL, type Role, type User } from '../../lib/types'

/**
 * Login accounts. Creating them is ADMIN-only (there is no self sign-up).
 * Each account is tied to one Student (STUDENT role) or one Staff record
 * (DEPARTMENT_OFFICER / ADMIN). That record's studentId/staffId is what the
 * person types as their login ID.
 */
export function UsersPage() {
  const me = useCurrentUser()
  const users = useUsers()
  const students = byId(useStudents().data)
  const staff = byId(useStaffList().data)
  const remove = useDeleteUser()
  const [editing, setEditing] = useState<User | 'new' | null>(null)
  const [deleting, setDeleting] = useState<User | null>(null)

  const describe = (u: User) => {
    if (u.studentId) {
      const s = students.get(u.studentId)
      return { name: studentName(s), loginId: s?.studentId ?? '—' }
    }
    const s = u.staffId ? staff.get(u.staffId) : undefined
    return { name: staffName(s), loginId: s?.staffId ?? '—' }
  }

  return (
    <>
      <PageHeader
        title="Users"
        subtitle="Sign-in accounts and their roles. People sign in with the student or staff ID of the linked record."
        actions={<Button onClick={() => setEditing('new')}>New account</Button>}
      />
      <Card>
        {users.isLoading && <Loading />}
        {users.error && <ErrorNotice error={users.error} onRetry={() => void users.refetch()} />}
        {users.data &&
          (users.data.length === 0 ? (
            <EmptyState>No accounts yet.</EmptyState>
          ) : (
            <Table head={['Person', 'Login ID', 'Role', 'Google email', '']}>
              {users.data.map((u) => {
                const { name, loginId } = describe(u)
                return (
                  <tr key={u.id}>
                    <Td className="font-semibold text-slate-900">
                      {name} {u.id === me.id && <Tag>You</Tag>}
                    </Td>
                    <Td className="text-slate-600">{loginId}</Td>
                    <Td>{ROLE_LABEL[u.role]}</Td>
                    <Td className="text-slate-600">{u.email ?? '—'}</Td>
                    <Td className="text-right whitespace-nowrap">
                      <Button size="sm" variant="ghost" onClick={() => setEditing(u)}>
                        Edit
                      </Button>
                      {u.id !== me.id && (
                        <Button size="sm" variant="ghost" className="text-red-700" onClick={() => setDeleting(u)}>
                          Delete
                        </Button>
                      )}
                    </Td>
                  </tr>
                )
              })}
            </Table>
          ))}
      </Card>

      {editing && <UserFormModal user={editing === 'new' ? null : editing} allUsers={users.data ?? []} onClose={() => setEditing(null)} />}
      {deleting && (
        <ConfirmDelete
          what={`the account for ${describe(deleting).name}`}
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

interface UserForm {
  role: Role
  studentId: string
  staffId: string
  password: string
  email: string
}

function UserFormModal({ user, allUsers, onClose }: { user: User | null; allUsers: User[]; onClose: () => void }) {
  const me = useCurrentUser()
  const save = useSaveUser()
  const students = useStudents()
  const staff = useStaffList()
  const { register, handleSubmit, watch, formState } = useForm<UserForm>({
    defaultValues: {
      role: user?.role ?? 'STUDENT',
      studentId: user?.studentId ?? '',
      staffId: user?.staffId ?? '',
      password: '',
      email: user?.email ?? '',
    },
  })
  const role = watch('role')
  const isStudent = role === 'STUDENT'
  const isSelf = user?.id === me.id
  const err = formState.errors

  // one account per Student/Staff record: hide records another account already uses
  const takenStudents = new Set(allUsers.filter((u) => u.id !== user?.id).map((u) => u.studentId))
  const takenStaff = new Set(allUsers.filter((u) => u.id !== user?.id).map((u) => u.staffId))

  const onSubmit = handleSubmit((values) =>
    save.mutate(
      {
        id: user?.id,
        body: {
          role: values.role,
          studentId: isStudent ? values.studentId : null,
          staffId: isStudent ? null : values.staffId,
          // the backend ignores password on update; only sent on create
          password: user ? '' : values.password,
          email: blankToNull(values.email),
        },
      },
      { onSuccess: onClose },
    ),
  )

  return (
    <Modal title={user ? 'Edit account' : 'New account'} onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <Field label="Role">
          {(id) => (
            <Select id={id} {...register('role')}>
              <option value="STUDENT">Student</option>
              <option value="DEPARTMENT_OFFICER">Department officer</option>
              <option value="ADMIN">Administrator</option>
            </Select>
          )}
        </Field>
        {isSelf && role !== me.role && (
          <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-900 ring-1 ring-inset ring-amber-300">
            You're changing your own role. Your menus and access will switch to {ROLE_LABEL[role]} as soon as you save.
          </p>
        )}

        {isStudent ? (
          <Field label="Student record" error={err.studentId?.message} hint="Their registration number becomes the login ID.">
            {(id) => (
              <Select id={id} {...register('studentId', { validate: (v) => !isStudent || !!v || 'Choose a student' })}>
                <option value="">Select…</option>
                {(students.data ?? [])
                  .filter((s) => !takenStudents.has(s.id))
                  .map((s) => (
                    <option key={s.id} value={s.id}>
                      {studentName(s)} ({s.studentId})
                    </option>
                  ))}
              </Select>
            )}
          </Field>
        ) : (
          <Field label="Staff record" error={err.staffId?.message} hint="Their staff ID becomes the login ID.">
            {(id) => (
              <Select id={id} {...register('staffId', { validate: (v) => isStudent || !!v || 'Choose a staff member' })}>
                <option value="">Select…</option>
                {(staff.data ?? [])
                  .filter((s) => !takenStaff.has(s.id))
                  .map((s) => (
                    <option key={s.id} value={s.id}>
                      {staffName(s)} ({s.staffId})
                    </option>
                  ))}
              </Select>
            )}
          </Field>
        )}

        {user ? (
          <p className="text-xs text-slate-600">Passwords can't be changed from this screen.</p>
        ) : (
          <Field label="Initial password" error={err.password?.message}>
            {(id) => (
              <PasswordInput
                id={id}
                autoComplete="new-password"
                {...register('password', { required: 'Required', minLength: { value: 8, message: 'At least 8 characters' } })}
              />
            )}
          </Field>
        )}

        <Field
          label="Google account email (optional)"
          error={err.email?.message}
          hint="Lets this person use “Sign in with Google”. Must be the exact, verified Google address."
        >
          {(id) => <Input id={id} type="email" {...register('email')} />}
        </Field>

        <ErrorNotice error={save.error} />
        <FormActions onCancel={onClose} pending={save.isPending} label={user ? 'Save' : 'Create account'} />
      </form>
    </Modal>
  )
}
