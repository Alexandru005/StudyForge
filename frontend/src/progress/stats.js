import { dayKey } from './store.js'

/** Un număr de zile înainte/după, păstrând ora locală (fără probleme la schimbarea orei). */
export function addDays(date, n) {
  const d = new Date(date)
  d.setDate(d.getDate() + n)
  return d
}

/** Începutul săptămânii (luni) pentru o dată. */
export function weekStart(date) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7))
  return d
}

const sum = (a, b) => ({ sec: a.sec + b.sec, ok: a.ok + b.ok, bad: a.bad + b.bad })
const ZERO = { sec: 0, ok: 0, bad: 0 }

/** Totalul unei zile, pentru o materie (subjectKey) sau pentru toate (null). */
export function dayTotals(progress, day, subjectKey = null) {
  const entry = progress.days[day]
  if (!entry) return ZERO
  let t = ZERO
  for (const [key, b] of Object.entries(entry)) {
    if (subjectKey && key !== subjectKey) continue
    t = sum(t, b)
  }
  return t
}

/** Totalul pe un interval de zile [from, to] inclusiv. */
export function rangeTotals(progress, from, to, subjectKey = null) {
  let t = ZERO
  for (let d = new Date(from); d <= to; d = addDays(d, 1)) t = sum(t, dayTotals(progress, dayKey(d), subjectKey))
  return t
}

/** Totalul pe toată perioada înregistrată. */
export function allTotals(progress, subjectKey = null) {
  let t = ZERO
  for (const day of Object.keys(progress.days)) t = sum(t, dayTotals(progress, day, subjectKey))
  return t
}

/** Procentul de răspunsuri corecte, sau null dacă nu există încă răspunsuri. */
export function accuracy({ ok, bad }) {
  const n = ok + bad
  return n ? Math.round((ok / n) * 100) : null
}

/** Materiile cunoscute (din progres), cele cu mai mult timp primele. */
export function subjectList(progress) {
  return Object.entries(progress.subjects)
    .map(([key, s]) => ({ key, title: s.title, ...allTotals(progress, key) }))
    .sort((a, b) => b.sec - a.sec || a.title.localeCompare(b.title))
}

/** Seria pe ultimele `n` zile (cea mai veche prima). */
export function dailySeries(progress, subjectKey, n = 14, today = new Date()) {
  return Array.from({ length: n }, (_, i) => {
    const date = addDays(today, i - (n - 1))
    const t = dayTotals(progress, dayKey(date), subjectKey)
    return { date, label: date.getDate() + '/' + (date.getMonth() + 1), ...t, score: accuracy(t) }
  })
}

/** Seria pe ultimele `n` săptămâni calendaristice (luni–duminică). */
export function weeklySeries(progress, subjectKey, n = 8, today = new Date()) {
  const start = weekStart(today)
  return Array.from({ length: n }, (_, i) => {
    const from = addDays(start, (i - (n - 1)) * 7)
    const t = rangeTotals(progress, from, addDays(from, 6), subjectKey)
    return { date: from, label: from.getDate() + '/' + (from.getMonth() + 1), ...t, score: accuracy(t) }
  })
}

/** Scorul pe săptămânile din plan (Săptămâna 1, 2, …), pentru o materie sau pentru toate. */
export function planWeekScores(progress, subjectKey = null) {
  const byWeek = {}
  for (const entry of Object.values(progress.days)) {
    for (const [key, b] of Object.entries(entry)) {
      if (subjectKey && key !== subjectKey) continue
      for (const [w, r] of Object.entries(b.weeks ?? {})) {
        byWeek[w] ??= { ok: 0, bad: 0 }
        byWeek[w].ok += r.ok
        byWeek[w].bad += r.bad
      }
    }
  }
  return Object.entries(byWeek)
    .map(([w, r]) => ({ week: Number(w), ...r, score: accuracy({ ok: r.ok, bad: r.bad }) }))
    .sort((a, b) => a.week - b.week)
}

/** Dispoziția mascotei după procentul de răspunsuri corecte. */
export const MOOD_HAPPY_FROM = 70
export const MOOD_SAD_BELOW = 40
export function moodFor(score) {
  if (score === null || score === undefined) return 'neutral'
  if (score >= MOOD_HAPPY_FROM) return 'happy'
  if (score < MOOD_SAD_BELOW) return 'sad'
  return 'neutral'
}

/** Diferența (în puncte procentuale) dintre două scoruri, sau null dacă lipsește unul. */
export function scoreDelta(now, before) {
  return now === null || before === null ? null : now - before
}

