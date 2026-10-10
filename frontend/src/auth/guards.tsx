import { Navigate, Outlet, useLocation } from 'react-router'
import { AccessDeniedPage } from '../pages/AccessDeniedPage'
import { ErrorNotice, FullPageSpinner } from '../components/ui'
import type { Role } from '../lib/types'
import { homePathFor, useAuth } from './AuthContext'
import type { LoginRedirect } from './AuthProvider'

/** Anything under this needs a valid token and a loaded /me. */
export function RequireAuth() {
  const { status, loadError, retry, notice } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <FullPageSpinner label="Checking your session…" />
  if (status === 'anonymous') {
    // remember the page only for a cold visit; after a session ends the next
    // person to sign in may have a different role, so they start at their home
    return <Navigate to="/login" replace state={(notice ? {} : { from: location.pathname }) satisfies LoginRedirect} />
  }
  if (status === 'error') {
    return (
      <div className="mx-auto mt-24 max-w-md px-4">
        <ErrorNotice error={loadError} onRetry={retry} />
      </div>
    )
  }
  return <Outlet />
}

/**
 * Route-level role check, based on the role /me reports *now*. A user who
 * follows a stale link (or whose role just changed) sees the access-denied
 * screen in place instead of a page that would only 403 anyway.
 */
export function RequireRole({ roles }: { roles: Role[] }) {
  const { user } = useAuth()
  if (!user || !roles.includes(user.role)) return <AccessDeniedPage />
  return <Outlet />
}

export function HomeRedirect() {
  const { user } = useAuth()
  return user ? <Navigate to={homePathFor(user.role)} replace /> : null
}
