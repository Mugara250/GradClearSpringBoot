import { useCurrentUser } from '../../auth/AuthContext'
import { Button, ErrorNotice, PageHeader } from '../../components/ui'
import { StudentProgressView } from '../../features/StudentProgress'
import { useStudentProgress, type StudentProgressData } from '../../features/useStudentProgress'
import { useInitiateClearance } from '../../lib/queries'
import type { UUID } from '../../lib/types'

export function StudentDashboard() {
  const user = useCurrentUser()

  if (!user.studentId) {
    return (
      <>
        <PageHeader title="My clearance" />
        <ErrorNotice error={new Error('This account is not linked to a student record. Ask an administrator to fix the link.')} />
      </>
    )
  }
  return <Dashboard studentId={user.studentId} />
}

function Dashboard({ studentId }: { studentId: UUID }) {
  const progress = useStudentProgress(studentId)
  const { student } = progress

  return (
    <>
      <PageHeader
        title={student ? `Welcome, ${student.firstName}` : 'My clearance'}
        subtitle={student && `Registration number ${student.studentId}`}
      />
      <div className="space-y-6">
        {!progress.isLoading && !progress.error && <InitiatePanel studentId={studentId} progress={progress} />}
        <StudentProgressView progress={progress} />
      </div>
    </>
  )
}

/**
 * The "Initiate clearance" action. The UI explains when it looks premature,
 * but the backend makes the real decision (credits, unresolved courses,
 * academic link), and its error message is shown as-is if it says no.
 */
function InitiatePanel({ studentId, progress }: { studentId: UUID; progress: StudentProgressData }) {
  const initiate = useInitiateClearance()
  const { requests, academicallyClear, creditShort, fullyCleared, passed, requiredCredits } = progress

  const outstanding = requests.filter((r) => r.status !== 'REJECTED').length
  let blocker: string | null = null
  if (outstanding > 0) {
    blocker = fullyCleared
      ? 'Every department has approved your clearance.'
      : 'Your clearance is already with the departments. Rejected items are reopened by the department once resolved.'
  } else if (academicallyClear === false) {
    blocker = 'Your transcript has failed or incomplete courses. Resolve them before requesting clearance.'
  } else if (creditShort) {
    blocker = `You have ${passed} of the ${requiredCredits} credits your department requires.`
  }

  if (fullyCleared) {
    return (
      <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-emerald-900">
        <p className="font-semibold">You are fully cleared for graduation 🎓</p>
        <p className="mt-0.5 text-sm">{blocker}</p>
      </div>
    )
  }

  return (
    <div className="rounded-xl border border-brand-200 bg-brand-50 px-5 py-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="font-semibold text-brand-900">Graduation clearance</p>
          <p className="mt-0.5 text-sm text-brand-800">
            {blocker ?? 'You meet the requirements. Initiating sends a clearance request to every department.'}
          </p>
        </div>
        <Button
          onClick={() => initiate.mutate(studentId)}
          loading={initiate.isPending}
          disabled={outstanding > 0}
          title={blocker ?? undefined}
        >
          Initiate clearance
        </Button>
      </div>
      {initiate.error && (
        <div className="mt-3">
          <ErrorNotice error={initiate.error} />
        </div>
      )}
      {initiate.isSuccess && (
        <p className="mt-3 text-sm text-emerald-800">Clearance requested from {initiate.data.length} departments.</p>
      )}
    </div>
  )
}
