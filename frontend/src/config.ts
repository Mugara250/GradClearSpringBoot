const backendUrl = (import.meta.env.VITE_BACKEND_URL as string | undefined) || 'http://localhost:8088'

// '' in dev: calls go to /api/... on the Vite server and are proxied.
export const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? ''

// Full-page navigation to the backend, which redirects to Google and, once
// signed in, back to CALLBACK_PATH below with the token in the URL fragment.
export const GOOGLE_LOGIN_URL = `${backendUrl}/oauth2/authorization/google`

// Must match gradclear.frontend-callback-path on the backend
// (OAuth2LoginSuccessHandler redirects here).
export const CALLBACK_PATH = '/auth/callback'

// Show the "session ending" banner this long before the token expires.
export const EXPIRY_WARNING_MS = 5 * 60 * 1000
