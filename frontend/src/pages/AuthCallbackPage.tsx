import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { homePathFor, useAuth } from '../auth/AuthContext'
import { Logo } from '../components/Logo'
import { ErrorNotice, FullPageSpinner } from '../components/ui'

/**
 * Where the backend sends the browser after Google sign-in
 * (OAuth2LoginSuccessHandler). It arrives as one of:
 *   /auth/callback#token=<jwt>&expiresAt=<ISO-8601>
 *   /auth/callback#error=<message>   (Google account not linked, email not verified)
 * The values are in the fragment, which browsers never send to a server, so
 * the token stays out of access logs. It is also wiped from the address bar
 * and history as soon as it's read.
 */
type CallbackResult = { token: string; expiresAt: string } | { error: string }

function readFragment(): CallbackResult {
  const params = new URLSearchParams(window.location.hash.slice(1))
  const token = params.get('token')
  const expiresAt = params.get('expiresAt')
  const failure = params.get('error')
  if (failure || !token || !expiresAt || Number.isNaN(Date.parse(expiresAt))) {
    return { error: failure ?? 'Google sign-in did not return a usable token.' }
  }
  return { token, expiresAt }
}

export function AuthCallbackPage() {
  const { acceptToken } = useAuth()
  const navigate = useNavigate()
  // read once on first render; the effect below wipes the fragment
  const [result] = useState(readFragment)
  const [loadError, setLoadError] = useState<unknown>(null)
  const handled = useRef(false)

  useEffect(() => {
    if (handled.current) return // StrictMode runs effects twice in dev
    handled.current = true
    window.history.replaceState(null, '', window.location.pathname)

    if ('token' in result) {
      acceptToken(result.token, result.expiresAt)
        .then((user) => navigate(homePathFor(user.role), { replace: true }))
        .catch(setLoadError)
    }
  }, [result, acceptToken, navigate])

  const error = 'error' in result ? new Error(result.error) : loadError
  if (!error) return <FullPageSpinner label="Finishing Google sign-in…" />

  return (
    <div className="mx-auto mt-24 max-w-md space-y-4 px-4">
      <div className="flex items-center gap-2">
        <Logo size={28} />
        <span className="font-semibold text-brand-900">GradClear</span>
      </div>
      <ErrorNotice error={error} />
      <Link to="/login" className="text-sm font-semibold text-brand-700 hover:underline">
        Back to sign in
      </Link>
    </div>
  )
}
