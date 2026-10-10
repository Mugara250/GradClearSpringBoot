import type { ReactNode } from 'react'
import { Card, ErrorNotice, Loading, Stat } from '../components/ui'
import type { ClearanceRequest, Transcript } from '../lib/types'
import { ClearanceCounts, ClearanceTable } from './ClearanceTable'
import { TranscriptTable } from './TranscriptTable'
import type { StudentProgressData } from './useStudentProgress'

export function ProgressStats({ progress }: { progress: StudentProgressData }) {
  const { passed, requiredCredits, creditShort, academicallyClear, requests, fullyCleared } = progress
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <Stat
        label="Clearance"
        value={requests.length === 0 ? 'Not started' : fullyCleared ? 'Fully cleared' : 'In progress'}
        tone={requests.length === 0 ? 'default' : fullyCleared ? 'good' : 'warn'}
        hint={requests.length > 0 ? <ClearanceCounts requests={requests} /> : 'No department has been asked yet'}
      />
      <Stat
        label="Passed credits"
        value={
          <>
            {passed}
            <span className="text-base font-normal text-slate-500"> / {requiredCredits ?? '—'}</span>
          </>
        }
        tone={creditShort ? 'warn' : 'good'}
        hint={creditShort ? `${(requiredCredits ?? 0) - passed} more credits required` : 'Credit requirement met'}
      />
      <Stat
        label="Academic standing"
        value={academicallyClear === undefined ? '—' : academicallyClear ? 'Clear' : 'Unresolved courses'}
        tone={academicallyClear === false ? 'bad' : 'good'}
        hint={academicallyClear === false ? 'Failed or incomplete courses on the transcript' : 'No failed or incomplete courses'}
      />
      <Stat label="Department" value={<span className="text-lg">{progress.academic?.name ?? '—'}</span>} hint="Academic department" />
    </div>
  )
}

export function StudentProgressView({
  progress,
  clearanceActions,
  transcriptActions,
  transcriptToolbar,
  clearanceToolbar,
}: {
  progress: StudentProgressData
  clearanceActions?: (r: ClearanceRequest) => ReactNode
  transcriptActions?: (t: Transcript) => ReactNode
  transcriptToolbar?: ReactNode
  clearanceToolbar?: ReactNode
}) {
  if (progress.isLoading) return <Loading />
  if (progress.error) return <ErrorNotice error={progress.error} onRetry={progress.refetch} />

  return (
    <div className="space-y-6">
      <ProgressStats progress={progress} />
      <Card title="Clearance by department" actions={clearanceToolbar}>
        <ClearanceTable requests={progress.requests} actions={clearanceActions} />
      </Card>
      <Card title="Transcript" actions={transcriptToolbar}>
        <TranscriptTable transcripts={progress.transcripts} actions={transcriptActions} />
      </Card>
    </div>
  )
}
