import { useState } from 'react'
import Spinner from '../components/Spinner.jsx'
import Tilt from '../components/Tilt.jsx'
import { safeNext, useAuth } from '../auth/context.js'
import usePageTitle from '../hooks/usePageTitle.js'
import { Link } from '../router/Router.jsx'
import { useRouter } from '../router/context.js'
import { useToast } from '../toast/context.js'
import { AuthGlows } from './Welcome.jsx'

/** /login: POST /api/auth/login, apoi înapoi la pagina cerută (implicit /plan, unde încarci programa). */
export default function Login() {
  usePageTitle('Log in')
  const { login } = useAuth()
  const { location, navigate } = useRouter()
  const toast = useToast()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')

  async function submit(e) {
    e.preventDefault()
    setErr('')
    if (!email.trim() || !password) {
      setErr('Completează emailul și parola.')
      return
    }
    setBusy(true)
    try {
      const user = await login({ email: email.trim(), password })
      toast(`Bine ai revenit, ${user.username}!`)
      navigate(safeNext(location.search), { replace: true })
    } catch (ex) {
      setErr(ex.message)
      setBusy(false)
    }
  }

  return (
    <div className="page-auth">
      <AuthGlows />
      <main className="wrap auth-main auth-center">
        <Tilt max={4} as="form" className="auth-card tilt-card login-card" onSubmit={submit} noValidate>
          <div className="eyebrow">Log in</div>
          <h1 className="auth-title">Bine ai revenit</h1>
          <p className="auth-sub">Intră cu emailul și parola ta.</p>

          <div className="field">
            <label htmlFor="login-email">Email</label>
            <input
              id="login-email"
              type="email"
              autoComplete="email"
              placeholder="nume@exemplu.ro"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="login-password">Parolă</label>
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <p className="err" role="alert">
            {err}
          </p>
          <button className="btn btn-block" type="submit" disabled={busy}>
            {busy && <Spinner />}
            <span>{busy ? 'Se verifică…' : 'Log in'}</span>
          </button>

          <p className="auth-switch">
            Nu ai cont? <Link to={'/register' + location.search}>Register</Link> ·{' '}
            <Link className="muted" to={'/cont' + location.search}>
              ‹ Înapoi
            </Link>
          </p>
        </Tilt>
      </main>
    </div>
  )
}
