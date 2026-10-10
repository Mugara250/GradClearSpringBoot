import { Link } from 'react-router'
import { Card, ErrorNotice, PageHeader, Stat } from '../../components/ui'
import { byId, departmentName } from '../../lib/format'
import { useAllClearanceRequests, useDepartments, useStaffList, useStudents, useUsers } from '../../lib/queries'

export function AdminOverviewPage() {
  const students = useStudents()
  const staff = useStaffList()
  const users = useUsers()
  const departments = useDepartments()
  const requests = useAllClearanceRequests()
  const deptMap = byId(departments.data)

  const all = requests.data ?? []
  const pending = all.filter((r) => r.status === 'PENDING')
  const pendingByDept = [...new Set(pending.map((r) => r.departmentId))]
    .map((id) => ({ id, count: pending.filter((r) => r.departmentId === id).length }))
    .sort((a, b) => b.count - a.count)

  const error = students.error ?? staff.error ?? users.error ?? departments.error ?? requests.error
  const n = (v: unknown[] | undefined) => (v ? v.length : '…')

  return (
    <>
      <PageHeader title="Administration" subtitle="Accounts, records and the state of clearance across GradClear." />
      {error && (
        <div className="mb-6">
          <ErrorNotice error={error} />
        </div>
      )}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Students" value={n(students.data)} hint={<Link className="text-brand-700 hover:underline" to="/students">Manage students</Link>} />
        <Stat label="Staff" value={n(staff.data)} hint={<Link className="text-brand-700 hover:underline" to="/staff">Manage staff</Link>} />
        <Stat label="User accounts" value={n(users.data)} hint={<Link className="text-brand-700 hover:underline" to="/users">Manage users</Link>} />
        <Stat
          label="Pending clearance items"
          value={requests.data ? pending.length : '…'}
          tone={pending.length > 0 ? 'warn' : 'default'}
          hint={<Link className="text-brand-700 hover:underline" to="/review">Open review queue</Link>}
        />
      </div>

      <Card title="Pending items by department" className="mt-6">
        {pendingByDept.length === 0 ? (
          <p className="text-sm text-slate-600">No clearance items are waiting.</p>
        ) : (
          <ul className="divide-y divide-slate-100 text-sm">
            {pendingByDept.map(({ id, count }) => (
              <li key={id} className="flex justify-between py-2">
                <span>{departmentName(deptMap.get(id))}</span>
                <span className="font-semibold text-amber-700">{count}</span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </>
  )
}
