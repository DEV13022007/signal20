import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { Dashboard } from './pages/Dashboard'
import { Headquarters } from './pages/Headquarters'
import { LoginPage } from './pages/LoginPage'
import { RequireAuth } from './components/RequireAuth'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <RequireAuth roles={['STATION_MANAGER', 'CREW', 'HQ_ADMIN']}>
              <Dashboard />
            </RequireAuth>
          }
        />
        <Route
          path="/hq"
          element={
            <RequireAuth roles={['HQ_ADMIN']}>
              <Headquarters />
            </RequireAuth>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App
