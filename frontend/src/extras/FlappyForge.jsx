import { useEffect, useRef, useState } from 'react'
import icon from '../assets/flappy-icon.svg'
import { createFlappy, readBest } from './flappyEngine.js'

/** Butonul din dreapta-jos: o pauză scurtă cu Flappy Forge (canvas, fără server). */
export default function FlappyForge() {
  const [open, setOpen] = useState(false)
  const [best, setBest] = useState(readBest)
  const canvasRef = useRef(null)
  const fabRef = useRef(null)
  const closeRef = useRef(null)

  useEffect(() => {
    if (!open) return undefined
    const game = createFlappy(canvasRef.current, { onBest: setBest })
    game.start()
    closeRef.current?.focus()
    const fab = fabRef.current
    const onKey = (e) => {
      if (e.key === 'Escape') setOpen(false)
      else if (e.code === 'Space' || e.key === ' ' || e.key === 'ArrowUp') {
        e.preventDefault()
        game.flap()
      }
    }
    document.addEventListener('keydown', onKey)
    canvasRef.current.onpointerdown = (e) => {
      e.preventDefault()
      game.flap()
    }
    return () => {
      game.stop()
      document.removeEventListener('keydown', onKey)
      fab?.focus()
    }
  }, [open])

  return (
    <>
      <button
        ref={fabRef}
        type="button"
        className={'sf-game-fab' + (open ? ' is-on' : '')}
        aria-label="Deschide jocul Flappy Forge"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        <img src={icon} alt="" />
        <span className="sf-game-badge" aria-hidden="true" />
        <span className="sf-game-tip">Pauză? Joacă Flappy Forge</span>
      </button>
      {open && (
        <div className="sf-game-overlay" onClick={(e) => e.target === e.currentTarget && setOpen(false)}>
          <div className="sf-game-card" role="dialog" aria-modal="true" aria-labelledby="sf-game-title">
            <div className="sf-game-head">
              <img src={icon} alt="" />
              <h2 id="sf-game-title">
                Flappy Forge<small>O pauză scurtă între sesiuni</small>
              </h2>
              <button ref={closeRef} type="button" className="sf-game-close" aria-label="Închide jocul" onClick={() => setOpen(false)}>
                ×
              </button>
            </div>
            <div className="sf-game-stage">
              <canvas
                ref={canvasRef}
                width="380"
                height="520"
                aria-label="Joc Flappy Forge. Apasă Spațiu sau click ca să zbori."
              />
            </div>
            <div className="sf-game-foot">
              <span>Spațiu / click = zbori</span>
              <span>
                Record: <b>{best}</b>
              </span>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
