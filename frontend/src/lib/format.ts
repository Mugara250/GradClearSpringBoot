import type { Academic, Course, Department, Staff, Student, UUID } from './types'

/** id -> item, for turning the UUIDs in DTOs back into names. */
export function byId<T extends { id: UUID }>(items: T[] | undefined): Map<UUID, T> {
  return new Map((items ?? []).map((item) => [item.id, item]))
}

export const studentName = (s: Student | undefined) => (s ? `${s.firstName} ${s.lastName}` : 'Unknown student')
export const staffName = (s: Staff | undefined) => (s ? `${s.firstName} ${s.lastName}` : 'Unknown staff')
export const courseLabel = (c: Course | undefined) => (c ? `${c.courseCode} · ${c.courseName}` : 'Unknown course')
export const departmentName = (d: Department | undefined) => d?.name ?? 'Unknown department'

/** "Faculty of X › Software Engineering" style path up the Academic tree. */
export function academicPath(id: UUID | null | undefined, academics: Map<UUID, Academic>): string {
  const parts: string[] = []
  let current = id ? academics.get(id) : undefined
  while (current && parts.length < 5) {
    parts.unshift(current.name)
    current = current.parentId ? academics.get(current.parentId) : undefined
  }
  return parts.join(' › ') || '—'
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso.length === 10 ? `${iso}T00:00:00` : iso)
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

/** Empty form strings -> null, so optional fields are sent as null, not "". */
export const blankToNull = (value: string | null | undefined) => (value && value.trim() ? value.trim() : null)
