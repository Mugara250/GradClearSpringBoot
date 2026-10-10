import { createContext, useContext } from 'react'
import type { Role, User } from '../lib/types'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous' | 'error'

export interface AuthContextValue {
  status: AuthStatus
  /** Current user as /me reports it. Null unless status is 'authenticated'. */
  user: User | null
  expiresAt: string | null
  /** Set when /me failed for a reason other than 401 (e.g. server down). */
  loadError: unknown
  /** Why the last session ended ("signed out", "expired"), shown on /login. */
  notice: string | null
  retry: () => void
  login: (loginId: string, password: string) => Promise<User>
  /** Store a token obtained elsewhere (the Google callback) and load /me. */
  acceptToken: (token: string, expiresAt: string) => Promise<User>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}

/** The authenticated user, for components that only render behind RequireAuth. */
export function useCurrentUser(): User {
  const { user } = useAuth()
  if (!user) throw new Error('useCurrentUser used outside an authenticated route')
  return user
}

export function homePathFor(role: Role): string {
  switch (role) {
    case 'STUDENT':
      return '/student'
    case 'DEPARTMENT_OFFICER':
      return '/review'
    case 'ADMIN':
      return '/admin'
  }
}
