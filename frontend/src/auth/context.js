import { createContext, useContext } from 'react'

export const AuthContext = createContext(null)

/**
 * { user, checking, login({ email, password }), register({ username, email, gender, password }), logout() }
 * user = { id, username, email, gender: "MALE"|"FEMALE"|"OTHER", createdAt } sau null
 */
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth trebuie folosit în interiorul <AuthProvider>.')
  return ctx
}

/** Adresa la care te întoarce login-ul (?next=/plan). Acceptă doar căi interne. */
export function safeNext(search, fallback = '/plan') {
  const next = new URLSearchParams(search).get('next')
  return next && next.startsWith('/') && !next.startsWith('//') ? next : fallback
}
