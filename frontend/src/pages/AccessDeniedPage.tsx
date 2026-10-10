import { Link } from 'react-router'
import { homePathFor, useAuth } from '../auth/AuthContext'
import { ROLE_LABEL } from '../lib/types'

/** Shown in place of a route the current role may not open. */
export function AccessDeniedPage() {
  const { user } = useAuth()
  return (
    <div className="mx-auto max-w-lg py-16 text-center">
      <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-amber-50 text-2xl text-amber-700 ring-1 ring-amber-300">
        !
      </div>
      <h1 className="mt-4 text-xl font-bold">Access denied</h1>
      <p className="mt-2 text-sm text-slate-600">
        {user
          ? `Your account (${ROLE_LABEL[user.role]}) doesn't have access to this page.`
          : "You don't have access to this page."}
      </p>
      {user && (
        <Link to={homePathFor(user.role)} className="mt-6 inline-block text-sm font-semibold text-brand-700 hover:underline">
          Go to your dashboard
        </Link>
      )}
    </div>
  )
}

export function NotFoundPage() {
  return (
    <div className="mx-auto max-w-lg py-16 text-center">
      <h1 className="text-xl font-bold">Page not found</h1>
      <Link to="/" className="mt-6 inline-block text-sm font-semibold text-brand-700 hover:underline">
        Go to your dashboard
      </Link>
    </div>
  )
}
