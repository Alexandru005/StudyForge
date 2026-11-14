import { useAuth } from '../auth/context.js'
import { useRouter } from '../router/context.js'
import FlappyForge from './FlappyForge.jsx'
import JokeBubble from './JokeBubble.jsx'

// pe pagina principală și pe paginile de cont nu apar butoanele plutitoare
const HIDE = ['/', '/cont', '/login', '/register']

/** Butoanele plutitoare din colțuri: glumă din materie (stânga) și Flappy Forge (dreapta). */
export default function Extras() {
  const { user } = useAuth()
  const { location } = useRouter()
  if (!user || HIDE.includes(location.pathname)) return null
  return (
    <>
      <JokeBubble />
      <FlappyForge />
    </>
  )
}
