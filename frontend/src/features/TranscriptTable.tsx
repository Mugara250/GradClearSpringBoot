import type { ReactNode } from 'react'
import { EmptyState, Loading, StatusBadge, Table, Td } from '../components/ui'
import { byId } from '../lib/format'
import { useCourses } from '../lib/queries'
import type { Transcript } from '../lib/types'

export function TranscriptTable({ transcripts, actions }: { transcripts: Transcript[]; actions?: (t: Transcript) => ReactNode }) {
  const lookup = useCourses()
  const courses = byId(lookup.data)

  // wait for the names, rather than flashing "Unknown …" rows
  if (lookup.isLoading) return <Loading />

  if (transcripts.length === 0) return <EmptyState>No transcript entries yet.</EmptyState>

  const sorted = [...transcripts].sort(
    (a, b) =>
      b.academicYear.localeCompare(a.academicYear) ||
      (courses.get(a.courseId)?.courseCode ?? '').localeCompare(courses.get(b.courseId)?.courseCode ?? ''),
  )

  return (
    <Table head={['Course', 'Credits', 'Grade', 'Status', 'Academic year', ...(actions ? [''] : [])]}>
      {sorted.map((t) => {
        const course = courses.get(t.courseId)
        return (
          <tr key={t.id}>
            <Td>
              <span className="font-semibold text-slate-900">{course?.courseCode ?? '—'}</span>
              <span className="ml-2 text-slate-600">{course?.courseName ?? 'Unknown course'}</span>
            </Td>
            <Td>{course?.creditHours ?? '—'}</Td>
            <Td className="tabular-nums">{t.grade}</Td>
            <Td>
              <StatusBadge status={t.status} />
            </Td>
            <Td className="whitespace-nowrap">{t.academicYear}</Td>
            {actions && <Td className="text-right whitespace-nowrap">{actions(t)}</Td>}
          </tr>
        )
      })}
    </Table>
  )
}
