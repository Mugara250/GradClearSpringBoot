# GradClear frontend

React + TypeScript + Vite + Tailwind CSS + React Router, with TanStack Query
for data and React Hook Form for forms. Talks to the Spring Boot backend in
`../src`. See `../SECURITY.md` for the full auth contract.

## Running it

```bash
# backend (from the repo root), on http://localhost:8088
./mvnw spring-boot:run

# frontend
cd frontend
npm install
npm run dev          # http://localhost:5173
```

In dev, Vite proxies `/api/*` to the backend (see `vite.config.ts`), so there
is no CORS setup. Copy `.env.example` to `.env.local` to point at a different
backend.

For Google sign-in, start the backend with the `oauth2` profile and Google
credentials (see `SECURITY.md`). The backend sends the browser back to
`FRONTEND_URL` + `/auth/callback`, and `FRONTEND_URL` defaults to this dev server.

## How auth works here

- **Login** (`/login`): `POST /api/v1/user/login` returns `token` and
  `tokenExpiresAt`. Both are stored by `src/lib/authStore.ts`, in memory plus
  `sessionStorage` (not `localStorage`, so the token goes away with the tab).
- **Every request** goes through `src/lib/api.ts`, which adds
  `Authorization: Bearer <token>` and unwraps `ApiResponse.data`.
  - **401**: the session is over. The stored token is cleared and the user is
    sent to `/login` with a notice.
  - **403**: the error is thrown to the screen that made the call, which shows
    "you don't have permission" in place. All cached data (including `/me`)
    is also refetched, because a 403 on an action the UI offered means the
    role or department membership changed since the page loaded.
- **Identity** always comes from `GET /api/v1/user/me`, never from a cached
  copy. It loads on startup, refreshes on window focus and every 60s. Menus and
  route guards follow the role `/me` reports, so a role change applies without
  signing in again.
- **Expiry**: a banner appears 5 minutes before `tokenExpiresAt`, and the user
  is signed out when it's reached.
- **Logout**: `POST /api/v1/user/logout` (revokes the token on the server),
  then the local token is discarded whatever the response.
- **Google** (`/auth/callback`): reads `#token=…&expiresAt=…` or `#error=…`
  from the URL fragment, wipes it from the address bar, then continues like a
  normal login.

## Roles → screens

| Role | Home | Can open |
|---|---|---|
| Student | `/student` | own clearance status, transcript, credits, "Initiate clearance" |
| Department officer | `/review` | review queue for *their* departments, students (read + transcript entries), academic structure, courses, departments (edit own) |
| Admin | `/admin` | everything above plus staff, users, student CRUD, all deletes |

Route guards (`src/auth/guards.tsx`) show an access-denied screen for routes
the current role can't open. Buttons for actions a role can't perform are
hidden. The backend still enforces every rule, and a 403 is handled if the
two ever disagree.

## Production

Serve `npm run build` output (`dist/`) behind something that also forwards
`/api` to the backend (nginx etc.). Alternatively, set `VITE_API_BASE_URL` to
the backend URL and add CORS on the backend for this origin. Auth is a bearer
header, not a cookie, so `allowCredentials` should not be needed. Confirm that
with the backend team. Set the backend's `FRONTEND_URL` to the deployed origin
so Google sign-in returns here.
