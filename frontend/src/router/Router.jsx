import { useCallback, useEffect, useMemo, useState } from 'react'
import { RouterContext, useRouter } from './context.js'

const read = () => ({
  pathname: window.location.pathname,
  search: window.location.search,
  hash: window.location.hash,
})

/** Router minimal bazat pe History API (fără dependențe externe). */
export function Router({ children }) {
  const [location, setLocation] = useState(read)

  useEffect(() => {
    const onPop = () => setLocation(read())
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [])

  const navigate = useCallback((to, { replace = false } = {}) => {
    const url = new URL(to, window.location.href)
    const next = url.pathname + url.search + url.hash
    if (replace) window.history.replaceState(null, '', next)
    else window.history.pushState(null, '', next)
    setLocation(read())
    if (!replace && !url.hash) window.scrollTo(0, 0)
  }, [])

  // ancore (#functii, #cum) după schimbarea paginii
  useEffect(() => {
    if (!location.hash) return
    document.getElementById(decodeURIComponent(location.hash.slice(1)))?.scrollIntoView()
  }, [location])

  const value = useMemo(() => ({ location, navigate }), [location, navigate])
  return <RouterContext.Provider value={value}>{children}</RouterContext.Provider>
}

/** <a> care navighează fără reîncărcarea paginii. */
export function Link({ to, onClick, children, ...rest }) {
  const { navigate } = useRouter()
  const handleClick = (e) => {
    onClick?.(e)
    if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return
    if (rest.target === '_blank') return
    e.preventDefault()
    navigate(to)
  }
  return (
    <a href={to} onClick={handleClick} {...rest}>
      {children}
    </a>
  )
}
