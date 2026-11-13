import usePageTitle from '../hooks/usePageTitle.js'
import { Link } from '../router/Router.jsx'

export default function NotFound() {
  usePageTitle('Pagină inexistentă')
  return (
    <div className="page-app">
      <main className="wrap">
        <section className="intro">
          <div className="eyebrow">404</div>
          <h1>
            Pagina nu <span>există.</span>
          </h1>
          <p>Adresa pe care ai deschis-o nu duce nicăieri. Te poți întoarce la pagina principală.</p>
          <div className="plan-actions">
            <Link className="btn" to="/">
              Pagina principală
            </Link>
          </div>
        </section>
      </main>
    </div>
  )
}
