import { useEffect } from 'react'

/**
 * Înclinare 3D după poziția mouse-ului (la fel ca în design).
 * hostRef = elementul peste care se mișcă mouse-ul; targetRef = elementul care se înclină (implicit același).
 * Setează --rx/--ry/--mx/--my și clasa "is-tilting" pe target.
 */
export function useTilt(hostRef, { max = 10, targetRef = hostRef, enabled = true } = {}) {
  useEffect(() => {
    const host = hostRef.current
    const target = targetRef.current
    if (!enabled || !host || !target) return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

    let raf = 0
    const onMove = (e) => {
      if (e.pointerType !== 'mouse') return
      const r = host.getBoundingClientRect()
      const x = (e.clientX - r.left) / r.width
      const y = (e.clientY - r.top) / r.height
      cancelAnimationFrame(raf)
      raf = requestAnimationFrame(() => {
        target.classList.add('is-tilting')
        target.style.setProperty('--ry', ((x - 0.5) * 2 * max).toFixed(2) + 'deg')
        target.style.setProperty('--rx', ((0.5 - y) * 2 * max).toFixed(2) + 'deg')
        target.style.setProperty('--mx', (x * 100).toFixed(1) + '%')
        target.style.setProperty('--my', (y * 100).toFixed(1) + '%')
      })
    }
    const onLeave = () => {
      cancelAnimationFrame(raf)
      target.classList.remove('is-tilting')
      target.style.setProperty('--rx', '0deg')
      target.style.setProperty('--ry', '0deg')
    }
    host.addEventListener('pointermove', onMove)
    host.addEventListener('pointerleave', onLeave)
    return () => {
      cancelAnimationFrame(raf)
      host.removeEventListener('pointermove', onMove)
      host.removeEventListener('pointerleave', onLeave)
    }
  }, [hostRef, targetRef, max, enabled])
}
