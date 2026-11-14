import { useState } from 'react'
import { AVATARS, GENDERS } from '../auth/avatars.js'
import { safeNext, useAuth } from '../auth/context.js'
import Spinner from '../components/Spinner.jsx'
import Tilt from '../components/Tilt.jsx'
import usePageTitle from '../hooks/usePageTitle.js'
import { Link } from '../router/Router.jsx'
import { useRouter } from '../router/context.js'
import { useToast } from '../toast/context.js'
import { AuthGlows } from './Welcome.jsx'

const MIN_PASSWORD = 8 // la fel ca în backend (AuthService.MIN_PASSWORD_LENGTH)

/** Verificări rapide înainte de cerere; backend-ul le face oricum pe toate. */
function check({ username, email, password, password2, gender }) {
  if (!gender) return 'Alege una dintre căsuțe (Feminin, Masculin sau Altul).'
  if (username.trim().split(/\s+/).filter(Boolean).length < 2)
    return 'Scrie numele și prenumele (de exemplu „Popescu Ion”).'
  if (!email.trim()) return 'Completează adresa de email.'
  if (password.length < MIN_PASSWORD) return `Parola trebuie să aibă minimum ${MIN_PASSWORD} caractere.`
  if (password !== password2) return 'Parolele nu se potrivesc.'
  return ''
}

/** /register: POST /api/auth/register (cu genul ales), apoi direct la /plan. */
export default function Register() {
  usePageTitle('Register')
  const { register } = useAuth()
  const { location, navigate } = useRouter()
  const toast = useToast()

  const [gender, setGender] = useState('')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [password2, setPassword2] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')

  async function submit(e) {
    e.preventDefault()
    const problem = check({ username, email, password, password2, gender })
    setErr(problem)
    if (problem) return
    setBusy(true)
    try {
      const user = await register({ username: username.trim(), email: email.trim(), gender, password })
      toast(`Cont creat. Bun venit, ${user.username}!`)
      navigate(safeNext(location.search), { replace: true })
    } catch (ex) {
      setErr(ex.message)
      setBusy(false)
    }
  }

  return (
    <div className="page-auth">
      <AuthGlows />
      <main className="wrap auth-main">
        <form className="register-grid" onSubmit={submit} noValidate>
          <div>
            <div className="eyebrow">Register</div>
            <h1 className="auth-title-lg">
              Cine <span>ești tu?</span>
            </h1>
            <p className="auth-lead">
              Alege căsuța care ți se potrivește. Poza de profil depinde doar de gen, iar mulți utilizatori pot avea
              aceeași poză. Fiecare are contul lui.
            </p>
            <div className="genders" role="radiogroup" aria-label="Genul">
              {GENDERS.map((g) => {
                const a = AVATARS[g]
                return (
                  <Tilt
                    key={g}
                    as="button"
                    type="button"
                    max={12}
                    role="radio"
                    aria-checked={gender === g}
                    className={'gender tilt-card' + (gender === g ? ' on' : '')}
                    onClick={() => setGender(g)}
                  >
                    <img src={a.src} alt="" width="120" height="120" />
                    <b>{a.name}</b>
                    <span>{a.label}</span>
                  </Tilt>
                )
              })}
            </div>
          </div>

          <Tilt max={4} className="auth-card tilt-card">
            <h2>Creează-ți contul</h2>
            <div className="field">
              <label htmlFor="reg-name">Nume și prenume</label>
              <input
                id="reg-name"
                type="text"
                autoComplete="name"
                placeholder="Popescu Ion"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="reg-email">Email</label>
              <input
                id="reg-email"
                type="email"
                autoComplete="email"
                placeholder="nume@exemplu.ro"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="reg-pw">Parolă</label>
              <input
                id="reg-pw"
                type="password"
                autoComplete="new-password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <p className="help">Minimum {MIN_PASSWORD} caractere.</p>
            </div>
            <div className="field">
              <label htmlFor="reg-pw2">Confirmă parola</label>
              <input
                id="reg-pw2"
                type="password"
                autoComplete="new-password"
                placeholder="••••••••"
                value={password2}
                onChange={(e) => setPassword2(e.target.value)}
              />
            </div>

            <p className="err" role="alert">
              {err}
            </p>
            <button className="btn btn-block" type="submit" disabled={busy}>
              {busy && <Spinner />}
              <span>{busy ? 'Se creează contul…' : 'Creează cont'}</span>
            </button>
            <p className="auth-switch">
              Ai deja cont? <Link to={'/login' + location.search}>Log in</Link> ·{' '}
              <Link className="muted" to={'/cont' + location.search}>
                ‹ Înapoi
              </Link>
            </p>
          </Tilt>
        </form>
      </main>
    </div>
  )
}
