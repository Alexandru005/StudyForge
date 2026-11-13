import Topbar from './components/Topbar.jsx'
import Flashcards from './pages/Flashcards.jsx'
import Home from './pages/Home.jsx'
import Library from './pages/Library.jsx'
import NotFound from './pages/NotFound.jsx'
import Plan from './pages/Plan.jsx'
import { Router } from './router/Router.jsx'
import { useRouter } from './router/context.js'
import { ToastProvider } from './toast/ToastProvider.jsx'

/*
 * Rute:
 *   /                  pagina principală
 *   /plan              generare plan din programa (PDF)       POST /api/plans
 *   /plan/:id          un plan salvat                         GET  /api/plans/{id}
 *   /flashcards        generare set de carduri                POST /api/quizzes
 *   /flashcards/:id    un set salvat                          GET  /api/quizzes/{id}
 *   /biblioteca        toate planurile și seturile            GET  /api/library
 */
function Routes() {
  const { location } = useRouter()
  const [section, id] = location.pathname.split('/').filter(Boolean)

  switch (section) {
    case undefined:
      return <Home />
    case 'plan':
      return <Plan planId={id} />
    case 'flashcards':
      return <Flashcards quizId={id} />
    case 'biblioteca':
      return <Library />
    default:
      return <NotFound />
  }
}

export default function App() {
  return (
    <Router>
      <ToastProvider>
        <Topbar />
        <Routes />
      </ToastProvider>
    </Router>
  )
}
