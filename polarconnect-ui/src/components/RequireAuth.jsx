import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

function homeFor(role) {
  return role === 'HQ_ADMIN' ? '/hq' : '/'
}

export function RequireAuth({ roles, children }) {
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to={homeFor(user.role)} replace />
  }
  return children
}
