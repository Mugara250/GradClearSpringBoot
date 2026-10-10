import type { ReactNode } from 'react'
import { EmptyState, Loading, StatusBadge, Table, Td } from '../components/ui'
import { byId, departmentName, formatDate } from '../lib/format'
import { useDepartments } from '../lib/queries'
import type { ClearanceRequest } from '../lib/types'

/** One row per department a clearance request went to, with its decision. */
export function ClearanceTable({ requests, actions }: { requests: ClearanceRequest[]; actions?: (r: ClearanceRequest) => ReactNode }) {
  const lookup = useDepartments()
  const departments = byId(lookup.data)

  // wait for the names, rather than flashing "Unknown …" rows
  if (lookup.isLoading) return <Loading />

  if (requests.length === 0) return <EmptyState>No clearance requests yet.</EmptyState>

  const sorted = [...requests].sort(
    (a, b) =>
      departmentName(departments.get(a.departmentId)).localeCompare(departmentName(departments.get(b.departmentId))) ||
      b.requestDate.localeCompare(a.requestDate),
  )

  return (
    <Table head={['Department', 'Status', 'Remarks', 'Requested', ...(actions ? [''] : [])]}>
      {sorted.map((r) => (
        <tr key={r.id}>
          <Td className="font-semibold text-slate-900">{departmentName(departments.get(r.departmentId))}</Td>
          <Td>
            <StatusBadge status={r.status} />
          </Td>
          <Td className="max-w-xs text-slate-600">{r.remarks || <span className="text-slate-500">—</span>}</Td>
          <Td className="whitespace-nowrap text-slate-600">{formatDate(r.requestDate)}</Td>
          {actions && <Td className="text-right">{actions(r)}</Td>}
        </tr>
      ))}
    </Table>
  )
}

/** PENDING / APPROVED / REJECTED counts across a student's requests. */
export function ClearanceCounts({ requests }: { requests: ClearanceRequest[] }) {
  const count = (status: ClearanceRequest['status']) => requests.filter((r) => r.status === status).length
  return (
    <div className="flex flex-wrap gap-4 text-sm">
      <span className="flex items-center gap-1.5">
        <span className="size-2.5 rounded-full bg-emerald-500" /> {count('APPROVED')} approved
      </span>
      <span className="flex items-center gap-1.5">
        <span className="size-2.5 rounded-full bg-amber-500" /> {count('PENDING')} pending
      </span>
      <span className="flex items-center gap-1.5">
        <span className="size-2.5 rounded-full bg-red-500" /> {count('REJECTED')} rejected
      </span>
    </div>
  )
}
