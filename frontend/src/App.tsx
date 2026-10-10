import { Route, Routes } from 'react-router'
import { HomeRedirect, RequireAuth, RequireRole } from './auth/guards'
import { AppLayout } from './components/AppLayout'
import { CALLBACK_PATH } from './config'
import { AccessDeniedPage, NotFoundPage } from './pages/AccessDeniedPage'
import { AuthCallbackPage } from './pages/AuthCallbackPage'
import { LoginPage } from './pages/LoginPage'
import { AdminOverviewPage } from './pages/admin/AdminOverviewPage'
import { StaffPage } from './pages/admin/StaffPage'
import { UsersPage } from './pages/admin/UsersPage'
import { AcademicsPage } from './pages/staff/AcademicsPage'
import { CoursesPage } from './pages/staff/CoursesPage'
import { DepartmentsPage } from './pages/staff/DepartmentsPage'
import { ReviewQueuePage } from './pages/staff/ReviewQueuePage'
import { StudentDetailPage } from './pages/staff/StudentDetailPage'
import { StudentsPage } from './pages/staff/StudentsPage'
import { StudentDashboard } from './pages/student/StudentDashboard'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path={CALLBACK_PATH} element={<AuthCallbackPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<HomeRedirect />} />
          <Route path="forbidden" element={<AccessDeniedPage />} />

          <Route element={<RequireRole roles={['STUDENT']} />}>
            <Route path="student" element={<StudentDashboard />} />
          </Route>

          <Route element={<RequireRole roles={['DEPARTMENT_OFFICER', 'ADMIN']} />}>
            <Route path="review" element={<ReviewQueuePage />} />
            <Route path="students" element={<StudentsPage />} />
            <Route path="students/:id" element={<StudentDetailPage />} />
            <Route path="academics" element={<AcademicsPage />} />
            <Route path="courses" element={<CoursesPage />} />
            <Route path="departments" element={<DepartmentsPage />} />
          </Route>

          <Route element={<RequireRole roles={['ADMIN']} />}>
            <Route path="admin" element={<AdminOverviewPage />} />
            <Route path="staff" element={<StaffPage />} />
            <Route path="users" element={<UsersPage />} />
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
