import { useCallback, useEffect, useState } from 'react'
import { getAuthToken, getAuthUser, setAuth, clearAuth, subscribeAuth } from '../lib/authStore'
import { api } from '../api/client'

export function useAuth() {
  const [state, setState] = useState({ token: getAuthToken(), user: getAuthUser() })

  useEffect(() => subscribeAuth(setState), [])

  const login = useCallback(async (username, password) => {
    const { token, user } = await api.login(username, password)
    setAuth(token, user)
    return user
  }, [])

  const logout = useCallback(() => {
    clearAuth()
  }, [])

  return { token: state.token, user: state.user, isAuthenticated: !!state.token, login, logout }
}
