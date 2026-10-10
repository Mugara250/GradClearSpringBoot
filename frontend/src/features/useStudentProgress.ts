import {
  useAcademic,
  useAcademicallyClear,
  useClearanceOf,
  useFullyCleared,
  usePassedCredits,
  useStudent,
  useTranscriptsOf,
} from '../lib/queries'
import type { UUID } from '../lib/types'

/**
 * Everything GradClear knows about one student's progress: credits,
 * academic standing, clearance status and the transcript. The student sees
 * it on their own dashboard; staff see it on the student detail page.
 */
export function useStudentProgress(studentId: UUID) {
  const student = useStudent(studentId)
  const academic = useAcademic(student.data?.academicId)
  const transcripts = useTranscriptsOf(studentId)
  const academicallyClear = useAcademicallyClear(studentId)
  const passedCredits = usePassedCredits(studentId)
  const clearance = useClearanceOf(studentId)
  const fullyCleared = useFullyCleared(studentId)

  const all = [student, academic, transcripts, academicallyClear, passedCredits, clearance, fullyCleared]
  const requiredCredits = academic.data?.requiredCredits ?? null
  const passed = passedCredits.data ?? 0
  const requests = clearance.data ?? []

  return {
    student: student.data,
    academic: academic.data,
    transcripts: transcripts.data ?? [],
    requests,
    passed,
    requiredCredits,
    academicallyClear: academicallyClear.data,
    creditShort: requiredCredits !== null && passed < requiredCredits,
    // the backend answers fully-cleared = true when a student has *no* rows
    // ("no unapproved row exists"), so only trust it once clearance started
    fullyCleared: requests.length > 0 && fullyCleared.data === true,
    isLoading: all.some((q) => q.isLoading),
    error: all.find((q) => q.error)?.error ?? null,
    refetch: () => all.forEach((q) => void q.refetch()),
  }
}

export type StudentProgressData = ReturnType<typeof useStudentProgress>
