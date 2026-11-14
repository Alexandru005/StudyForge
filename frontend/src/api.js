/**
 * Clientul pentru backend-ul StudyForge.
 *
 * Endpoint-uri folosite:
 *   POST /api/auth/register  { username, email, gender, password } -> { token, user }
 *   POST /api/auth/login     { email, password }                   -> { token, user }
 *   GET  /api/auth/me                     -> user
 *   GET  /api/status                     -> { status, geminiConfigured, geminiModel }
 *   GET  /api/library                     -> { plans: PlanSummary[], quizzes: QuizSummary[] }
 *   POST /api/plans        (multipart: file, startDate?)  -> { plan, content }
 *   GET  /api/plans        -> PlanSummary[]
 *   GET  /api/plans/{id}   -> { plan, content }
 *   GET  /api/plans/{id}/pdf[?download=true]
 *   POST /api/quizzes      (multipart: file, week, count?) -> { quiz, flashcards }
 *   GET  /api/quizzes[?planId=]           -> QuizSummary[]
 *   GET  /api/quizzes/{id} -> { quiz, flashcards }
 *   GET  /api/quizzes/{id}/pdf[?download=true]
 *
 * Erorile vin ca JSON (ProblemDetail) cu mesajul pentru utilizator în câmpul "detail".
 */

// Gol = același origin (în dev, Vite trimite /api către backend). Poți seta VITE_API_URL.
const BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/+$/, '')

const isAbsolute = (url) => /^https?:\/\//i.test(url)

/** Adresa completă pentru o cale de backend (ex. /api/plans/{id}/pdf). */
export const apiUrl = (path) => (!path ? '' : isAbsolute(path) ? path : BASE + path)

// ------------------------------------------------------------------ sesiune (token JWT)
// Backend-ul cere `Authorization: Bearer <token>` la toate rutele, în afară de
// /api/auth/register, /api/auth/login și /api/status.

const TOKEN_KEY = 'studyforge.token'
const USER_KEY = 'studyforge.user'

function storageGet(key) {
  try {
    return window.localStorage.getItem(key)
  } catch {
    return null
  }
}

function storageSet(key, value) {
  try {
    if (value == null) window.localStorage.removeItem(key)
    else window.localStorage.setItem(key, value)
  } catch {
    // stocare indisponibilă (mod privat): sesiunea ține doar până la reîncărcare
  }
}

let token = storageGet(TOKEN_KEY)

export const getToken = () => token

/** Utilizatorul salvat la ultima autentificare (sau null). */
export function getSavedUser() {
  try {
    return JSON.parse(storageGet(USER_KEY) || 'null')
  } catch {
    return null
  }
}

/** Salvează (sau șterge, cu null) token-ul și utilizatorul. */
export function saveSession(auth) {
  token = auth?.token ?? null
  storageSet(TOKEN_KEY, token)
  storageSet(USER_KEY, auth?.user ? JSON.stringify(auth.user) : null)
}

export function saveUser(user) {
  storageSet(USER_KEY, user ? JSON.stringify(user) : null)
}

/** Evenimentul trimis când backend-ul răspunde 401 (token expirat / invalid). */
export const UNAUTHORIZED_EVENT = 'studyforge:unauthorized'

/**
 * Link pentru <a href> către un PDF salvat. Un link simplu nu poate trimite header-ul
 * Authorization, așa că token-ul merge ca `?access_token=` (backend-ul acceptă asta la GET).
 */
export function pdfHref(path) {
  const url = apiUrl(path)
  if (!url || !token) return url
  return url + (url.includes('?') ? '&' : '?') + 'access_token=' + encodeURIComponent(token)
}

export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request(path, init = {}) {
  const headers = new Headers(init.headers)
  if (token && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`)
  let res
  try {
    res = await fetch(apiUrl(path), { ...init, headers })
  } catch {
    throw new ApiError('Nu mă pot conecta la server. Verifică dacă backend-ul rulează (portul 8080).')
  }
  if (!res.ok) {
    let detail = ''
    try {
      const body = await res.json()
      detail = body.detail || body.title || ''
    } catch {
      // răspuns fără JSON (de ex. proxy-ul când backend-ul e oprit)
    }
    if (!detail && res.status >= 500) {
      detail = 'Serverul nu răspunde sau a întâmpinat o eroare. Verifică dacă backend-ul rulează.'
    }
    // Sesiune expirată: la login/register un 401 înseamnă doar parolă greșită.
    if (res.status === 401 && token && !path.startsWith('/api/auth/login')) {
      saveSession(null)
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT, { detail }))
    }
    throw new ApiError(detail || `Cererea a eșuat (${res.status}).`, res.status)
  }
  return res
}

const json = async (path, init) => (await request(path, init)).json()

const postJson = (path, data) =>
  json(path, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) })

/** { username: "Popescu Ion", email, gender: "MALE"|"FEMALE"|"OTHER", password } -> { token, tokenType, expiresAt, user } */
export const register = (data) => postJson('/api/auth/register', data)

/** { email, password } -> { token, tokenType, expiresAt, user } */
export const login = (data) => postJson('/api/auth/login', data)

/** Utilizatorul curent (verifică dacă token-ul salvat mai e valid). */
export const getMe = () => json('/api/auth/me')

export const getStatus = () => json('/api/status')
export const getLibrary = () => json('/api/library')

export const listPlans = () => json('/api/plans')
export const getPlan = (id) => json(`/api/plans/${encodeURIComponent(id)}`)

/** Programa (PDF) -> plan pe săptămâni. startDate: "YYYY-MM-DD", opțional. */
export function createPlan(file, startDate) {
  const body = new FormData()
  body.append('file', file)
  if (startDate) body.append('startDate', startDate)
  return json('/api/plans', { method: 'POST', body })
}

export const listQuizzes = (planId) =>
  json(planId ? `/api/quizzes?planId=${encodeURIComponent(planId)}` : '/api/quizzes')
export const getQuiz = (id) => json(`/api/quizzes/${encodeURIComponent(id)}`)

/** PDF-ul planului + săptămâna (de la 1) + numărul de carduri (5-40) -> set de flashcard-uri. */
export function createQuiz({ file, week, count }) {
  const body = new FormData()
  body.append('file', file)
  body.append('week', String(week))
  if (count) body.append('count', String(count))
  return json('/api/quizzes', { method: 'POST', body })
}

/** Descarcă un PDF salvat și îl întoarce ca fișier, ca să poată fi retrimis la POST /api/quizzes. */
export async function fetchPdfAsFile(pdfUrl, fileName = 'plan.pdf') {
  const blob = await (await request(pdfUrl)).blob()
  return new File([blob], fileName, { type: 'application/pdf' })
}
