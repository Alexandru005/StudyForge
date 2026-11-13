import { createContext, useContext } from 'react'

export const RouterContext = createContext(null)

/** { location: { pathname, search, hash }, navigate(to, { replace }) } */
export function useRouter() {
  const ctx = useContext(RouterContext)
  if (!ctx) throw new Error('useRouter trebuie folosit în interiorul <Router>.')
  return ctx
}
