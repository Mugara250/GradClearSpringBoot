import { useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { useCurrentUser } from '../../auth/AuthContext'
import {
  Button,
  Card,
  EmptyState,
  ErrorNotice,
  Field,
  Loading,
  Modal,
  PageHeader,
  Table,
  Td,
  Textarea,
} from '../../components/ui'
import { blankToNull, byId, formatDate, studentName } from '../../lib/format'
import {
  useAllClearanceRequests,
  useDepartments,
  usePendingFor,
  useStaffMember,
  useStudents,
  useUpdateClearanceStatus,
} from '../../lib/queries'
import type { ClearanceRequest, ClearanceStatus, Department, UUID } from '../../lib/types'

/**
 * Clearance requests waiting on a department. Officers see only the
 * departments their Staff record is attached to (the backend refuses the rest
 * with 403). Admins can pick any department.
 */
export function ReviewQueuePage() {
  const user = useCurrentUser()
  const departments = useDepartments()
  const me = useStaffMember(user.role === 'DEPARTMENT_OFFICER' ? user.staffId : null)

  const available: Department[] =
    user.role === 'ADMIN'
      ? (departments.data ?? [])
      : (departments.data ?? []).filter((d) => me.data?.departmentIds.includes(d.id))

  const [selected, setSelected] = useState<UUID | null>(null)
  const departmentId = selected && available.some((d) => d.id === selected) ? selected : (available[0]?.id ?? null)

  const loading = departments.isLoading || me.isLoading
  const error = departments.error ?? me.error

  return (
    <>
      <PageHeader
        title="Review queue"
        subtitle={
          user.role === 'ADMIN'
            ? 'Clearance requests by department.'
            : 'Clearance requests waiting on your department. Approval is final and cannot be undone.'
        }
      />
      {loading && <Loading />}
      {error && <ErrorNotice error={error} />}
      {!loading && !error && available.length === 0 && (
        <EmptyState>
          {user.role === 'ADMIN'
            ? 'No departments exist yet.'
            : 'Your staff record is not attached to any department, so there is nothing for you to review. Ask an administrator to assign you.'}
        </EmptyState>
      )}

      {departmentId && (
        <>
          {available.length > 1 && (
            <div className="mb-4 flex flex-wrap gap-2" role="tablist">
              {available.map((d) => (
                <button
                  key={d.id}
                  role="tab"
                  aria-selected={d.id === departmentId}
                  onClick={() => setSelected(d.id)}
                  className={`rounded-full px-3 py-1.5 text-sm font-semibold ring-1 ring-inset ${
                    d.id === departmentId ? 'bg-brand-700 text-white ring-brand-700' : 'bg-white text-brand-800 ring-brand-200 hover:bg-sky-50'
                  }`}
                >
                  {d.name}
                </button>
              ))}
            </div>
          )}
          <DepartmentQueue departmentId={departmentId} />
        </>
      )}
    </>
  )
}

function DepartmentQueue({ departmentId }: { departmentId: UUID }) {
  const pending = usePendingFor(departmentId)
  const all = useAllClearanceRequests()
  const students = byId(useStudents().data)
  const [review, setReview] = useState<{ request: ClearanceRequest; decision: ClearanceStatus } | null>(null)

  const rejected = (all.data ?? []).filter((r) => r.departmentId === departmentId && r.status === 'REJECTED')

  const rows = (requests: ClearanceRequest[], actions: (r: ClearanceRequest) => ReactNode) => (
    <Table head={['Student', 'Reg. number', 'Requested', 'Remarks', '']}>
      {requests.map((r) => {
        const student = students.get(r.studentId)
        return (
          <tr key={r.id}>
            <Td>
              <Link to={`/students/${r.studentId}`} className="font-semibold text-brand-700 hover:underline">
                {studentName(student)}
              </Link>
            </Td>
            <Td className="text-slate-600">{student?.studentId ?? '—'}</Td>
            <Td className="whitespace-nowrap text-slate-600">{formatDate(r.requestDate)}</Td>
            <Td className="max-w-xs text-slate-600">{r.remarks || <span className="text-slate-500">—</span>}</Td>
            <Td className="text-right whitespace-nowrap">{actions(r)}</Td>
          </tr>
        )
      })}
    </Table>
  )

  return (
    <div className="space-y-6">
      <Card title={`Pending (${pending.data?.length ?? 0})`}>
        {pending.isLoading && <Loading />}
        {pending.error && <ErrorNotice error={pending.error} onRetry={() => void pending.refetch()} />}
        {pending.data &&
          (pending.data.length === 0 ? (
            <EmptyState>Nothing waiting for review.</EmptyState>
          ) : (
            rows(pending.data, (r) => (
              <div className="flex justify-end gap-2">
                <Button size="sm" variant="success" onClick={() => setReview({ request: r, decision: 'APPROVED' })}>
                  Approve
                </Button>
                <Button size="sm" variant="danger" onClick={() => setReview({ request: r, decision: 'REJECTED' })}>
                  Reject
                </Button>
              </div>
            ))
          ))}
      </Card>

      <Card title={`Rejected (${rejected.length})`}>
        <p className="-mt-1 mb-3 text-sm text-slate-600">
          Once the student has resolved the issue, reopen the request to put it back in the pending queue.
        </p>
        {all.isLoading && <Loading />}
        {all.error && <ErrorNotice error={all.error} />}
        {all.data &&
          (rejected.length === 0 ? (
            <EmptyState>No rejected requests.</EmptyState>
          ) : (
            rows(rejected, (r) => (
              <Button size="sm" variant="secondary" onClick={() => setReview({ request: r, decision: 'PENDING' })}>
                Reopen
              </Button>
            ))
          ))}
      </Card>

      {review && (
        <ReviewDialog
          request={review.request}
          decision={review.decision}
          studentLabel={studentName(students.get(review.request.studentId))}
          onClose={() => setReview(null)}
        />
      )}
    </div>
  )
}

const DIALOG: Record<ClearanceStatus, { title: string; button: string; variant: 'success' | 'danger' | 'primary' }> = {
  APPROVED: { title: 'Approve clearance', button: 'Approve', variant: 'success' },
  REJECTED: { title: 'Reject clearance', button: 'Reject', variant: 'danger' },
  PENDING: { title: 'Reopen clearance request', button: 'Reopen', variant: 'primary' },
}

/** Approve / reject / reopen one request. Rejecting requires remarks. Approving is final. */
export function ReviewDialog({
  request,
  decision,
  studentLabel,
  onClose,
}: {
  request: ClearanceRequest
  decision: ClearanceStatus
  studentLabel: string
  onClose: () => void
}) {
  const update = useUpdateClearanceStatus()
  const { register, handleSubmit, formState } = useForm<{ remarks: string }>({
    defaultValues: { remarks: decision === 'PENDING' ? '' : (request.remarks ?? '') },
  })
  const config = DIALOG[decision]

  const onSubmit = handleSubmit(({ remarks }) =>
    update.mutate({ id: request.id, status: decision, remarks: blankToNull(remarks) }, { onSuccess: onClose }),
  )

  return (
    <Modal title={config.title} onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4">
        <p className="text-sm">
          Student: <span className="font-semibold text-slate-900">{studentLabel}</span>
        </p>
        {decision === 'APPROVED' && (
          <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-900 ring-1 ring-inset ring-amber-300">
            Approval is final. An approved request can't be changed afterwards.
          </p>
        )}
        <Field
          label={decision === 'REJECTED' ? 'Reason for rejection' : 'Remarks (optional)'}
          error={formState.errors.remarks?.message}
          hint={decision === 'REJECTED' ? 'This is sent to the student, so say what they need to resolve.' : undefined}
        >
          {(id) => (
            <Textarea
              id={id}
              rows={3}
              {...register('remarks', {
                validate: (v) => decision !== 'REJECTED' || v.trim().length > 0 || 'Remarks are required when rejecting',
              })}
            />
          )}
        </Field>
        {decision !== 'PENDING' && (
          <p className="text-xs text-slate-600">The student is notified of this decision by email and SMS.</p>
        )}
        <ErrorNotice error={update.error} />
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" variant={config.variant} loading={update.isPending}>
            {config.button}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
