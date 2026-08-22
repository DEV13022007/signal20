// Token + user info persisted to localStorage so a refresh doesn't force a re-login.
// Plain module (not a React context) so non-component code — the fetch wrapper in
// client.js, the STOMP client in useAlerts — can read the current token synchronously.
const STORAGE_KEY = 'polarconnect.auth'

let state = load()
const listeners = new Set()

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : { token: null, user: null }
  } catch {
    return { token: null, user: null }
  }
}

export function getAuthToken() {
  return state.token
}

export function getAuthUser() {
  return state.user
}

export function setAuth(token, user) {
  state = { token, user }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  listeners.forEach((fn) => fn(state))
}

export function clearAuth() {
  state = { token: null, user: null }
  localStorage.removeItem(STORAGE_KEY)
  listeners.forEach((fn) => fn(state))
}

export function subscribeAuth(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}
