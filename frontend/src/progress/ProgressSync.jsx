import { useEffect } from 'react'
import { useAuth } from '../auth/context.js'
import { PROGRESS_EVENT } from './store.js'
import { syncProgress } from './sync.js'

const DELAY_MS = 4000 // după un răspuns la card, așteptăm puțin ca să trimitem mai multe odată

/**
 * Ține progresul sincronizat cu serverul pentru utilizatorul conectat: la conectare îl aduce de pe server
 * (și îl îmbină cu cel local), apoi îl trimite la câteva secunde după fiecare modificare și când fila e ascunsă.
 */
export default function ProgressSync() {
  const { user } = useAuth()
  const userKey = user?.id

  useEffect(() => {
    if (!userKey) return undefined
    let timer
    const run = (opts) => syncProgress(userKey, opts).catch(() => {}) // fără server: încercăm la următoarea modificare

    run()
    const onChange = (e) => {
      if (e.detail !== userKey) return
      clearTimeout(timer)
      timer = setTimeout(run, DELAY_MS)
    }
    const onHide = () => {
      if (document.visibilityState !== 'hidden') return
      clearTimeout(timer)
      run({ keepalive: true })
    }
    window.addEventListener(PROGRESS_EVENT, onChange)
    document.addEventListener('visibilitychange', onHide)
    return () => {
      clearTimeout(timer)
      window.removeEventListener(PROGRESS_EVENT, onChange)
      document.removeEventListener('visibilitychange', onHide)
    }
  }, [userKey])

  return null
}
