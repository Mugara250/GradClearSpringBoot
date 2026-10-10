import { useEffect, useState } from 'react'
import { NavLink, Outlet } from 'react-router'
import { useAuth, useCurrentUser } from '../auth/AuthContext'
import { EXPIRY_WARNING_MS } from '../config'
import { ROLE_LABEL, type Role } from '../lib/types'
import { Logo } from './Logo'
import { Button } from './ui'

interface NavItem {
  to: string
  label: string
}

// One nav set per role. These are the three dashboard "shells"; the routes
// behind them are guarded separately (App.tsx), so hiding a link is never the
// only protection.
const NAV: Record<Role, NavItem[]> = {
  STUDENT: [{ to: '/student', label: 'My clearance' }],
  DEPARTMENT_OFFICER: [
    { to: '/review', label: 'Review queue' },
    { to: '/students', label: 'Students' },
    { to: '/academics', label: 'Academic structure' },
    { to: '/courses', label: 'Courses' },
    { to: '/departments', label: 'Departments' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Overview' },
    { to: '/review', label: 'Review queue' },
    { to: '/students', label: 'Students' },
    { to: '/staff', label: 'Staff' },
    { to: '/users', label: 'Users' },
    { to: '/departments', label: 'Departments' },
    { to: '/academics', label: 'Academic structure' },
    { to: '/courses', label: 'Courses' },
  ],
}

export function AppLayout() {
  const user = useCurrentUser()
  const { logout } = useAuth()
  const [signingOut, setSigningOut] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)

  const links = NAV[user.role].map((item) => (
    <NavLink
      key={item.to}
      to={item.to}
      onClick={() => setMenuOpen(false)}
      className={({ isActive }) =>
        `rounded-md px-3 py-2 text-sm font-semibold transition-colors ${
          isActive ? 'bg-brand-800 text-white' : 'text-brand-100 hover:bg-brand-800/60 hover:text-white'
        }`
      }
    >
      {item.label}
    </NavLink>
  ))

  return (
    <div className="min-h-screen">
      <header className="bg-brand-900 shadow">
        <div className="mx-auto flex max-w-7xl items-center gap-4 px-4 py-2.5 sm:px-6">
          <div className="flex items-center gap-2">
            <span className="rounded-md bg-white p-0.5">
              <Logo size={24} />
            </span>
            <span className="text-lg font-extrabold tracking-tight text-white">GradClear</span>
          </div>

          <nav className="hidden flex-1 flex-wrap gap-1 lg:flex">{links}</nav>

          <div className="ml-auto flex items-center gap-3">
            <span className="hidden rounded-full bg-brand-800 px-2.5 py-1 text-xs font-semibold text-sky-100 ring-1 ring-brand-600 sm:inline">
              {ROLE_LABEL[user.role]}
            </span>
            <Button
              size="sm"
              variant="secondary"
              loading={signingOut}
              onClick={() => {
                setSigningOut(true)
                void logout()
              }}
            >
              Sign out
            </Button>
            <button
              type="button"
              className="rounded-md p-2 text-brand-100 hover:bg-brand-800 lg:hidden"
              aria-label="Menu"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((open) => !open)}
            >
              ☰
            </button>
          </div>
        </div>
        {menuOpen && <nav className="flex flex-col gap-1 border-t border-brand-800 px-4 py-2 lg:hidden">{links}</nav>}
      </header>

      <ExpiryBanner />

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
        <Outlet />
      </main>
    </div>
  )
}

/** Warns shortly before the token's tokenExpiresAt, when the session will end. */
function ExpiryBanner() {
  const { expiresAt, logout } = useAuth()
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 15_000)
    return () => window.clearInterval(timer)
  }, [])

  if (!expiresAt) return null
  const remaining = Date.parse(expiresAt) - now
  if (remaining > EXPIRY_WARNING_MS) return null

  const minutes = Math.max(1, Math.ceil(remaining / 60_000))
  return (
    <div className="border-b border-amber-300 bg-amber-50 px-4 py-2 text-center text-sm text-amber-900">
      Your session ends in about {minutes} minute{minutes === 1 ? '' : 's'}. Save your work and{' '}
      <button type="button" className="font-semibold underline" onClick={() => void logout()}>
        sign in again
      </button>
      .
    </div>
  )
}
