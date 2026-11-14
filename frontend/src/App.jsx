import { useEffect } from 'react'
import { AuthProvider } from './auth/AuthProvider.jsx'
import { useAuth } from './auth/context.js'
import Topbar from './components/Topbar.jsx'
import Flashcards from './pages/Flashcards.jsx'
import Home from './pages/Home.jsx'
import Library from './pages/Library.jsx'
import Login from './pages/Login.jsx'
import NotFound from './pages/NotFound.jsx'
import Plan from './pages/Plan.jsx'
import Register from './pages/Register.jsx'
import Welcome from './pages/Welcome.jsx'
import { Router } from './router/Router.jsx'
import { useRouter } from './router/context.js'
import { ToastProvider } from './toast/ToastProvider.jsx'

/*
 * Rute:
 *   /                  pagina principală ("Începe gratuit" -> /cont)
 *   /cont              alegi Log in sau Register
 *   /login             autentificare                            POST /api/auth/login
 *   /register          cont nou (cu genul ales)                 POST /api/auth/register
 *   --- necesită cont (altfel -> /login?next=...) ---
 *   /plan              generare plan din programa (PDF)       POST /api/plans
 *   /plan/:id          un plan salvat                         GET  /api/plans/{id}
 *   /flashcards        generare set de carduri                POST /api/quizzes
 *   /flashcards/:id    un set salvat                          GET  /api/quizzes/{id}
 *   /biblioteca        toate planurile și seturile            GET  /api/library
 */

/** Trimite la altă adresă (fără să rămână în istoric). */
function Redirect({ to }) {
  const { navigate } = useRouter()
  useEffect(() => {
    navigate(to, { replace: true })
  }, [navigate, to])
  return null
}

function Routes() {
  const { location } = useRouter()
  const { user } = useAuth()
  const [section, id] = location.pathname.split('/').filter(Boolean)
  const here = location.pathname + location.search

  // Paginile aplicației cer cont
  const needsUser = (page) => (user ? page : <Redirect to={`/login?next=${encodeURIComponent(here)}`} />)
  // Paginile de autentificare nu mai au sens după conectare
  const guestOnly = (page) => (user ? <Redirect to="/plan" /> : page)

  switch (section) {
    case undefined:
      return <Home />
    case 'cont':
      return guestOnly(<Welcome />)
    case 'login':
      return guestOnly(<Login />)
    case 'register':
      return guestOnly(<Register />)
    case 'plan':
      return needsUser(<Plan planId={id} />)
    case 'flashcards':
      return needsUser(<Flashcards quizId={id} />)
    case 'biblioteca':
      return needsUser(<Library />)
    default:
      return <NotFound />
  }
}

export default function App() {
  return (
    <Router>
      <ToastProvider>
        <AuthProvider>
          <Topbar />
          <Routes />
        </AuthProvider>
      </ToastProvider>
    </Router>
  )
}
