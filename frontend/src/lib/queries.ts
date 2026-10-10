import { useMutation, useQuery, useQueryClient, type QueryKey } from '@tanstack/react-query'
import { api } from './api'
import type {
  Academic,
  AcademicLevel,
  ClearanceRequest,
  ClearanceStatus,
  Course,
  Department,
  Staff,
  Student,
  Transcript,
  User,
  UserRegistration,
  UUID,
} from './types'

const V1 = '/api/v1'

// ---- query keys --------------------------------------------------------
// Grouped by resource, so a mutation can invalidate everything under it.
export const keys = {
  me: ['me'] as const,
  users: ['users'] as const,
  students: ['students'] as const,
  student: (id: UUID) => ['students', id] as const,
  staff: ['staff'] as const,
  staffMember: (id: UUID) => ['staff', id] as const,
  departments: ['departments'] as const,
  academics: ['academics'] as const,
  academic: (id: UUID) => ['academics', id] as const,
  courses: ['courses'] as const,
  transcripts: ['transcripts'] as const,
  transcriptsOf: (studentId: UUID) => ['transcripts', 'student', studentId] as const,
  academicallyClear: (studentId: UUID) => ['transcripts', 'student', studentId, 'clear'] as const,
  passedCredits: (studentId: UUID) => ['transcripts', 'student', studentId, 'credits'] as const,
  clearance: ['clearance'] as const,
  clearanceOf: (studentId: UUID) => ['clearance', 'student', studentId] as const,
  fullyCleared: (studentId: UUID) => ['clearance', 'student', studentId, 'fully-cleared'] as const,
  pendingFor: (departmentId: UUID) => ['clearance', 'department', departmentId, 'pending'] as const,
}

const opt = (id: UUID | null | undefined) => ({ enabled: !!id })

// ---- reads -------------------------------------------------------------

export const useStudents = (enabled = true) =>
  useQuery({ queryKey: keys.students, queryFn: () => api.get<Student[]>(`${V1}/student`), enabled })

export const useStudent = (id: UUID | null | undefined) =>
  useQuery({ queryKey: keys.student(id!), queryFn: () => api.get<Student>(`${V1}/student/${id}`), ...opt(id) })

export const useStaffList = (enabled = true) =>
  useQuery({ queryKey: keys.staff, queryFn: () => api.get<Staff[]>(`${V1}/staff`), enabled })

export const useStaffMember = (id: UUID | null | undefined) =>
  useQuery({ queryKey: keys.staffMember(id!), queryFn: () => api.get<Staff>(`${V1}/staff/${id}`), ...opt(id) })

export const useDepartments = () =>
  useQuery({ queryKey: keys.departments, queryFn: () => api.get<Department[]>(`${V1}/department`) })

export const useAcademics = () =>
  useQuery({ queryKey: keys.academics, queryFn: () => api.get<Academic[]>(`${V1}/academic`) })

export const useAcademic = (id: UUID | null | undefined) =>
  useQuery({ queryKey: keys.academic(id!), queryFn: () => api.get<Academic>(`${V1}/academic/${id}`), ...opt(id) })

export const useCourses = () => useQuery({ queryKey: keys.courses, queryFn: () => api.get<Course[]>(`${V1}/course`) })

export const useUsers = (enabled = true) =>
  useQuery({ queryKey: keys.users, queryFn: () => api.get<User[]>(`${V1}/user`), enabled })

export const useTranscriptsOf = (studentId: UUID | null | undefined) =>
  useQuery({
    queryKey: keys.transcriptsOf(studentId!),
    queryFn: () => api.get<Transcript[]>(`${V1}/transcript/student/${studentId}`),
    ...opt(studentId),
  })

export const useAcademicallyClear = (studentId: UUID | null | undefined) =>
  useQuery({
    queryKey: keys.academicallyClear(studentId!),
    queryFn: () => api.get<boolean>(`${V1}/transcript/student/${studentId}/academically-clear`),
    ...opt(studentId),
  })

export const usePassedCredits = (studentId: UUID | null | undefined) =>
  useQuery({
    queryKey: keys.passedCredits(studentId!),
    queryFn: () => api.get<number>(`${V1}/transcript/student/${studentId}/total-passed-credits`),
    ...opt(studentId),
  })

export const useClearanceOf = (studentId: UUID | null | undefined) =>
  useQuery({
    queryKey: keys.clearanceOf(studentId!),
    queryFn: () => api.get<ClearanceRequest[]>(`${V1}/clearance-request/student/${studentId}`),
    ...opt(studentId),
  })

export const useFullyCleared = (studentId: UUID | null | undefined) =>
  useQuery({
    queryKey: keys.fullyCleared(studentId!),
    queryFn: () => api.get<boolean>(`${V1}/clearance-request/student/${studentId}/fully-cleared`),
    ...opt(studentId),
  })

export const useAllClearanceRequests = (enabled = true) =>
  useQuery({
    queryKey: keys.clearance,
    queryFn: () => api.get<ClearanceRequest[]>(`${V1}/clearance-request`),
    enabled,
  })

export const usePendingFor = (departmentId: UUID | null | undefined) =>
  useQuery({
    queryKey: keys.pendingFor(departmentId!),
    queryFn: () => api.get<ClearanceRequest[]>(`${V1}/clearance-request/department/${departmentId}/pending`),
    ...opt(departmentId),
  })

// ---- writes ------------------------------------------------------------

function useInvalidating<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>, invalidate: QueryKey[]) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => Promise.all(invalidate.map((queryKey) => queryClient.invalidateQueries({ queryKey }))),
  })
}

/** POST when there's no id, PUT /{id} when there is. */
function useSave<TBody, TResult>(path: string, invalidate: QueryKey[]) {
  return useInvalidating(
    ({ id, body }: { id?: UUID; body: TBody }) =>
      id ? api.put<TResult>(`${V1}/${path}/${id}`, body) : api.post<TResult>(`${V1}/${path}`, body),
    invalidate,
  )
}

function useRemove(path: string, invalidate: QueryKey[]) {
  return useInvalidating((id: UUID) => api.delete(`${V1}/${path}/${id}`), invalidate)
}

export type StudentInput = Omit<Student, 'id'>
export type StaffInput = Omit<Staff, 'id'>
export type DepartmentInput = Omit<Department, 'id'>
export type AcademicInput = { name: string; level: AcademicLevel; parentId: UUID | null; requiredCredits: number | null }
export type CourseInput = Omit<Course, 'id'>
export type TranscriptInput = Omit<Transcript, 'id'>

export const useSaveStudent = () => useSave<StudentInput, Student>('student', [keys.students])
export const useDeleteStudent = () => useRemove('student', [keys.students])

export const useSaveStaff = () => useSave<StaffInput, Staff>('staff', [keys.staff])
export const useDeleteStaff = () => useRemove('staff', [keys.staff])

export const useSaveDepartment = () => useSave<DepartmentInput, Department>('department', [keys.departments])
export const useDeleteDepartment = () => useRemove('department', [keys.departments])

export const useSaveAcademic = () => useSave<AcademicInput, Academic>('academic', [keys.academics])
export const useDeleteAcademic = () => useRemove('academic', [keys.academics])

export const useSaveCourse = () => useSave<CourseInput, Course>('course', [keys.courses])
export const useDeleteCourse = () => useRemove('course', [keys.courses])

export const useSaveTranscript = () => useSave<TranscriptInput, Transcript>('transcript', [keys.transcripts])
export const useDeleteTranscript = () => useRemove('transcript', [keys.transcripts])

// a user edit may change the caller's own role, so /me is refreshed too
export const useSaveUser = () => useSave<UserRegistration, User>('user', [keys.users, keys.me])
export const useDeleteUser = () => useRemove('user', [keys.users, keys.me])

export const useInitiateClearance = () =>
  useInvalidating(
    (studentId: UUID) => api.post<ClearanceRequest[]>(`${V1}/clearance-request/initiate/${studentId}`),
    [keys.clearance],
  )

export const useUpdateClearanceStatus = () =>
  useInvalidating(
    ({ id, status, remarks }: { id: UUID; status: ClearanceStatus; remarks: string | null }) =>
      api.put<ClearanceRequest>(`${V1}/clearance-request/${id}/status`, { status, remarks }),
    [keys.clearance],
  )
