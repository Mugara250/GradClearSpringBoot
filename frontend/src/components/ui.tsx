import {
  forwardRef,
  useEffect,
  useId,
  useState,
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react'
import { ApiError, errorMessage } from '../lib/api'
import type { ClearanceStatus, CourseStatus } from '../lib/types'

const cx = (...classes: (string | false | null | undefined)[]) => classes.filter(Boolean).join(' ')

// ---- buttons -----------------------------------------------------------

type Variant = 'primary' | 'secondary' | 'danger' | 'success' | 'ghost'

const variants: Record<Variant, string> = {
  primary: 'bg-brand-700 text-white hover:bg-brand-600 focus-visible:outline-brand-500',
  secondary: 'bg-white text-brand-800 ring-1 ring-inset ring-brand-200 hover:bg-sky-50 focus-visible:outline-brand-500',
  danger: 'bg-red-600 text-white hover:bg-red-500 focus-visible:outline-red-500',
  success: 'bg-emerald-600 text-white hover:bg-emerald-500 focus-visible:outline-emerald-500',
  ghost: 'text-brand-700 hover:bg-sky-50 focus-visible:outline-brand-500',
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: 'sm' | 'md'
  loading?: boolean
}

export function Button({ variant = 'primary', size = 'md', loading, disabled, className, children, ...rest }: ButtonProps) {
  return (
    <button
      type="button"
      disabled={disabled || loading}
      className={cx(
        'inline-flex items-center justify-center gap-2 rounded-md font-semibold transition-colors',
        'focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50',
        size === 'sm' ? 'px-2.5 py-1.5 text-xs' : 'px-4 py-2 text-sm',
        variants[variant],
        className,
      )}
      {...rest}
    >
      {loading && <Spinner className="size-4" />}
      {children}
    </button>
  )
}

// ---- layout ------------------------------------------------------------

export function Card({ title, actions, children, className }: { title?: ReactNode; actions?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={cx('rounded-xl border border-slate-200 bg-white shadow-sm', className)}>
      {(title || actions) && (
        <header className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 px-5 py-3">
          {title && <h2 className="text-base font-bold text-slate-900">{title}</h2>}
          {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
        </header>
      )}
      <div className="p-5">{children}</div>
    </section>
  )
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-600">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

export function Stat({ label, value, hint, tone = 'default' }: { label: string; value: ReactNode; hint?: ReactNode; tone?: 'default' | 'good' | 'warn' | 'bad' }) {
  const toneClass = { default: 'text-slate-900', good: 'text-emerald-700', warn: 'text-amber-700', bad: 'text-red-700' }[tone]
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-xs font-bold uppercase tracking-wide text-slate-600">{label}</p>
      <p className={cx('mt-2 text-2xl font-bold', toneClass)}>{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-600">{hint}</p>}
    </div>
  )
}

// ---- feedback ----------------------------------------------------------

export function Spinner({ className = 'size-5' }: { className?: string }) {
  return (
    <svg className={cx('animate-spin', className)} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeOpacity="0.25" strokeWidth="4" />
      <path d="M22 12a10 10 0 0 0-10-10" stroke="currentColor" strokeWidth="4" strokeLinecap="round" />
    </svg>
  )
}

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 py-6 text-sm text-slate-600">
      <Spinner className="size-4 text-brand-600" /> {label}
    </div>
  )
}

export function FullPageSpinner({ label }: { label: string }) {
  return (
    <div className="flex min-h-screen items-center justify-center gap-3 text-slate-600">
      <Spinner className="size-6 text-brand-600" /> {label}
    </div>
  )
}

/** Shows an API error. A 403 gets its own wording: the action was refused, not broken. */
export function ErrorNotice({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  if (!error) return null
  const forbidden = error instanceof ApiError && error.isForbidden
  const status = error instanceof ApiError ? error.status : 0
  const title = forbidden
    ? 'Permission denied'
    : status === 409
      ? 'Already exists'
      : status === 400 || status === 404
        ? "Couldn't complete that"
        : 'Something went wrong'
  return (
    <div
      role="alert"
      className={cx(
        'flex flex-wrap items-start justify-between gap-3 rounded-lg border px-4 py-3 text-sm',
        forbidden ? 'border-amber-300 bg-amber-50 text-amber-900' : 'border-red-200 bg-red-50 text-red-800',
      )}
    >
      <div>
        <p className="font-bold">{title}</p>
        <p className="mt-0.5">
          {forbidden
            ? "You don't have permission to do this. Your access may have changed, so the page has been refreshed."
            : errorMessage(error)}
        </p>
      </div>
      {onRetry && !forbidden && (
        <Button size="sm" variant="secondary" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <p className="rounded-lg border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-600">{children}</p>
}

// ---- status badges -----------------------------------------------------

const statusTone: Record<ClearanceStatus | CourseStatus, string> = {
  PENDING: 'bg-amber-50 text-amber-800 ring-amber-300',
  INCOMPLETE: 'bg-amber-50 text-amber-800 ring-amber-300',
  APPROVED: 'bg-emerald-50 text-emerald-800 ring-emerald-300',
  PASSED: 'bg-emerald-50 text-emerald-800 ring-emerald-300',
  REJECTED: 'bg-red-50 text-red-800 ring-red-300',
  FAILED: 'bg-red-50 text-red-800 ring-red-300',
}

export function StatusBadge({ status }: { status: ClearanceStatus | CourseStatus }) {
  return (
    <span className={cx('inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ring-1 ring-inset', statusTone[status])}>
      {status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  )
}

export function Tag({ children }: { children: ReactNode }) {
  return <span className="inline-flex items-center rounded-md bg-brand-50 px-2 py-0.5 text-xs font-semibold text-brand-800">{children}</span>
}

// ---- tables ------------------------------------------------------------

export function Table({ head, children }: { head: ReactNode[]; children: ReactNode }) {
  return (
    <div className="-mx-5 overflow-x-auto">
      <table className="min-w-full text-sm">
        <thead>
          <tr className="border-b border-slate-200 text-left text-xs font-bold uppercase tracking-wide text-slate-600">
            {head.map((h, i) => (
              <th key={i} className="px-5 py-2 whitespace-nowrap">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">{children}</tbody>
      </table>
    </div>
  )
}

export const Td = ({ children, className }: { children?: ReactNode; className?: string }) => (
  <td className={cx('px-5 py-2.5 align-middle', className)}>{children}</td>
)

// ---- form controls -----------------------------------------------------

const control =
  'block w-full rounded-md border-0 bg-white px-3 py-2 text-sm text-slate-900 ring-1 ring-inset ring-slate-300 placeholder:text-slate-500 focus:ring-2 focus:ring-brand-500 disabled:bg-slate-100'

export function Field({ label, error, hint, children }: { label: string; error?: string; hint?: ReactNode; children: (id: string) => ReactNode }) {
  const id = useId()
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-semibold text-slate-800">
        {label}
      </label>
      {children(id)}
      {hint && !error && <p className="mt-1 text-xs text-slate-600">{hint}</p>}
      {error && <p className="mt-1 text-xs text-red-700">{error}</p>}
    </div>
  )
}

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(({ className, ...rest }, ref) => (
  <input ref={ref} className={cx(control, className)} {...rest} />
))

/** Password field with a show/hide toggle. */
export const PasswordInput = forwardRef<HTMLInputElement, Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>>(
  ({ className, ...rest }, ref) => {
    const [visible, setVisible] = useState(false)
    return (
      <div className="relative">
        <input ref={ref} type={visible ? 'text' : 'password'} className={cx(control, 'pr-11', className)} {...rest} />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? 'Hide password' : 'Show password'}
          aria-pressed={visible}
          title={visible ? 'Hide password' : 'Show password'}
          className="absolute inset-y-0 right-0 flex w-10 items-center justify-center rounded-r-md text-slate-600 hover:text-brand-700 focus-visible:outline-2 focus-visible:outline-brand-500"
        >
          {visible ? <EyeOffIcon /> : <EyeIcon />}
        </button>
      </div>
    )
  },
)

const EyeIcon = () => (
  <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
    <circle cx="12" cy="12" r="3" />
  </svg>
)

const EyeOffIcon = () => (
  <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M10.6 5.1A10.8 10.8 0 0 1 12 5c6.5 0 10 7 10 7a17.6 17.6 0 0 1-2.6 3.6M6.6 6.6C3.8 8.4 2 12 2 12s3.5 7 10 7a9.7 9.7 0 0 0 5.4-1.6" />
    <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
    <path d="m2 2 20 20" />
  </svg>
)

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(({ className, ...rest }, ref) => (
  <select ref={ref} className={cx(control, className)} {...rest} />
))

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(({ className, ...rest }, ref) => (
  <textarea ref={ref} className={cx(control, className)} {...rest} />
))

// ---- modal -------------------------------------------------------------

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-brand-950/40 p-4 sm:items-center" onMouseDown={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="w-full max-w-lg rounded-xl bg-white shadow-xl"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <header className="flex items-center justify-between border-b border-slate-100 px-5 py-3">
          <h2 className="text-lg font-bold">{title}</h2>
          <button type="button" onClick={onClose} className="rounded p-1 text-slate-500 hover:bg-slate-100 hover:text-slate-600" aria-label="Close">
            ✕
          </button>
        </header>
        <div className="p-5">{children}</div>
      </div>
    </div>
  )
}

/** Confirm-before-delete dialog that shows the server's error if the delete fails. */
export function ConfirmDelete({ what, onConfirm, onClose, pending, error }: { what: string; onConfirm: () => void; onClose: () => void; pending: boolean; error: unknown }) {
  return (
    <Modal title={`Delete ${what}?`} onClose={onClose}>
      <p className="text-sm">This can't be undone.</p>
      <div className="mt-4 space-y-4">
        <ErrorNotice error={error} />
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="danger" loading={pending} onClick={onConfirm}>
            Delete
          </Button>
        </div>
      </div>
    </Modal>
  )
}

export function FormActions({ onCancel, pending, label = 'Save' }: { onCancel: () => void; pending: boolean; label?: string }) {
  return (
    <div className="flex justify-end gap-2 pt-2">
      <Button variant="secondary" onClick={onCancel}>
        Cancel
      </Button>
      <Button type="submit" loading={pending}>
        {label}
      </Button>
    </div>
  )
}
