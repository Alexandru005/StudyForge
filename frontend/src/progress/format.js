/** Durată din secunde: „45 s”, „14 min”, „2 h 05 min”. */
export function formatDuration(sec) {
  const s = Math.round(sec)
  if (s < 60) return `${s} s`
  const m = Math.round(s / 60)
  if (m < 60) return `${m} min`
  const h = Math.floor(m / 60)
  return `${h} h ${String(m % 60).padStart(2, '0')} min`
}

export const percent = (n) => (n === null || n === undefined ? '–' : `${n}%`)
