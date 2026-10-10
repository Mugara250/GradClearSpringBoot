import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backend = env.VITE_BACKEND_URL || 'http://localhost:8088'

  return {
    plugins: [react(), tailwindcss()],
    server: {
      // Dev only: the browser talks to Vite (localhost:5173), and Vite
      // forwards /api to Spring Boot, so the API looks same-origin and CORS
      // never comes into play.
      //
      // Production needs one of:
      //   - a reverse proxy (nginx, etc.) doing the same /api -> backend
      //     forwarding in front of the built files, or
      //   - CORS on the backend allowing this app's origin, the
      //     Authorization header and GET/POST/PUT/DELETE.
      // Auth is a bearer token in a header, not a cookie, so
      // `allowCredentials(true)` should NOT be needed. That still needs
      // confirming with the backend; the backend has no CORS config today.
      proxy: {
        '/api': { target: backend, changeOrigin: true },
      },
    },
  }
})
