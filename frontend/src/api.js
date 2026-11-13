/**
 * Clientul pentru backend-ul StudyForge.
 *
 * Endpoint-uri folosite:
 *   GET  /api/status                      -> { status, geminiConfigured, geminiModel }
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

/** Adresa de folosit în <a href> pentru căile returnate de backend (ex. /api/plans/{id}/pdf). */
export const apiUrl = (path) => (!path ? '' : isAbsolute(path) ? path : BASE + path)

export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request(path, init) {
  let res
  try {
    res = await fetch(apiUrl(path), init)
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
    throw new ApiError(detail || `Cererea a eșuat (${res.status}).`, res.status)
  }
  return res
}

const json = async (path, init) => (await request(path, init)).json()

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
