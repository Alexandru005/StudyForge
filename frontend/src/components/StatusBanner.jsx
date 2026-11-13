import { useEffect, useState } from 'react'
import { getStatus } from '../api.js'
import { IconWarn } from './icons.jsx'

/** Avertizează dacă backend-ul nu răspunde sau dacă cheia Gemini nu e setată (GET /api/status). */
export default function StatusBanner() {
  const [state, setState] = useState(null)

  useEffect(() => {
    let alive = true
    getStatus()
      .then((s) => alive && setState({ ok: true, gemini: s.geminiConfigured }))
      .catch((e) => alive && setState({ ok: false, message: e.message }))
    return () => {
      alive = false
    }
  }, [])

  if (!state) return null
  if (!state.ok) {
    return (
      <div className="status-slot">
        <div className="warn" role="alert">
          <IconWarn />
          <span>{state.message}</span>
        </div>
      </div>
    )
  }
  if (!state.gemini) {
    return (
      <div className="status-slot">
        <div className="warn" role="alert">
          <IconWarn />
          <span>
            Cheia Gemini nu este setată în backend (variabila GEMINI_API_KEY din .env), deci generarea de planuri și
            flashcard-uri nu va funcționa.
          </span>
        </div>
      </div>
    )
  }
  return null
}
