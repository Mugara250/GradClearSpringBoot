import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { homePathFor, useAuth } from '../auth/AuthContext'
import type { LoginRedirect } from '../auth/AuthProvider'
import { Logo } from '../components/Logo'
import { Button, ErrorNotice, Field, Input, PasswordInput } from '../components/ui'
import { GOOGLE_LOGIN_URL } from '../config'

interface LoginForm {
  loginId: string
  password: string
}

export function LoginPage() {
  const { status, user, login, notice } = useAuth()
  const navigate = useNavigate()
  const state = (useLocation().state ?? {}) as LoginRedirect
  const [error, setError] = useState<unknown>(null)
  const { register, handleSubmit, formState } = useForm<LoginForm>()

  if (status === 'authenticated' && user) return <Navigate to={homePathFor(user.role)} replace />

  const onSubmit = handleSubmit(async ({ loginId, password }) => {
    setError(null)
    try {
      const signedIn = await login(loginId.trim(), password)
      navigate(state.from ?? homePathFor(signedIn.role), { replace: true })
    } catch (e) {
      setError(e)
    }
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-brand-900 to-brand-700 px-4 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex flex-col items-center text-center text-white">
          <div className="rounded-2xl bg-white p-3 shadow-lg">
            <Logo size={56} />
          </div>
          <h1 className="mt-4 text-3xl font-extrabold text-white">GradClear</h1>
          <p className="mt-1 text-sm text-brand-100">Graduation clearance, tracked in one place</p>
        </div>

        <div className="rounded-xl bg-white p-6 shadow-xl">
          {notice && (
            <p className="mb-4 rounded-md bg-sky-50 px-3 py-2 text-sm text-brand-800 ring-1 ring-inset ring-sky-200">{notice}</p>
          )}

          <form onSubmit={onSubmit} className="space-y-4" noValidate>
            <Field label="Student or staff ID" error={formState.errors.loginId?.message} hint="e.g. your registration number">
              {(id) => (
                <Input id={id} autoComplete="username" autoFocus {...register('loginId', { required: 'Enter your student or staff ID' })} />
              )}
            </Field>
            <Field label="Password" error={formState.errors.password?.message}>
              {(id) => (
                <PasswordInput id={id} autoComplete="current-password" {...register('password', { required: 'Enter your password' })} />
              )}
            </Field>

            <ErrorNotice error={error} />

            <Button type="submit" className="w-full" loading={formState.isSubmitting}>
              Sign in
            </Button>
          </form>

          <div className="my-5 flex items-center gap-3 text-xs text-slate-500">
            <span className="h-px flex-1 bg-slate-200" /> or <span className="h-px flex-1 bg-slate-200" />
          </div>

          {/* full-page navigation to the backend, which hands off to Google */}
          <a
            href={GOOGLE_LOGIN_URL}
            className="flex w-full items-center justify-center gap-2 rounded-md bg-white px-4 py-2 text-sm font-semibold text-slate-800 ring-1 ring-inset ring-slate-300 hover:bg-sky-50"
          >
            <GoogleGlyph /> Sign in with Google
          </a>
          <p className="mt-2 text-center text-xs text-slate-600">
            Only works for Google accounts an administrator has linked to your GradClear account.
          </p>
        </div>
      </div>
    </div>
  )
}

function GoogleGlyph() {
  return (
    <svg viewBox="0 0 48 48" className="size-4" aria-hidden="true">
      <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.4-.4-3.5z" />
      <path fill="#FF3D00" d="m6.3 14.7 6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z" />
      <path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2c-2 1.5-4.5 2.4-7.2 2.4-5.2 0-9.6-3.3-11.3-7.9l-6.5 5C9.5 39.6 16.2 44 24 44z" />
      <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.4-.4-3.5z" />
    </svg>
  )
}
