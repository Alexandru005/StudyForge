import { avatarFor } from '../auth/avatars.js'

const MOODS = {
  happy: 'fericit',
  neutral: 'indiferent',
  sad: 'trist',
}

/** Mascota contului, mare, cu o dispoziție: fericită (sărituri + steluțe), indiferentă sau tristă (nor + lacrimă). */
export default function MoodMascot({ gender, mood = 'neutral', size = 220 }) {
  const a = avatarFor(gender)
  return (
    <div className={'mood mood-' + mood} style={{ width: size, height: size }} role="img" aria-label={`${a.name} e ${MOODS[mood]}`}>
      <img className="mood-img" src={a.src} alt="" width={size} height={size} />
      {mood === 'happy' && (
        <svg className="mood-fx" viewBox="0 0 220 220" aria-hidden="true">
          <path className="spark s1" d="M30 52 l4 11 11 4 -11 4 -4 11 -4 -11 -11 -4 11 -4z" />
          <path className="spark s2" d="M188 40 l3 8 8 3 -8 3 -3 8 -3 -8 -8 -3 8 -3z" />
          <path className="spark s3" d="M196 128 l4 11 11 4 -11 4 -4 11 -4 -11 -11 -4 11 -4z" />
          <path className="spark s4" d="M20 150 l3 8 8 3 -8 3 -3 8 -3 -8 -8 -3 8 -3z" />
        </svg>
      )}
      {mood === 'neutral' && (
        <svg className="mood-fx" viewBox="0 0 220 220" aria-hidden="true">
          <circle className="dot d1" cx="168" cy="40" r="5" />
          <circle className="dot d2" cx="186" cy="30" r="6.5" />
          <circle className="dot d3" cx="208" cy="20" r="8" />
        </svg>
      )}
      {mood === 'sad' && (
        <svg className="mood-fx" viewBox="0 0 220 220" aria-hidden="true">
          <g className="rain">
            <ellipse cx="110" cy="22" rx="52" ry="16" className="cloud" />
            <circle cx="82" cy="20" r="18" className="cloud" />
            <circle cx="116" cy="12" r="22" className="cloud" />
            <circle cx="142" cy="22" r="16" className="cloud" />
            <path className="drop dr1" d="M88 46 q-4 8 0 11 q4 -3 0 -11z" />
            <path className="drop dr2" d="M112 46 q-4 8 0 11 q4 -3 0 -11z" />
            <path className="drop dr3" d="M136 46 q-4 8 0 11 q4 -3 0 -11z" />
          </g>
          <path className="tear" d="M84 112 q-8 16 0 22 q8 -6 0 -22z" />
        </svg>
      )}
    </div>
  )
}
