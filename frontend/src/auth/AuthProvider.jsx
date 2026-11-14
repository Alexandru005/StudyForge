import { useCallback, useEffect, useMemo, useState } from 'react'
import * as api from '../api.js'
import { useToast } from '../toast/context.js'
import { AuthContext } from './context.js'

/** Ține minte utilizatorul conectat (token JWT în localStorage) și îl verifică la pornire cu GET /api/auth/me. */
export function AuthProvider({ children }) {
  const toast = useToast()
  const [user, setUser] = useState(() => (api.getToken() ? api.getSavedUser() : null))
  const [checking, setChecking] = useState(() => Boolean(api.getToken()))

  // La pornire: token-ul salvat mai e valid?
  useEffect(() => {
    if (!api.getToken()) return
    let alive = true
    api
      .getMe()
      .then((me) => {
        if (!alive) return
        api.saveUser(me)
        setUser(me)
      })
      .catch(() => {
        // 401 -> sesiunea e ștearsă prin evenimentul de mai jos; altă eroare (server oprit) -> păstrăm datele salvate
      })
      .finally(() => alive && setChecking(false))
    return () => {
      alive = false
    }
  }, [])

  // Orice 401 de la backend: token expirat / invalid
  useEffect(() => {
    const onUnauthorized = () => {
      setUser(null)
      toast('Sesiunea a expirat. Autentifică-te din nou.')
    }
    window.addEventListener(api.UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(api.UNAUTHORIZED_EVENT, onUnauthorized)
  }, [toast])

  const login = useCallback(async (credentials) => {
    const auth = await api.login(credentials)
    api.saveSession(auth)
    setUser(auth.user)
    return auth.user
  }, [])

  const register = useCallback(async (data) => {
    const auth = await api.register(data)
    api.saveSession(auth)
    setUser(auth.user)
    return auth.user
  }, [])

  const logout = useCallback(() => {
    api.saveSession(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, checking, login, register, logout }),
    [user, checking, login, register, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
