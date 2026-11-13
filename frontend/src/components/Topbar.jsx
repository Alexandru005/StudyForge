import { Link } from '../router/Router.jsx'
import { useRouter } from '../router/context.js'
import { IconLogo } from './icons.jsx'

const LINKS = [
  { to: '/#functii', label: 'Funcționalități' },
  { to: '/#cum', label: 'Cum funcționează' },
  { to: '/plan', label: 'Plan' },
  { to: '/flashcards', label: 'Flashcards' },
  { to: '/biblioteca', label: 'Bibliotecă' },
]

export default function Topbar() {
  const { location } = useRouter()
  const isActive = (to) =>
    !to.startsWith('/#') && (location.pathname === to || location.pathname.startsWith(to + '/'))

  return (
    <header className="topbar">
      <nav className="wrap nav" aria-label="Navigare principală">
        <Link className="logo" to="/" aria-label="Study Forge – pagina principală">
          <div className="logo-mark">
            <IconLogo />
          </div>
          <span className="brand">Study Forge</span>
        </Link>
        <div className="links">
          {LINKS.map((l) => (
            <Link key={l.to} className="nl" to={l.to} aria-current={isActive(l.to) ? 'page' : undefined}>
              {l.label}
            </Link>
          ))}
        </div>
      </nav>
    </header>
  )
}
