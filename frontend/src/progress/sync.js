import { getProgress, saveProgress } from '../api.js'
import { loadProgress, mergeProgress, replaceProgress } from './store.js'

/*
 * Progresul se salvează și pe server, ca să nu se piardă odată cu datele browserului.
 * 1. La conectare: progresul de pe server se îmbină cu cel local (restoreProgress).
 * 2. După fiecare modificare: progresul complet se trimite la server (syncProgress).
 * Nu trimitem nimic înainte de pasul 1, ca un browser gol să nu suprascrie progresul de pe server.
 */

const KEEPALIVE_MAX = 60_000 // browserele acceptă cel mult ~64 KB la cererile keepalive
const restored = new Map() // userKey -> Promise (îmbinarea cu serverul, o dată pe sesiune)
const lastSent = new Map() // userKey -> JSON-ul trimis ultima dată
const running = new Map() // userKey -> Promise (o singură trimitere odată)

/** Îmbină progresul de pe server cu cel din browser (o singură dată pe sesiune, reîncearcă după o eroare). */
export function restoreProgress(userKey) {
  if (!restored.has(userKey)) {
    const p = getProgress().then(({ data }) => {
      if (!data) return
      const local = loadProgress(userKey)
      const merged = mergeProgress(local, data)
      if (JSON.stringify(merged) !== JSON.stringify(local)) replaceProgress(userKey, merged)
      lastSent.set(userKey, JSON.stringify(mergeProgress(data, null)))
    })
    p.catch(() => restored.delete(userKey)) // fără server: încercăm din nou data viitoare
    restored.set(userKey, p)
  }
  return restored.get(userKey)
}

/** Trimite progresul la server dacă s-a schimbat. Rezolvă cu true dacă a trimis ceva. */
export function syncProgress(userKey, { keepalive = false } = {}) {
  if (!userKey) return Promise.resolve(false)
  const previous = running.get(userKey) ?? Promise.resolve()
  const next = previous
    .catch(() => {})
    .then(() => restoreProgress(userKey))
    .then(async () => {
      const data = loadProgress(userKey)
      const body = JSON.stringify(data)
      if (body === lastSent.get(userKey)) return false
      await saveProgress(data, { keepalive: keepalive && body.length < KEEPALIVE_MAX })
      lastSent.set(userKey, body)
      return true
    })
  running.set(userKey, next)
  return next
}
