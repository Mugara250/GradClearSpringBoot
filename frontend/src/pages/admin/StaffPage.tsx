import { useState } from 'react'
import { useForm } from 'react-hook-form'
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
  Td,
} from '../../components/ui'
import { blankToNull, byId, staffName } from '../../lib/format'
import { useDeleteStaff, useDepartments, useSaveStaff, useStaffList, type StaffInput } from '../../lib/queries'
import type { Staff } from '../../lib/types'

export function StaffPage() {
  const staff = useStaffList()
  const departments = byId(useDepartments().data)
  const remove = useDeleteStaff()
  const [editing, setEditing] = useState<Staff | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Staff | null>(null)

  return (
    <>
      <PageHeader
        title="Staff"
        subtitle="Staff records. Officers review clearance for the departments they're attached to."
        actions={<Button onClick={() => setEditing('new')}>New staff member</Button>}
      />
      <Card>
        {staff.isLoading && <Loading />}
        {staff.error && <ErrorNotice error={staff.error} onRetry={() => void staff.refetch()} />}
        {staff.data &&
          (staff.data.length === 0 ? (
            <EmptyState>No staff yet.</EmptyState>
          ) : (
            <Table head={['Name', 'Staff ID', 'Position', 'Departments', '']}>
              {[...staff.data]
                .sort((a, b) => a.lastName.localeCompare(b.lastName))
                .map((s) => (
                  <tr key={s.id}>
                    <Td className="font-semibold text-slate-900">{staffName(s)}</Td>
                    <Td className="text-slate-600">{s.staffId}</Td>
                    <Td className="text-slate-600">{s.position ?? '—'}</Td>
                    <Td className="text-slate-600">
                      {s.departmentIds.map((id) => departments.get(id)?.name ?? '?').join(', ') || '—'}
                    </Td>
                    <Td className="text-right whitespace-nowrap">
                      <Button size="sm" variant="ghost" onClick={() => setEditing(s)}>
                        Edit
                      </Button>
                      <Button size="sm" variant="ghost" className="text-red-700" onClick={() => setDeleting(s)}>
                        Delete
                      </Button>
                    </Td>
                  </tr>
                ))}
            </Table>
          ))}
      </Card>

      {editing && <StaffFormModal staff={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
      {deleting && (
        <ConfirmDelete
          what={`staff member ${staffName(deleting)}`}
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

function StaffFormModal({ staff, onClose }: { staff: Staff | null; onClose: () => void }) {
  const save = useSaveStaff()
  const departments = useDepartments()
  const { register, handleSubmit, formState } = useForm<StaffInput>({
    defaultValues: staff ?? { staffId: '', firstName: '', lastName: '', position: '', departmentIds: [] },
  })
  const err = formState.errors

  const onSubmit = handleSubmit((values) =>
    save.mutate(
      {
        id: staff?.id,
        body: { ...values, staffId: values.staffId.trim(), position: blankToNull(values.position), departmentIds: values.departmentIds ?? [] },
      },
      { onSuccess: onClose },
    ),
  )

  return (
    <Modal title={staff ? 'Edit staff member' : 'New staff member'} onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <Field label="Staff ID" error={err.staffId?.message} hint="They sign in with this.">
          {(id) => <Input id={id} {...register('staffId', { required: 'Required' })} />}
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="First name" error={err.firstName?.message}>
            {(id) => <Input id={id} {...register('firstName', { required: 'Required' })} />}
          </Field>
          <Field label="Last name" error={err.lastName?.message}>
            {(id) => <Input id={id} {...register('lastName', { required: 'Required' })} />}
          </Field>
        </div>
        <Field label="Position">{(id) => <Input id={id} {...register('position')} />}</Field>
        <fieldset>
          <legend className="mb-1 text-sm font-semibold text-slate-800">Clearance departments</legend>
          <div className="max-h-40 space-y-1 overflow-y-auto rounded-md p-2 ring-1 ring-slate-300">
            {(departments.data ?? []).map((d) => (
              <label key={d.id} className="flex items-center gap-2 text-sm">
                <input type="checkbox" value={d.id} className="accent-brand-700" {...register('departmentIds')} />
                {d.name}
              </label>
            ))}
            {departments.data?.length === 0 && <p className="text-sm text-slate-600">No departments exist yet.</p>}
          </div>
          <p className="mt-1 text-xs text-slate-600">An officer can review clearance for these departments.</p>
        </fieldset>
        <ErrorNotice error={save.error} />
        <FormActions onCancel={onClose} pending={save.isPending} />
      </form>
    </Modal>
  )
}
