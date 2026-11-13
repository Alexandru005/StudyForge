import { useState } from 'react'
import { countLabel, formatDate, formatRange, todayIso } from '../format.js'
import { Link } from '../router/Router.jsx'
import { IconSaved } from './icons.jsx'
import PdfLinks from './PdfLinks.jsx'

/** Planul complet: rezumat, evaluări, săptămâni (teme + sarcini) și sfaturi. details = { plan, content } din API. */
export default function PlanView({ details }) {
  const { plan, content } = details
  const [today] = useState(todayIso)
  const weeks = (content?.weeks ?? []).filter(Boolean)
  const assessments = (content?.assessments ?? []).filter(Boolean)
  const tips = (content?.tips ?? []).filter(Boolean)

  return (
    <section className="study" aria-labelledby="plan-title">
      <div className="study-head">
        <div>
          <div className="course">{plan.subject || 'Plan de învățare'}</div>
          <h2 id="plan-title">{plan.title}</h2>
          <div className="meta">
            <span>{formatRange(plan.startDate, plan.endDate)}</span>
            <span>· {countLabel(plan.weekCount, 'săptămână', 'săptămâni')}</span>
            <span className="pill-saved">
              <IconSaved />
              Salvat în bibliotecă
            </span>
          </div>
        </div>
        <PdfLinks pdfUrl={plan.pdfUrl} downloadUrl={plan.downloadUrl} />
      </div>

      {content?.summary && <p className="plan-summary">{content.summary}</p>}

      <div className="plan-actions">
        <Link className="btn" to={`/flashcards?plan=${plan.id}`}>
          Generează flashcards din plan
        </Link>
      </div>

      {assessments.length > 0 && (
        <div className="block">
          <h3>Evaluări</h3>
          <ul className="assess-list">
            {assessments.map((a, i) => (
              <li key={i} className="assess glass">
                <div className="assess-head">
                  <span className="assess-name">{a.name}</span>
                  <span className="assess-date">
                    {[a.type, a.date ? formatDate(a.date, true) : 'Dată necunoscută'].filter(Boolean).join(' · ')}
                  </span>
                </div>
                {a.coverage?.length > 0 && (
                  <div className="mini-chips">
                    {a.coverage.map((c, j) => (
                      <span key={j} className="mini-chip">
                        {c}
                      </span>
                    ))}
                  </div>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      {weeks.length > 0 && (
        <div className="block">
          <h3>Plan pe săptămâni</h3>
          <div className="weeks">
            {weeks.map((w) => {
              const current = w.startDate <= today && today <= w.endDate
              return (
                <article key={w.weekNumber} className={'week glass' + (current ? ' current' : '')}>
                  <div className="week-head">
                    <span className="topic">
                      <i />
                      Săptămâna {w.weekNumber}
                    </span>
                    <span className="week-dates">
                      {formatRange(w.startDate, w.endDate)}
                      {current && <b className="now"> · săptămâna curentă</b>}
                    </span>
                  </div>
                  {w.focus && <h4>{w.focus}</h4>}
                  <div className="week-cols">
                    {w.topics?.length > 0 && (
                      <div>
                        <div className="lbl-sm">Teme</div>
                        <ul>
                          {w.topics.map((t, i) => (
                            <li key={i}>{t}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                    {w.tasks?.length > 0 && (
                      <div>
                        <div className="lbl-sm">Sarcini</div>
                        <ul>
                          {w.tasks.map((t, i) => (
                            <li key={i}>{t}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                  {w.assessments?.length > 0 && (
                    <div className="mini-chips">
                      {w.assessments.map((a, i) => (
                        <span key={i} className="mini-chip">
                          {a}
                        </span>
                      ))}
                    </div>
                  )}
                  <div className="week-actions">
                    <Link className="btn-ghost" to={`/flashcards?plan=${plan.id}&week=${w.weekNumber}`}>
                      Flashcards pentru săptămâna {w.weekNumber}
                    </Link>
                  </div>
                </article>
              )
            })}
          </div>
        </div>
      )}

      {tips.length > 0 && (
        <div className="block">
          <h3>Sfaturi</h3>
          <ul className="tips glass">
            {tips.map((t, i) => (
              <li key={i}>{t}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}
