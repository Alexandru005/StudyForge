/** Funcții de formatare (date, numere, nume de fișiere) în română. */

/** "2026-11-16" -> Date locală (fără decalaj de fus orar). */
export function parseIsoDate(iso) {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso ?? '')
  return m ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])) : null
}

export function formatDate(iso, withYear = false) {
  const d = parseIsoDate(iso)
  if (!d) return ''
  return d.toLocaleDateString('ro-RO', { day: 'numeric', month: 'short', ...(withYear ? { year: 'numeric' } : {}) })
}

export function formatRange(startIso, endIso) {
  const a = formatDate(startIso)
  const b = formatDate(endIso)
  return a && b ? `${a} – ${b}` : a || b
}

/** Instant ISO din backend (createdAt) -> "13 nov. 2026". */
export function formatCreated(instant) {
  const d = new Date(instant)
  return Number.isNaN(d.getTime())
    ? ''
    : d.toLocaleDateString('ro-RO', { day: 'numeric', month: 'short', year: 'numeric' })
}

/** Data de azi ca "YYYY-MM-DD", în ora locală. */
export function todayIso() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 1 săptămână · 2 săptămâni · 20 de săptămâni */
export function countLabel(n, one, many) {
  if (n === 1) return `1 ${one}`
  const mod = n % 100
  return `${n} ${mod === 0 || mod >= 20 ? 'de ' : ''}${many}`
}

/** "Analiză matematică I" -> "analiza-matematica-i" */
export function fileSlug(text, fallback = 'plan') {
  const s = (text ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return s || fallback
}
