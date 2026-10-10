# GradClear: Authentication & Authorization

The API is stateless. Every request carries a JWT in the `Authorization` header.
There is no server session and no cookie, so CSRF protection is off.

## 1. Getting a token

There are two independent ways to log in. **Both end in `JwtService.issue()` and
produce the same kind of token**, so the code that runs after login
(`JwtAuthenticationFilter` and the RBAC rules) can't tell them apart.

### a) Local login (existing accounts)

```
POST /api/v1/user/login            (public)
Content-Type: application/json

{ "loginId": "<studentId or staffId>", "password": "..." }
```

`200 OK`:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "id": "<user uuid>",
    "role": "STUDENT | DEPARTMENT_OFFICER | ADMIN",
    "studentId": "<Student uuid or null>",
    "staffId": "<Staff uuid or null>",
    "email": "<linked Google email or null>",
    "token": "<JWT>",
    "tokenExpiresAt": "2026-10-11T19:11:20Z"
  },
  "timestamp": "..."
}
```

Wrong `loginId` or password returns `401`.

### b) Google sign-in (OAuth2)

This is only enabled when the `oauth2` profile is active and the Google
credentials are set as environment variables. Never commit them.

```
SPRING_PROFILES_ACTIVE=oauth2
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

In Google Cloud Console, register this as an authorized redirect URI:
`http://localhost:8088/login/oauth2/code/google`

Flow: the browser opens `GET /oauth2/authorization/google`, is sent to Google,
and comes back to `/login/oauth2/code/google`. `OAuth2LoginSuccessHandler` then
**redirects (302) to the frontend's callback route**, with the outcome in the URL
fragment:

```
{FRONTEND_URL}/auth/callback#token=<JWT>&expiresAt=<ISO-8601>
{FRONTEND_URL}/auth/callback#error=<message>
```

`FRONTEND_URL` is `gradclear.frontend-url` (env `FRONTEND_URL`, default
`http://localhost:5173`). The path is `gradclear.frontend-callback-path`
(`/auth/callback`). Values are form-encoded. The fragment is never sent to any
server, so the token stays out of access logs. The frontend reads it, wipes it
from the address bar, stores the token, and calls `/me`.

**Limitation: pre-linked accounts only.** A Google login never creates a user.
The Google account's *verified* email must match either:

1. `User.email`, which an ADMIN sets through `POST`/`PUT /api/v1/user` (`"email"` field), or
2. for STUDENT users, the `email` on their linked `Student` record.

Matching ignores case. An unknown email or an unverified Google email does not
get a token. It is redirected with `#error=` instead ("No GradClear account is
linked to …"). The reason for this
rule: every GradClear user has to be tied to a real Student or Staff record and
a role, and nothing in a Google profile tells us which. Staff records have no
email of their own, so staff can only use Google once an admin sets `User.email`.

> The OAuth2 handshake uses a short-lived HttpSession for its `state`
> parameter, and that session is invalidated as soon as the JWT is issued. A
> failure *at Google* (e.g. the user cancels) is handled by the failure
> handler in `SecurityConfig`, which still answers with a JSON 401 on the
> backend origin rather than redirecting to the frontend.

### First admin

Creating users is ADMIN-only. To get into an empty database, start the app once
with `GRADCLEAR_ADMIN_PASSWORD` set. If the `users` table is empty, `AdminBootstrap`
creates a Staff record and an ADMIN user with loginId `STF-ADMIN-0001` (override
it with `GRADCLEAR_ADMIN_STAFF_ID`). It does nothing once any user exists.
Unset the variable afterwards.

## 2. Using the token

```
Authorization: Bearer <token>
```

| Situation | Response |
|---|---|
| No header on a protected endpoint | `401` |
| Malformed, tampered, expired or logged-out token, or the user was deleted | `401` (also on public endpoints, if the header is sent) |
| Valid token but the role or ownership rule says no | `403` |

All errors share the existing `ErrorResponse` shape:
`{ "success": false, "status": 401, "error": "Unauthorized", "message": "...", "timestamp": "..." }`

**`GET /api/v1/user/me`** returns the current user (the same `data` as login,
without `token`). A frontend calls this on page load to recover identity from
a stored token.

**`POST /api/v1/user/logout`** revokes the presented token server-side, and the
client should also discard it. Revocations are kept **in memory** until the
token would have expired. They are lost on restart and not shared between
instances (see `TokenBlocklist`). To survive restarts or run several instances,
move the blocklist to a Postgres table or Redis.

## 3. Token format

HS256-signed JWT. Claims:

| Claim | Value |
|---|---|
| `sub` | User UUID |
| `role` | `STUDENT`, `DEPARTMENT_OFFICER` or `ADMIN` (informational, see below) |
| `jti` | random token id (used by logout) |
| `iss` | `gradclear` (verified) |
| `iat` / `exp` | issued / expiry. Default lifetime **24h** (`JWT_ACCESS_TOKEN_TTL`) |

The signing key comes from `JWT_SECRET` and must be at least 32 bytes. The
fallback in `application.properties` is public and for local development only,
so **always set `JWT_SECRET` in any shared or deployed environment.**

On every request the filter reloads the user from the database, and access
decisions use the user's *current* role, not the `role` claim. Changing a
user's role or deleting the user takes effect immediately, even for tokens
already issued.

## 4. Access rules

The URL-level rules in `SecurityConfig` are: login and OAuth2 endpoints are
public, every `DELETE /api/v1/**` needs ADMIN, and everything else needs a
login. The finer rules are `@PreAuthorize` on each controller method.
Ownership checks live in `AuthorizationService` (`@authz`).

"Staff" below means DEPARTMENT_OFFICER or ADMIN. "Own" means the Student
linked to the caller's account.

| Resource | Read | Create / Update | Delete |
|---|---|---|---|
| Academic, Course | any logged-in user | staff | ADMIN |
| Department | any logged-in user | create: ADMIN. Update: ADMIN, or an officer attached to *that* department | ADMIN |
| Staff | staff | ADMIN | ADMIN |
| Student | list: staff. One record: staff, or the student themself | ADMIN | ADMIN |
| Transcript | list: staff. One record, by-student, credits, academically-clear: staff or the owning student | staff | ADMIN |
| Clearance request | list: staff. One record, by-student, fully-cleared: staff or the owning student. `department/{id}/pending`: ADMIN or an officer of that department | `initiate/{studentId}`: the student themself or ADMIN. `PUT {id}/status`: ADMIN, or an officer of the department the request is addressed to | ADMIN |
| User | list: ADMIN. One record: ADMIN or self. `/me`: any logged-in user | ADMIN | ADMIN |

When the target doesn't exist, ownership checks answer `403`, not `404`, so
students can't probe which ids exist. Staff still get `404`.

## 5. Clearance status rule

`APPROVED` is final. A `PUT /api/v1/clearance-request/{id}/status` on an
approved request returns `400` ("Cannot change status of an already-approved
clearance request"), even from ADMIN. `REJECTED` and `PENDING` can still change,
so a rejected request can go back through `PENDING → APPROVED` on resubmission.
This business rule applies on top of the RBAC check.

## 6. Known gaps in the domain model

These are places where a clean rule couldn't be expressed, and the choice that was made instead:

- **"Department" means two unrelated things.** `Staff` belongs to clearance
  `Department`s (Library, Finance, …). Students and courses hang off the
  `Academic` tree, whose levels are FACULTY, DEPARTMENT and PROGRAM. No link
  exists between staff and `Academic`, so an officer's write access to
  Academic, Course and Transcript **can't be limited to their own academic
  department**. Any officer can write any of them. Limiting it needs a
  Staff↔Academic relationship.
- **Student records are ADMIN-only to create or update** for the same reason:
  there is no way to say which officer "owns" a student.
- **Officers can read all clearance requests and transcripts**, not just
  their own department's. Only the *review* action (`PUT …/status`) and the
  pending queue are limited to their department.
- **Registration is not public.** `POST /api/v1/user` lets the caller pick the
  role, so opening it would let anyone create an ADMIN. It is ADMIN-only, and
  the first admin comes from the bootstrap described above.
