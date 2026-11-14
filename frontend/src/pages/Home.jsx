import { useRef } from 'react'
import { useTilt } from '../hooks/useTilt.js'
import usePageTitle from '../hooks/usePageTitle.js'
import { useAuth } from '../auth/context.js'
import { Link } from '../router/Router.jsx'

const ICON = {
  width: 24,
  height: 24,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: '#E07A94', // iconițe funcționalități
  strokeWidth: 2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
}

const FEATURES = [
  {
    title: 'Flashcard-uri',
    text: 'Carduri de întrebări generate cu AI din săptămâna de plan la care ești, ca să repeți exact ce urmează.',
    points: ['Marchezi ce știi și ce mai ai de repetat', 'Indiciu de la mascotă când te blochezi', 'Descarci setul ca PDF'],
    icon: (
      <svg {...ICON}>
        <rect x="3" y="5" width="14" height="14" rx="2" />
        <path d="M7 3h14v14" />
      </svg>
    ),
  },
  {
    title: 'Planificator',
    text: 'Încarci programa materiei (PDF) și primești un plan pe săptămâni, cu teme, sarcini și datele evaluărilor.',
    points: ['Pornește din ziua pe care o alegi', 'Testele și examenele marcate în calendar', 'Planul se salvează în bibliotecă'],
    icon: (
      <svg {...ICON}>
        <rect x="3" y="4" width="18" height="17" rx="2" />
        <path d="M8 2v4M16 2v4M3 10h18" />
      </svg>
    ),
  },
  {
    title: 'Progres',
    text: 'Vezi cât ai studiat la fiecare materie, ce scor ai la flashcard-uri și cum ai evoluat de la o săptămână la alta.',
    points: ['Grafice pe zile și pe săptămâni', 'Mascota se bucură când îți merge bine', 'Comparație cu săptămâna trecută'],
    icon: (
      <svg {...ICON}>
        <path d="M3 20h18M6 16v-4M11 16V8M16 16v-6M21 16V5" />
      </svg>
    ),
  },
]

const STEPS = [
  {
    n: '01',
    title: 'Adaugă materiile',
    text: 'Încarcă programa fiecărei materii în format PDF. În câteva secunde primești un plan de studiu împărțit pe săptămâni, până la examen.',
    meta: '~1 minut',
  },
  {
    n: '02',
    title: 'Exersează activ',
    text: 'Alege săptămâna la care ești și generează flashcard-uri. Răspunde, întoarce cardul și marchează ce știi, ca să repeți doar ce mai trebuie.',
    meta: '15 minute pe zi',
  },
  {
    n: '03',
    title: 'Urmărește progresul',
    text: 'Pe pagina Progres vezi timpul petrecut și scorul la fiecare materie. Mascota îți arată dintr-o privire cum stai.',
    meta: 'în fiecare săptămână',
  },
]

/** Container care se înclină 3D după mouse. */
function Tilt({ max, className, children }) {
  const ref = useRef(null)
  useTilt(ref, { max })
  return (
    <div ref={ref} className={className}>
      {children}
    </div>
  )
}

export default function Home() {
  usePageTitle('')
  const { user } = useAuth()
  // „Începe gratuit”: fără cont -> Log in / Register, apoi la /plan (încărcarea programei)
  const start = user ? '/plan' : '/cont?next=%2Fplan'

  return (
    <div className="page-home">
      {/* HERO */}
      <div id="top" className="wrap">
        <section className="hero">
          <div className="hero-text">
            <div className="badge">
              <i />
              Platforma ta de studiu
            </div>
            <h1>
              Forjează-ți <span>fiecare examen.</span>
            </h1>
            <p className="lead">
              Organizează materia, exersează activ și urmărește progresul într-un singur loc. Studiul devine o
              rutină, nu o corvoadă.
            </p>
            <div className="hero-cta">
              <Link className="btn" to={start}>
                Începe gratuit
              </Link>
            </div>
          </div>
        </section>
      </div>

      {/* FEATURES */}
      <section id="functii" className="features">
        <div className="glow g1" aria-hidden="true" />
        <div className="glow g2" aria-hidden="true" />
        <div className="glow g3" aria-hidden="true" />
        <div className="wrap">
          <div className="eyebrow">Funcționalități</div>
          <h2>Tot ce ai nevoie ca să înveți mai bine.</h2>
          <div className="grid">
            {FEATURES.map((f) => (
              <Tilt key={f.title} max={10} className="card feat">
                <div className="icon">{f.icon}</div>
                <h3>{f.title}</h3>
                <p>{f.text}</p>
                <ul className="feat-list">
                  {f.points.map((pt) => (
                    <li key={pt}>{pt}</li>
                  ))}
                </ul>
              </Tilt>
            ))}
          </div>
        </div>
      </section>

      {/* HOW */}
      <section id="cum" className="how wrap">
        <h2>Trei pași, de la haos la rutină.</h2>
        <div className="steps">
          {STEPS.map((s) => (
            <div className="step" key={s.n}>
              <div className="num">{s.n}</div>
              <h3>{s.title}</h3>
              <p>{s.text}</p>
              <span className="meta">{s.meta}</span>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <div id="start" className="wrap cta-wrap">
        <div className="glow-layer" aria-hidden="true">
          <div className="glow g4" />
          <div className="glow g5" />
        </div>
        <Tilt max={6} className="cta">
          <h2>Gata să începi?</h2>
          <p>Încarcă programa materiei și primești planul de studiu, apoi exersezi cu flashcard-uri.</p>
          <Link className="btn" to={start}>
            Generează planul
          </Link>
        </Tilt>
      </div>

      {/* FOOTER */}
      <footer>
        <div className="wrap foot">
          <span>© Study Forge</span>
          <span className="l">
            <a href="#start">Termeni</a>
            <a href="#start">Confidențialitate</a>
            <a href="#start">Contact</a>
          </span>
        </div>
      </footer>
    </div>
  )
}
