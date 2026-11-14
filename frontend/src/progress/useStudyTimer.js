import { useEffect } from 'react'
import { accrue } from './accrue.js'
import { addSeconds } from './store.js'

const ACTIVITY_EVENTS = ['pointerdown', 'pointermove', 'keydown', 'wheel', 'touchstart', 'scroll']
const FLUSH_EVERY_S = 5

/**
 * Numără timpul de studiu ACTIV pentru o materie: doar cât fila e vizibilă și ai făcut o acțiune
 * în ultimul minut. Se salvează la fiecare 5 secunde, la ascunderea filei și la ieșirea din pagină.
 */
export function useStudyTimer({ userKey, subjectKey, subjectTitle, enabled }) {
  useEffect(() => {
    if (!enabled || !subjectKey) return undefined
    const subject = { key: subjectKey, title: subjectTitle || subjectKey }
    let lastTick = Date.now()
    let lastInput = lastTick
    let pending = 0

    const touch = () => {
      lastInput = Date.now()
    }
    const flush = () => {
      if (pending > 0) addSeconds(userKey, subject, pending)
      pending = 0
    }
    const tick = () => {
      const now = Date.now()
      pending += accrue({ lastTick, lastInput, now, visible: document.visibilityState === 'visible' })
      lastTick = now
      if (pending >= FLUSH_EVERY_S) flush()
    }
    const onVisibility = () => {
      tick()
      if (document.visibilityState !== 'visible') flush()
    }

    const id = setInterval(tick, 1000)
    ACTIVITY_EVENTS.forEach((ev) => window.addEventListener(ev, touch, { passive: true, capture: true }))
    document.addEventListener('visibilitychange', onVisibility)
    window.addEventListener('pagehide', flush)
    return () => {
      clearInterval(id)
      tick()
      flush()
      ACTIVITY_EVENTS.forEach((ev) => window.removeEventListener(ev, touch, { capture: true }))
      document.removeEventListener('visibilitychange', onVisibility)
      window.removeEventListener('pagehide', flush)
    }
  }, [userKey, subjectKey, subjectTitle, enabled])
}
