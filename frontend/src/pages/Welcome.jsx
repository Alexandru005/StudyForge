import Tilt from '../components/Tilt.jsx'
import { IconArrow, IconCheck, IconLogin, IconUserPlus } from '../components/icons.jsx'
import usePageTitle from '../hooks/usePageTitle.js'
import { Link } from '../router/Router.jsx'
import { useRouter } from '../router/context.js'

const PERKS = ['Plan de studiu pe săptămâni, din PDF-ul tău', 'Flashcards generate cu AI', 'Bibliotecă cu tot ce ai salvat']

/** /cont: alegi între Log in și Register. */
export default function Welcome() {
  usePageTitle('Intră în cont')
  const { location } = useRouter()
  const keep = location.search // păstrează ?next= spre pagina de după login

  return (
    <div className="page-auth">
      <AuthGlows />
      <main className="wrap auth-main">
        <div className="welcome-grid">
          <div>
            <div className="eyebrow">Bun venit</div>
            <h1 className="auth-title-xl">
              Învață mai organizat, <span>în fiecare zi.</span>
            </h1>
            <p className="auth-lead">Intră în cont sau creează unul nou și ai totul într-un singur loc.</p>
            <ul className="perks">
              {PERKS.map((p) => (
                <li key={p}>
                  <IconCheck size={18} />
                  {p}
                </li>
              ))}
            </ul>
          </div>

          <Tilt max={6} className="auth-card tilt-card">
            <h2>Intră în Study Forge</h2>
            <p className="auth-sub">Alege cum vrei să continui.</p>
            <div className="choices">
              <Link className="choice primary" to={'/login' + keep}>
                <span className="choice-icon">
                  <IconLogin />
                </span>
                <span className="choice-text">
                  <b>Log in</b>
                  <span>Ai deja cont? Continuă de unde ai rămas.</span>
                </span>
                <IconArrow />
              </Link>
              <Link className="choice" to={'/register' + keep}>
                <span className="choice-icon">
                  <IconUserPlus />
                </span>
                <span className="choice-text">
                  <b>Register</b>
                  <span>Ești nou aici? Creează un cont.</span>
                </span>
                <IconArrow />
              </Link>
            </div>
          </Tilt>
        </div>
      </main>
    </div>
  )
}

/** Luminile animate din fundal (aceleași ca pe celelalte pagini). */
export function AuthGlows() {
  return (
    <div className="bg-glows" aria-hidden="true">
      <div className="glow g1" />
      <div className="glow g2" />
      <div className="glow g3" />
    </div>
  )
}
