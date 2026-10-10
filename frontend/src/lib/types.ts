// Mirrors the backend DTOs (src/main/java/auca/ac/rw/*/dto). UUIDs are strings,
// LocalDate is "YYYY-MM-DD", Instant is an ISO-8601 timestamp.

export type UUID = string

export type Role = 'STUDENT' | 'DEPARTMENT_OFFICER' | 'ADMIN'
export type AcademicLevel = 'PROGRAM' | 'FACULTY' | 'DEPARTMENT'
export type CourseScope = 'GENERAL' | 'MAJOR'
export type CourseStatus = 'PASSED' | 'FAILED' | 'INCOMPLETE'
export type ClearanceStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ApiResponse<T> {
  success: true
  message: string
  data: T
  timestamp: string
}

export interface ErrorResponse {
  success: false
  status: number
  error: string
  message: string
  timestamp: string
}

export interface User {
  id: UUID
  role: Role
  studentId: UUID | null // linked Student row id, not the registration number
  staffId: UUID | null // linked Staff row id
  email: string | null
  token?: string // only on login responses
  tokenExpiresAt?: string
}

export interface UserRegistration {
  password: string
  role: Role
  studentId: UUID | null
  staffId: UUID | null
  email: string | null
}

export interface Student {
  id: UUID
  studentId: string // registration number, used as loginId
  firstName: string
  lastName: string
  email: string | null
  phoneNumber: string | null
  academicId: UUID
}

export interface Staff {
  id: UUID
  staffId: string // used as loginId
  firstName: string
  lastName: string
  position: string | null
  departmentIds: UUID[]
}

export interface Department {
  id: UUID
  name: string
  description: string | null
}

export interface Academic {
  id: UUID
  name: string
  level: AcademicLevel
  parentId: UUID | null
  requiredCredits: number | null
}

export interface Course {
  id: UUID
  courseCode: string
  courseName: string
  creditHours: number
  scope: CourseScope
  academicIds: UUID[]
}

export interface Transcript {
  id: UUID
  grade: number
  status: CourseStatus
  academicYear: string
  studentId: UUID
  courseId: UUID
}

export interface ClearanceRequest {
  id: UUID
  studentId: UUID
  departmentId: UUID
  status: ClearanceStatus
  remarks: string | null
  requestDate: string
}

export const ROLE_LABEL: Record<Role, string> = {
  STUDENT: 'Student',
  DEPARTMENT_OFFICER: 'Department officer',
  ADMIN: 'Administrator',
}
