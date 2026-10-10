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
  Table,
  Tag,
  Td,
  Textarea,
} from '../../components/ui'
import { blankToNull } from '../../lib/format'
import { useDeleteDepartment, useDepartments, useSaveDepartment, useStaffMember, type DepartmentInput } from '../../lib/queries'
import type { Department } from '../../lib/types'

/**
 * Clearance departments (Library, Finance, …). These are the offices a
 * clearance request goes to, not the academic departments in the Academic
 * structure. Admins manage all of them. An officer may only edit the ones
 * they belong to.
 */
export function DepartmentsPage() {
  const user = useCurrentUser()
  const isAdmin = user.role === 'ADMIN'
  const departments = useDepartments()
  const me = useStaffMember(isAdmin ? null : user.staffId)
  const remove = useDeleteDepartment()
  const [editing, setEditing] = useState<Department | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Department | null>(null)

  const mine = new Set(me.data?.departmentIds ?? [])
  const canEdit = (d: Department) => isAdmin || mine.has(d.id)

  return (
    <>
      <PageHeader
        title="Departments"
        subtitle="The offices every student must be cleared by before graduating."
        actions={isAdmin && <Button onClick={() => setEditing('new')}>New department</Button>}
      />
      <Card>
        {departments.isLoading && <Loading />}
        {departments.error && <ErrorNotice error={departments.error} onRetry={() => void departments.refetch()} />}
        {departments.data &&
          (departments.data.length === 0 ? (
            <EmptyState>No departments yet.</EmptyState>
          ) : (
            <Table head={['Name', 'Description', '']}>
              {[...departments.data]
                .sort((a, b) => a.name.localeCompare(b.name))
                .map((d) => (
                  <tr key={d.id}>
                    <Td className="font-semibold text-slate-900">
                      {d.name} {!isAdmin && mine.has(d.id) && <Tag>Yours</Tag>}
                    </Td>
                    <Td className="text-slate-600">{d.description ?? '—'}</Td>
                    <Td className="text-right whitespace-nowrap">
                      {canEdit(d) && (
                        <Button size="sm" variant="ghost" onClick={() => setEditing(d)}>
                          Edit
                        </Button>
                      )}
                      {isAdmin && (
                        <Button size="sm" variant="ghost" className="text-red-700" onClick={() => setDeleting(d)}>
                          Delete
                        </Button>
                      )}
                    </Td>
                  </tr>
                ))}
            </Table>
          ))}
      </Card>

      {editing && <DepartmentFormModal department={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
      {deleting && (
        <ConfirmDelete
          what={`department "${deleting.name}"`}
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

function DepartmentFormModal({ department, onClose }: { department: Department | null; onClose: () => void }) {
  const save = useSaveDepartment()
  const { register, handleSubmit, formState } = useForm<DepartmentInput>({
    defaultValues: department ?? { name: '', description: '' },
  })

  const onSubmit = handleSubmit((values) =>
    save.mutate(
      { id: department?.id, body: { name: values.name.trim(), description: blankToNull(values.description) } },
      { onSuccess: onClose },
    ),
  )

  return (
    <Modal title={department ? 'Edit department' : 'New department'} onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <Field label="Name" error={formState.errors.name?.message}>
          {(id) => <Input id={id} {...register('name', { required: 'Required' })} />}
        </Field>
        <Field label="Description">{(id) => <Textarea id={id} rows={3} {...register('description')} />}</Field>
        {!department && (
          <p className="text-xs text-slate-600">Students who initiate clearance after this will also need this department's approval.</p>
        )}
        <ErrorNotice error={save.error} />
        <FormActions onCancel={onClose} pending={save.isPending} />
      </form>
    </Modal>
  )
}
