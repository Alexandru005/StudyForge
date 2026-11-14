/*
 * Progresul de studiu se păstrează în browser, separat pentru fiecare cont, și se salvează și pe server
 * (vezi sync.js): la conectare se îmbină cu cel de pe server, deci nu se pierde dacă schimbi browserul
 * sau adresa ori se șterg datele din browser. Structura:
 *   { v: 1,
 *     subjects: { [subjectKey]: { title } },
 *     days: { 'YYYY-MM-DD': { [subjectKey]: { sec, ok, bad, weeks: { [nr]: { ok, bad } } } } } }
 * `sec` = secunde active, `ok`/`bad` = răspunsuri „știut” / „de repetat” date în ziua respectivă.
 */

export const GUEST_KEY = 'guest'
const storageKey = (userKey) => `studyforge:progress:v1:${userKey || GUEST_KEY}`

/** Cheia locală a unei zile, după ora dispozitivului: YYYY-MM-DD. */
export function dayKey(date = new Date()) {
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${m}-${d}`
}

/** Materia unui set sau a unui plan: un plan salvat, altfel fișierul sursă. */
export function subjectOf({ planId, planTitle, title, sourceFileName }) {
  if (planId) return { key: `plan:${planId}`, title: planTitle || title || 'Plan fără titlu' }
  if (sourceFileName) return { key: `file:${sourceFileName}`, title: sourceFileName.replace(/\.pdf$/i, '') }
  return { key: 'other', title: 'Fără materie' }
}

const empty = () => ({ v: 1, subjects: {}, days: {} })

export function loadProgress(userKey) {
  try {
    const raw = JSON.parse(localStorage.getItem(storageKey(userKey)) || 'null')
    if (raw && raw.v === 1 && raw.days && raw.subjects) return raw
  } catch {
    // date corupte -> pornim de la zero
  }
  return empty()
}

/** Evenimentul trimis la fiecare modificare a progresului (detail = userKey), pentru sincronizarea cu serverul. */
export const PROGRESS_EVENT = 'studyforge:progress-changed'

function save(userKey, data) {
  try {
    localStorage.setItem(storageKey(userKey), JSON.stringify(data))
  } catch {
    // spațiu de stocare indisponibil -> progresul acestei sesiuni se pierde
  }
  window.dispatchEvent(new CustomEvent(PROGRESS_EVENT, { detail: userKey || GUEST_KEY }))
}

/** Înlocuiește progresul local (ex. după îmbinarea cu cel de pe server). */
export function replaceProgress(userKey, data) {
  save(userKey, data)
}

const num = (n) => (Number.isFinite(Number(n)) && Number(n) > 0 ? Number(n) : 0)

/**
 * Îmbină două progrese (local + server): pentru fiecare zi, materie și săptămână din plan păstrează
 * valoarea mai mare, deci nimic nu se numără de două ori și nimic nu se pierde.
 */
export function mergeProgress(a, b) {
  const out = { v: 1, subjects: { ...(b?.subjects ?? {}), ...(a?.subjects ?? {}) }, days: {} }
  for (const src of [a, b]) {
    for (const [day, entry] of Object.entries(src?.days ?? {})) {
      for (const [key, x] of Object.entries(entry ?? {})) {
        if (!x) continue
        out.days[day] ??= {}
        const cur = (out.days[day][key] ??= { sec: 0, ok: 0, bad: 0, weeks: {} })
        cur.sec = Math.max(cur.sec, num(x.sec))
        cur.ok = Math.max(cur.ok, num(x.ok))
        cur.bad = Math.max(cur.bad, num(x.bad))
        for (const [w, r] of Object.entries(x.weeks ?? {})) {
          const cw = (cur.weeks[w] ??= { ok: 0, bad: 0 })
          cw.ok = Math.max(cw.ok, num(r?.ok))
          cw.bad = Math.max(cw.bad, num(r?.bad))
        }
        out.subjects[key] ??= { title: key }
      }
    }
  }
  return out
}

function bucket(data, day, subject) {
  data.subjects[subject.key] = { title: subject.title }
  data.days[day] ??= {}
  data.days[day][subject.key] ??= { sec: 0, ok: 0, bad: 0, weeks: {} }
  return data.days[day][subject.key]
}

/** Adaugă secunde de studiu activ pentru o materie. */
export function addSeconds(userKey, subject, seconds, now = new Date()) {
  if (!(seconds > 0)) return
  const data = loadProgress(userKey)
  bucket(data, dayKey(now), subject).sec += Math.round(seconds * 10) / 10
  save(userKey, data)
}

/** Înregistrează un răspuns la un card: result = 'ok' | 'bad'. */
export function addAttempt(userKey, subject, weekNumber, result, now = new Date()) {
  if (result !== 'ok' && result !== 'bad') return
  const data = loadProgress(userKey)
  const b = bucket(data, dayKey(now), subject)
  b[result] += 1
  if (weekNumber >= 1) {
    b.weeks[weekNumber] ??= { ok: 0, bad: 0 }
    b.weeks[weekNumber][result] += 1
  }
  save(userKey, data)
}
