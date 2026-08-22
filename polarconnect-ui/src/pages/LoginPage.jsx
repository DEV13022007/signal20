import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import './LoginPage.css'

const DEMO_ACCOUNTS = [
  { username: 'hq.admin', password: 'admin123', label: 'HQ Admin' },
  { username: 'maitri.manager', password: 'manager123', label: 'Maitri Station Manager' },
  { username: 'bharati.crew', password: 'crew123', label: 'Bharati Crew' },
]

function homeFor(role) {
  return role === 'HQ_ADMIN' ? '/hq' : '/'
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const user = await login(username, password)
      const redirectTo = location.state?.from ?? homeFor(user.role)
      navigate(redirectTo, { replace: true })
    } catch {
      setError('Invalid username or password.')
    } finally {
      setBusy(false)
    }
  }

  function fillDemoAccount(account) {
    setUsername(account.username)
    setPassword(account.password)
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={handleSubmit}>
        <div className="eyebrow">POLARCONNECT</div>
        <h1 className="login-card__title">Sign in</h1>

        <label className="login-field">
          <span>Username</span>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
        </label>
        <label className="login-field">
          <span>Password</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>

        {error && <div className="login-error">{error}</div>}

        <button className="login-submit" type="submit" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>

        <div className="login-demo">
          <div className="login-demo__label">Demo accounts</div>
          {DEMO_ACCOUNTS.map((account) => (
            <button
              key={account.username}
              type="button"
              className="login-demo__account"
              onClick={() => fillDemoAccount(account)}
            >
              {account.label} <span className="mono">({account.username})</span>
            </button>
          ))}
        </div>
      </form>
    </div>
  )
}
