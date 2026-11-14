import { useEffect, useRef, useState } from 'react'
import mascota from '../assets/mascota.svg'
import { avatarFor } from '../auth/avatars.js'
import { useAuth } from '../auth/context.js'
import { Link } from '../router/Router.jsx'
import { useRouter } from '../router/context.js'
import { useToast } from '../toast/context.js'
import { IconChevron, IconLogout } from './icons.jsx'

// Înainte de conectare: secțiunile paginii principale. După conectare: paginile aplicației.
const GUEST_LINKS = [
  { to: '/#functii', label: 'Funcționalități' },
  { to: '/#cum', label: 'Cum funcționează' },
]
const USER_LINKS = [
  { to: '/plan', label: 'Plan' },
  { to: '/flashcards', label: 'Flashcards' },
  { to: '/biblioteca', label: 'Bibliotecă' },
]
const AUTH_PAGES = ['/cont', '/login', '/register']

export default function Topbar() {
  const { location } = useRouter()
  const { user } = useAuth()
  const onAuthPage = AUTH_PAGES.includes(location.pathname)
  const links = onAuthPage ? [] : user ? USER_LINKS : GUEST_LINKS
  const isActive = (to) =>
    !to.startsWith('/#') && (location.pathname === to || location.pathname.startsWith(to + '/'))

  return (
    <header className="topbar">
      <nav className="wrap nav" aria-label="Navigare principală">
        <Link className="logo" to="/" aria-label="Study Forge – pagina principală">
          <img className="logo-img" src={mascota} alt="" width="52" height="52" />
          <span className="brand">Study Forge</span>
        </Link>
        {(links.length > 0 || (user && !onAuthPage)) && (
          <div className="nav-right">
            <div className="links">
              {links.map((l) => (
                <Link key={l.to} className="nl" to={l.to} aria-current={isActive(l.to) ? 'page' : undefined}>
                  {l.label}
                </Link>
              ))}
            </div>
            {user && !onAuthPage && <UserMenu user={user} />}
          </div>
        )}
      </nav>
    </header>
  )
}

/** Cine e conectat: poza de profil (după gen) + nume; meniul are Log out. */
function UserMenu({ user }) {
  const { logout } = useAuth()
  const { navigate } = useRouter()
  const toast = useToast()
  const [open, setOpen] = useState(false)
  const boxRef = useRef(null)
  const avatar = avatarFor(user.gender)

  useEffect(() => {
    if (!open) return
    const onDown = (e) => {
      if (!boxRef.current?.contains(e.target)) setOpen(false)
    }
    const onKey = (e) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('pointerdown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  function doLogout() {
    setOpen(false)
    logout()
    toast('Te-ai deconectat.')
    navigate('/')
  }

  return (
    <div className="user-menu" ref={boxRef}>
      <button
        type="button"
        className="user-chip"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        <img src={avatar.src} alt={`Poza de profil ${avatar.name}`} width="36" height="36" />
        <span className="user-name">{user.username}</span>
        <IconChevron />
      </button>
      {open && (
        <div className="user-pop" role="menu">
          <div className="user-pop-head">
            Conectat ca
            <b>{user.email}</b>
          </div>
          <button type="button" role="menuitem" className="logout" onClick={doLogout}>
            <IconLogout />
            Log out
          </button>
        </div>
      )}
    </div>
  )
}
