import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError, api, setAuthHandlers } from '../lib/api'
import { authStore, type StoredToken } from '../lib/authStore'
import { keys } from '../lib/queries'
import type { User } from '../lib/types'
import { AuthContext, type AuthContextValue, type AuthStatus } from './AuthContext'

/** Router state for /login: where a logged-out visitor was trying to go. */
export interface LoginRedirect {
  from?: string
}

const ME_PATH = '/api/v1/user/me'

// login responses carry the token; /me never does, so don't cache it as one
const withoutToken = ({ token: _t, tokenExpiresAt: _e, ...user }: User): User => user

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [notice, setNotice] = useState<string | null>(null)
  const [token, setToken] = useState<StoredToken | null>(() => authStore.get())

  // Identity always comes from the server, never from a cached copy, and is
  // re-checked on focus and every minute, so a role change made by an admin
  // shows up in the UI without the user having to sign in again.
  const me = useQuery({
    queryKey: keys.me,
    queryFn: () => api.get<User>(ME_PATH),
    enabled: token !== null,
    staleTime: 30_000,
    refetchInterval: 60_000,
    refetchOnWindowFocus: true,
    retry: false,
  })

  // RequireAuth does the redirect to /login once the token is gone; doing it
  // here as well would race it and lose the notice
  const endSession = useCallback(
    (reason: string) => {
      setNotice(reason)
      authStore.clear()
      setToken(null)
      queryClient.clear()
    },
    [queryClient],
  )

  useEffect(() => {
    setAuthHandlers({
      unauthorized: () => {
        // ignore stray 401s from requests made before the session ended
        if (authStore.get() === null && token === null) return
        endSession('Your session has ended. Please sign in again.')
      },
      // a 403 on something the UI offered means our picture of the user's
      // permissions (role, department membership, …) is stale: refetch all
      forbidden: () => void queryClient.invalidateQueries(),
    })
  }, [endSession, queryClient, token])

  // sign out when the token's lifetime runs out
  useEffect(() => {
    if (!token) return
    const remaining = Date.parse(token.expiresAt) - Date.now()
    const timer = window.setTimeout(
      () => endSession('Your session expired. Please sign in again.'),
      Math.max(0, remaining),
    )
    return () => window.clearTimeout(timer)
  }, [token, endSession])

  const storeToken = useCallback((stored: StoredToken) => {
    authStore.set(stored)
    setToken(stored)
  }, [])

  const login = useCallback<AuthContextValue['login']>(
    async (loginId, password) => {
      // a 401 here means wrong credentials, not an expired session
      const user = await api.post<User>('/api/v1/user/login', { loginId, password }, { skipAuthRedirect: true })
      if (!user.token || !user.tokenExpiresAt) throw new Error('Login response did not include a token')
      queryClient.setQueryData(keys.me, withoutToken(user))
      setNotice(null)
      storeToken({ token: user.token, expiresAt: user.tokenExpiresAt })
      return user
    },
    [queryClient, storeToken],
  )

  const acceptToken = useCallback<AuthContextValue['acceptToken']>(
    async (newToken, expiresAt) => {
      setNotice(null)
      storeToken({ token: newToken, expiresAt })
      return queryClient.fetchQuery({ queryKey: keys.me, queryFn: () => api.get<User>(ME_PATH) })
    },
    [queryClient, storeToken],
  )

  const logout = useCallback(async () => {
    try {
      // revokes the token server-side; discard it locally whatever happens
      await api.post('/api/v1/user/logout', undefined, { skipAuthRedirect: true })
    } catch {
      // already invalid, or server unreachable: nothing more to do
    }
    endSession('You have been signed out.')
  }, [endSession])

  const value = useMemo<AuthContextValue>(() => {
    let status: AuthStatus
    if (!token) status = 'anonymous'
    else if (me.data) status = 'authenticated'
    else if (me.isError && !(me.error instanceof ApiError && me.error.status === 401)) status = 'error'
    else status = 'loading'

    return {
      status,
      user: status === 'authenticated' ? (me.data ?? null) : null,
      expiresAt: token?.expiresAt ?? null,
      loadError: status === 'error' ? me.error : null,
      notice,
      retry: () => void me.refetch(),
      login,
      acceptToken,
      logout,
    }
  }, [token, me, notice, login, acceptToken, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
