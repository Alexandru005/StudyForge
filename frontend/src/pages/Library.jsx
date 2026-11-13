import { useEffect, useRef, useState } from 'react'
import { getLibrary, listQuizzes } from '../api.js'
import PdfLinks from '../components/PdfLinks.jsx'
import StatusBanner from '../components/StatusBanner.jsx'
import { countLabel, formatCreated, formatRange } from '../format.js'
import usePageTitle from '../hooks/usePageTitle.js'
import { Link } from '../router/Router.jsx'

/** Pagina "Bibliotecă": toate planurile și toate seturile de flashcard-uri salvate (GET /api/library). */
export default function Library() {
  usePageTitle('Bibliotecă')

  const [data, setData] = useState(null) // { plans, quizzes }
  const [err, setErr] = useState('')
  const [reload, setReload] = useState(0)

  const [filter, setFilter] = useState('') // planId ('' = toate)
  const [filtered, setFiltered] = useState(null) // quizzes pentru filtrul ales
  const [filterErr, setFilterErr] = useState('')
  const request = useRef(0)

  useEffect(() => {
    let alive = true
    getLibrary()
      .then((d) => alive && setData(d))
      .catch((e) => alive && setErr(e.message))
    return () => {
      alive = false
    }
  }, [reload])

  function retry() {
    setErr('')
    setData(null)
    setReload((n) => n + 1)
  }

  function onFilter(planId) {
    setFilter(planId)
    setFilterErr('')
    setFiltered(null)
    if (!planId) return
    const id = ++request.current
    listQuizzes(planId)
      .then((list) => id === request.current && setFiltered(list))
      .catch((e) => id === request.current && setFilterErr(e.message))
  }

  const plans = data?.plans ?? []
  const quizzes = filter ? (filtered ?? []) : (data?.quizzes ?? [])
  const quizzesLoading = Boolean(filter) && filtered === null && !filterErr

  return (
    <div className="page-app">
      <div className="bg-glows" aria-hidden="true">
        <div className="glow g1" />
        <div className="glow g2" />
        <div className="glow g3" />
      </div>

      <main className="wrap">
        <section className="intro">
          <div className="eyebrow">Bibliotecă</div>
          <h1>
            Tot ce ai <span>generat.</span>
          </h1>
          <p>
            Planurile de învățare și seturile de flashcard-uri salvate. Deschide-le, descarcă-le în PDF sau continuă să
            exersezi.
          </p>
        </section>

        <StatusBanner />

        {err ? (
          <div className="empty glass">
            <h3>Nu am putut încărca biblioteca</h3>
            <p>{err}</p>
            <div className="done-actions">
              <button className="btn" type="button" onClick={retry}>
                Încearcă din nou
              </button>
            </div>
          </div>
        ) : !data ? (
          <div className="empty glass" role="status">
            <h3>Se încarcă biblioteca…</h3>
          </div>
        ) : (
          <>
            {/* ===== PLANURI ===== */}
            <section className="lib-section" aria-labelledby="lib-plans">
              <div className="lib-head">
                <h2 id="lib-plans">
                  Planuri de învățare <span className="count">{plans.length}</span>
                </h2>
                <Link className="btn btn-sm" to="/plan">
                  Plan nou
                </Link>
              </div>
              {plans.length === 0 ? (
                <div className="empty glass">
                  <h3>Niciun plan salvat</h3>
                  <p>Încarcă programa unei materii și AI-ul îți face un plan pe săptămâni.</p>
                </div>
              ) : (
                <div className="lib-grid">
                  {plans.map((p) => (
                    <article key={p.id} className="item glass">
                      <div className="item-top">
                        <span className="topic">
                          <i />
                          {p.subject || 'Plan'}
                        </span>
                        <span className="item-date">{formatCreated(p.createdAt)}</span>
                      </div>
                      <h3>{p.title}</h3>
                      <p className="item-meta">
                        {[formatRange(p.startDate, p.endDate), countLabel(p.weekCount, 'săptămână', 'săptămâni')]
                          .filter(Boolean)
                          .join(' · ')}
                      </p>
                      {p.syllabusFileName && <p className="item-sub">Din: {p.syllabusFileName}</p>}
                      <div className="item-actions">
                        <Link className="btn btn-sm" to={`/plan/${p.id}`}>
                          Deschide
                        </Link>
                        <Link className="btn-ghost" to={`/flashcards?plan=${p.id}`}>
                          Flashcards
                        </Link>
                        <PdfLinks pdfUrl={p.pdfUrl} downloadUrl={p.downloadUrl} />
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>

            {/* ===== SETURI DE FLASHCARD-URI ===== */}
            <section className="lib-section" aria-labelledby="lib-quizzes">
              <div className="lib-head">
                <h2 id="lib-quizzes">
                  Seturi de flashcard-uri <span className="count">{quizzes.length}</span>
                </h2>
                <div className="lib-tools">
                  {plans.length > 0 && (
                    <div className="filter">
                      <label className="sr-only" htmlFor="quiz-filter">
                        Filtrează după plan
                      </label>
                      <select id="quiz-filter" value={filter} onChange={(e) => onFilter(e.target.value)}>
                        <option value="">Toate planurile</option>
                        {plans.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.subject || p.title}
                          </option>
                        ))}
                      </select>
                    </div>
                  )}
                  <Link className="btn btn-sm" to="/flashcards">
                    Set nou
                  </Link>
                </div>
              </div>
              {filterErr ? (
                <p className="err">{filterErr}</p>
              ) : quizzesLoading ? (
                <p className="help" role="status">
                  Se încarcă…
                </p>
              ) : quizzes.length === 0 ? (
                <div className="empty glass">
                  <h3>{filter ? 'Niciun set pentru acest plan' : 'Niciun set salvat'}</h3>
                  <p>Alege un plan și o săptămână, iar AI-ul îți pregătește cardurile.</p>
                </div>
              ) : (
                <div className="lib-grid">
                  {quizzes.map((q) => (
                    <article key={q.id} className="item glass">
                      <div className="item-top">
                        <span className="topic">
                          <i />
                          {q.weekNumber ? `Săptămâna ${q.weekNumber}` : 'Flashcards'}
                        </span>
                        <span className="item-date">{formatCreated(q.createdAt)}</span>
                      </div>
                      <h3>{q.learningStage}</h3>
                      <p className="item-meta">
                        {[countLabel(q.flashcardCount, 'card', 'carduri'), q.planTitle || q.sourceFileName]
                          .filter(Boolean)
                          .join(' · ')}
                      </p>
                      <div className="item-actions">
                        <Link className="btn btn-sm" to={`/flashcards/${q.id}`}>
                          Studiază
                        </Link>
                        <PdfLinks pdfUrl={q.pdfUrl} downloadUrl={q.downloadUrl} />
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  )
}
