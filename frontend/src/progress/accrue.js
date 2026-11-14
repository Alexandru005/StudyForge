/** Cât timp (secunde) se adaugă la un tic: doar dacă fila e vizibilă și ultima acțiune e recentă. */
export const IDLE_MS = 60_000
export const MAX_STEP_S = 5 // un tic ratat (laptop adormit etc.) nu adaugă mai mult de atât

export function accrue({ lastTick, lastInput, now, visible, idleMs = IDLE_MS }) {
  if (!visible || now - lastInput > idleMs) return 0
  return Math.min(Math.max(now - lastTick, 0) / 1000, MAX_STEP_S)
}
