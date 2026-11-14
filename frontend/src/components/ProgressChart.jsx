import { useEffect, useRef } from 'react'
import { formatDuration } from '../progress/format.js'

const W = 680
const H = 270
const PAD = { l: 44, r: 44, t: 18, b: 34 }

/** Bare = minute de studiu activ (axa stângă), linie = % răspunsuri corecte (axa dreaptă). */
export default function ProgressChart({ series, unit }) {
  const scroller = useRef(null)
  // pe ecrane înguste graficul se derulează; pornim de la zilele cele mai recente
  useEffect(() => {
    const el = scroller.current
    if (el) el.scrollLeft = el.scrollWidth
  }, [series])
  const iw = W - PAD.l - PAD.r
  const ih = H - PAD.t - PAD.b
  const maxMin = Math.max(10, ...series.map((p) => p.sec / 60))
  const top = Math.ceil(maxMin / 10) * 10
  const step = iw / series.length
  const x = (i) => PAD.l + step * i + step / 2
  const yMin = (m) => PAD.t + ih - (m / top) * ih
  const yScore = (s) => PAD.t + ih - (s / 100) * ih
  const barW = Math.min(34, step * 0.56)

  // linia de scor se întrerupe în zilele fără răspunsuri
  const segments = []
  let cur = []
  series.forEach((p, i) => {
    if (p.score === null) {
      if (cur.length) segments.push(cur)
      cur = []
    } else cur.push([x(i), yScore(p.score)])
  })
  if (cur.length) segments.push(cur)

  const labelEvery = series.length > 10 ? 2 : 1
  const summary = series
    .map((p) => `${p.label}: ${formatDuration(p.sec)}${p.score === null ? '' : `, ${p.score}% corecte`}`)
    .join('; ')

  return (
    <figure className="pg-chart">
      <div className="pg-chart-scroll" ref={scroller}>
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label={`Grafic pe ${unit}. ${summary}`}>
        {[0, 0.5, 1].map((f) => (
          <g key={f}>
            <line className="pg-grid" x1={PAD.l} x2={W - PAD.r} y1={yMin(top * f)} y2={yMin(top * f)} />
            <text className="pg-axis" x={PAD.l - 8} y={yMin(top * f) + 4} textAnchor="end">
              {Math.round(top * f)}m
            </text>
            <text className="pg-axis score" x={W - PAD.r + 8} y={yScore(100 * f) + 4}>
              {Math.round(100 * f)}%
            </text>
          </g>
        ))}
        {series.map((p, i) => (
          <g key={i}>
            <rect
              className="pg-bar"
              x={x(i) - barW / 2}
              y={yMin(p.sec / 60)}
              width={barW}
              height={Math.max(p.sec > 0 ? 3 : 0, PAD.t + ih - yMin(p.sec / 60))}
              rx="6"
            >
              <title>{`${p.label}: ${formatDuration(p.sec)}${p.score === null ? '' : ` · ${p.score}% corecte`}`}</title>
            </rect>
            {i % labelEvery === (series.length - 1) % labelEvery && (
              <text className="pg-axis" x={x(i)} y={H - 10} textAnchor="middle">
                {p.label}
              </text>
            )}
          </g>
        ))}
        {segments.map((seg, i) =>
          seg.length > 1 ? <polyline key={i} className="pg-line" points={seg.map((p) => p.join(',')).join(' ')} /> : null,
        )}
        {series.map((p, i) =>
          p.score === null ? null : <circle key={i} className="pg-pt" cx={x(i)} cy={yScore(p.score)} r="4.5" />,
        )}
      </svg>
      </div>
      <figcaption className="pg-legend">
        <span>
          <i className="k-bar" /> Timp activ (minute)
        </span>
        <span>
          <i className="k-line" /> Răspunsuri corecte (%)
        </span>
      </figcaption>
    </figure>
  )
}
