import { API_BASE } from '../config'
import { authStore } from './authStore'
import type { ApiResponse, ErrorResponse } from './types'

/** Any non-2xx response, carrying the backend's ErrorResponse message. */
export class ApiError extends Error {
  readonly status: number
  readonly error: string

  constructor(status: number, error: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.error = error
  }

  get isForbidden() {
    return this.status === 403
  }
}

type Handler = () => void
let onUnauthorized: Handler = () => {}
let onForbidden: Handler = () => {}

/**
 * Set by AuthProvider. A 401 means the token is missing, expired, revoked or
 * the account is gone: auth is cleared and the user is sent to /login.
 * A 403 is just thrown to the caller, which shows it inline. It also triggers a
 * refetch of everything, because a 403 on something the UI offered means the
 * user's role or department membership changed since the page loaded.
 */
export function setAuthHandlers(handlers: { unauthorized: Handler; forbidden: Handler }) {
  onUnauthorized = handlers.unauthorized
  onForbidden = handlers.forbidden
}

interface RequestOptions {
  body?: unknown
  /** Don't treat a 401 as "session over" (used by login itself). */
  skipAuthRedirect?: boolean
}

async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const stored = authStore.get()
  if (stored) headers.Authorization = `Bearer ${stored.token}`
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Network error', 'Could not reach the GradClear server. Check your connection and try again.')
  }

  // DELETE endpoints answer 204 with no body
  const text = await response.text()
  const json: unknown = text ? safeParse(text) : null

  if (!response.ok) {
    const err = json as Partial<ErrorResponse> | null
    const apiError = new ApiError(
      response.status,
      err?.error ?? response.statusText,
      err?.message ?? `Request failed (${response.status})`,
    )
    if (response.status === 401 && !options.skipAuthRedirect) onUnauthorized()
    if (response.status === 403) onForbidden()
    throw apiError
  }

  return (json as ApiResponse<T> | null)?.data as T
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'body'>) =>
    request<T>('POST', path, { ...options, body }),
  put: <T>(path: string, body: unknown) => request<T>('PUT', path, { body }),
  delete: (path: string) => request<void>('DELETE', path),
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 403) return "You don't have permission to do that."
    return error.message
  }
  return error instanceof Error ? error.message : 'Something went wrong.'
}
