import { useEffect, useRef, useState } from 'react'
import { getJoke } from '../api.js'
import mascota from '../assets/mascota.svg'

/**
 * Butonul din stânga-jos: o glumă pasiv-agresivă (cu drag) generată de AI din planul tău de studiu.
 * POST /api/jokes { previousJoke } -> { joke, subject }. Gluma nu se salvează nicăieri.
 */
export default function JokeBubble() {
  const [open, setOpen] = useState(false)
  const [joke, setJoke] = useState(null) // { text, subject }
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [count, setCount] = useState(0)
  const fabRef = useRef(null)
  const req = useRef(0)

  async function generate(previous) {
    const n = ++req.current
    setLoading(true)
    setError('')
    try {
      const res = await getJoke(previous)
      if (n !== req.current) return
      setJoke({ text: res.joke, subject: res.subject })
      setCount((c) => c + 1)
    } catch (e) {
      if (n === req.current) setError(e.message)
    } finally {
      if (n === req.current) setLoading(false)
    }
  }

  function toggle() {
    if (open) {
      close()
    } else {
      setOpen(true)
      generate(joke?.text)
    }
  }

  function close() {
    req.current++ // răspunsul în curs nu mai contează
    setLoading(false)
    setOpen(false)
  }

  useEffect(() => {
    if (!open) return undefined
    const onKey = (e) => {
      if (e.key === 'Escape') {
        setOpen(false)
        fabRef.current?.focus()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open])

  return (
    <>
      <button
        ref={fabRef}
        type="button"
        className={'sf-joke-fab' + (open ? ' is-on' : '')}
        aria-label="Generează o glumă despre materie"
        aria-expanded={open}
        aria-controls="sf-joke-card"
        onClick={toggle}
      >
        <svg viewBox="0 0 48 48" aria-hidden="true">
          <path className="bub" d="M8 10h32a4 4 0 0 1 4 4v17a4 4 0 0 1-4 4H22l-9 8v-8H8a4 4 0 0 1-4-4V14a4 4 0 0 1 4-4z" fill="#e07a94" />
          <circle className="ink" cx="17" cy="21" r="2.4" fill="#17060a" />
          <circle className="ink" cx="31" cy="21" r="2.4" fill="#17060a" />
          <path className="ink" d="M15 26.5c2.6 4.6 15.4 4.6 18 0-3 1.4-15 1.4-18 0z" fill="#17060a" />
          <path d="M36 5l1.3 3 3 1.3-3 1.3L36 14l-1.3-3.4-3-1.3 3-1.3z" fill="#ffe9a8" />
        </svg>
        <span className="sf-joke-tip">Glumă pasiv-agresivă din materie</span>
      </button>

      {open && (
        <section className="sf-joke-card" id="sf-joke-card" role="dialog" aria-labelledby="sf-joke-title">
          <div className="sf-joke-head">
            <div className="sf-joke-logo">
              <img src={mascota} alt="Study Forge" />
            </div>
            <h2 id="sf-joke-title">
              Study Forge are o vorbă cu tine<small>Glumă generată din materialele tale</small>
            </h2>
            <button
              type="button"
              className="sf-joke-x"
              aria-label="Închide gluma"
              onClick={() => {
                close()
                fabRef.current?.focus()
              }}
            >
              ×
            </button>
          </div>
          <div className="sf-joke-body" aria-live="polite">
            <span className="sf-joke-q" aria-hidden="true">
              “
            </span>
            {loading ? (
              <div aria-label="Se generează o glumă">
                <div className="sf-joke-sk" />
                <div className="sf-joke-sk" />
                <div className="sf-joke-sk" />
              </div>
            ) : error ? (
              <p className="sf-joke-text sf-joke-err">{error}</p>
            ) : (
              <p className="sf-joke-text">{joke?.text}</p>
            )}
          </div>
          <div className="sf-joke-meta">
            {joke?.subject && <span className="sf-joke-chip">{joke.subject}</span>}
            <span className="sf-joke-chip ai">Generat de AI</span>
          </div>
          <div className="sf-joke-foot">
            <span>{count > 0 ? `Gluma ${count}` : ''}</span>
            <button type="button" className="sf-joke-more" disabled={loading} onClick={() => generate(joke?.text)}>
              Altă glumă
            </button>
          </div>
        </section>
      )}
    </>
  )
}
