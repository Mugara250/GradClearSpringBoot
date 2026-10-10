// The bearer token, kept in memory and mirrored to sessionStorage so a page
// refresh doesn't log the user out. sessionStorage, not localStorage, so the
// token dies with the tab instead of lingering on disk. This narrows the
// window an XSS bug could exploit, but doesn't remove it.

export interface StoredToken {
  token: string
  expiresAt: string // ISO-8601
}

const KEY = 'gradclear.auth'

let current: StoredToken | null = read()

function read(): StoredToken | null {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as StoredToken
    if (!parsed.token || !parsed.expiresAt) return null
    return Date.parse(parsed.expiresAt) > Date.now() ? parsed : null
  } catch {
    return null
  }
}

export const authStore = {
  get(): StoredToken | null {
    if (current && Date.parse(current.expiresAt) <= Date.now()) {
      authStore.clear()
    }
    return current
  },
  set(token: StoredToken) {
    current = token
    try {
      sessionStorage.setItem(KEY, JSON.stringify(token))
    } catch {
      // storage unavailable (private mode etc.): memory-only for this tab
    }
  },
  clear() {
    current = null
    try {
      sessionStorage.removeItem(KEY)
    } catch {
      // ignore
    }
  },
}
