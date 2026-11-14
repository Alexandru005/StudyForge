import { useEffect, useMemo, useState } from 'react'
import { getLibrary } from '../api.js'
import { useAuth } from '../auth/context.js'
import MoodMascot from '../components/MoodMascot.jsx'
import ProgressChart from '../components/ProgressChart.jsx'
import usePageTitle from '../hooks/usePageTitle.js'
import { formatDuration, percent } from '../progress/format.js'
import {
  MOOD_HAPPY_FROM,
  MOOD_SAD_BELOW,
  accuracy,
  addDays,
  allTotals,
  dailySeries,
  moodFor,
  planWeekScores,
  rangeTotals,
  scoreDelta,
  subjectList,
  weekStart,
  weeklySeries,
} from '../progress/stats.js'
import { GUEST_KEY, loadProgress } from '../progress/store.js'
import { Link } from '../router/Router.jsx'

const MOOD_TEXT = {
  happy: 'Super! Te descurci foarte bine, continuă așa.',
  neutral: 'E ok, dar mai ai loc de mai bine. Reia cardurile greșite.',
  sad: 'Hai că poți mai bine! Reia cardurile greșite și încearcă din nou.',
}
const NO_DATA_TEXT = 'Încă nu am destule date. Rezolvă câteva flashcard-uri și mă vezi reacționând.'

const signed = (n) => (n === null ? '–' : n > 0 ? `+${n}` : String(n))

/** Pagina "Progres": alegi materia și vezi timpul, scorul la flashcards și evoluția pe zile / săptămâni. */
export default function Progress() {
  usePageTitle('Progres')
  const { user } = useAuth()
  const userKey = user?.id ?? GUEST_KEY

  const [today] = useState(() => new Date())
  const progress = useMemo(() => loadProgress(userKey), [userKey])
  const [subjectKey, setSubjectKey] = useState(null) // null = toate materiile
  const [range, setRange] = useState('days') // 'days' | 'weeks'

  // planurile salvate apar ca materii și când nu ai încă timp petrecut la ele
  const [plans, setPlans] = useState([])
  useEffect(() => {
    let alive = true
    getLibrary()
      .then((d) => alive && setPlans(d.plans ?? []))
      .catch(() => {}) // fără backend, rămân doar materiile din progres
    return () => {
      alive = false
    }
  }, [])

  const subjects = useMemo(() => {
    const list = subjectList(progress)
    for (const p of plans) {
      const key = `plan:${p.id}`
      if (!list.some((s) => s.key === key)) list.push({ key, title: p.title, sec: 0, ok: 0, bad: 0 })
    }
    return list
  }, [progress, plans])

  const totalAll = allTotals(progress)
  const chosen = subjects.find((s) => s.key === subjectKey)
  const sel = allTotals(progress, subjectKey)
  const score = accuracy(sel)
  const mood = moodFor(score)
  const share = totalAll.sec > 0 ? Math.round((sel.sec / totalAll.sec) * 100) : null
  const hasAnyData = totalAll.sec > 0 || totalAll.ok + totalAll.bad > 0

  const thisWeekStart = weekStart(today)
  const thisWeek = rangeTotals(progress, thisWeekStart, today, subjectKey)
  const lastWeek = rangeTotals(progress, addDays(thisWeekStart, -7), addDays(thisWeekStart, -1), subjectKey)
  const todayTotals = rangeTotals(progress, today, today, subjectKey)
  const weekDelta = scoreDelta(accuracy(thisWeek), accuracy(lastWeek))

  const series = range === 'days' ? dailySeries(progress, subjectKey, 14, today) : weeklySeries(progress, subjectKey, 8, today)
  const planWeeks = planWeekScores(progress, subjectKey)

  const gender = user?.gender ?? 'OTHER'
  const scopeLabel = chosen ? chosen.title : 'toate materiile'

  return (
    <div className="page-app">
      <div className="bg-glows" aria-hidden="true">
        <div className="glow g1" />
        <div className="glow g2" />
        <div className="glow g3" />
      </div>

      <main className="wrap pg">
        <section className="intro">
          <div className="eyebrow">Progres</div>
          <h1>
            Cum îți <span>merge.</span>
          </h1>
          <p>
            Alege materia și vezi cât ai studiat, ce scor ai la flashcard-uri și cum ai evoluat pe zile și pe
            săptămâni.
          </p>
        </section>

        {/* ===== ALEGE MATERIA ===== */}
        <section className="pg-block" aria-labelledby="pg-pick">
          <h2 id="pg-pick">Alege materia</h2>
          <div className="pg-subjects" role="radiogroup" aria-labelledby="pg-pick">
            <SubjectCard
              checked={subjectKey === null}
              onPick={() => setSubjectKey(null)}
              title="Toate materiile"
              sec={totalAll.sec}
              score={accuracy(totalAll)}
            />
            {subjects.map((s) => (
              <SubjectCard
                key={s.key}
                checked={subjectKey === s.key}
                onPick={() => setSubjectKey(s.key)}
                title={s.title}
                sec={s.sec}
                score={accuracy(s)}
              />
            ))}
          </div>
          {!subjects.length && (
            <p className="pg-hint">
              Materiile apar aici după ce studiezi un plan sau un set de carduri.{' '}
              <Link to="/plan">Generează un plan</Link> sau <Link to="/flashcards">începe flashcard-urile</Link>.
            </p>
          )}
        </section>

        {/* ===== MASCOTĂ + STATISTICI ===== */}
        <section className="pg-hero glass" aria-labelledby="pg-scope" aria-live="polite">
          <div className="pg-mascot">
            <MoodMascot gender={gender} mood={mood} />
            <p className="pg-mood-text">{score === null ? NO_DATA_TEXT : MOOD_TEXT[mood]}</p>
            <p className="pg-mood-rule">
              Fericit de la {MOOD_HAPPY_FROM}% corecte, trist sub {MOOD_SAD_BELOW}%.
            </p>
          </div>
          <div className="pg-stats">
            <h2 id="pg-scope">
              <span className="pg-scope-lbl">Statistici pentru</span> {scopeLabel}
            </h2>
            <div className="pg-tiles">
              <div className="tile">
                <span className="lbl-sm">{chosen ? 'Timp la materia aleasă' : 'Timp la toate materiile'}</span>
                <b data-testid="t-time">{formatDuration(sel.sec)}</b>
                <span className="sub">timp activ</span>
              </div>
              <div className="tile">
                <span className="lbl-sm">Din tot timpul tău de studiu</span>
                <b data-testid="t-share">{percent(share)}</b>
                <span className="meter" aria-hidden="true">
                  <i style={{ width: (share ?? 0) + '%' }} />
                </span>
              </div>
              <div className="tile">
                <span className="lbl-sm">Timp activ în total</span>
                <b data-testid="t-total">{formatDuration(totalAll.sec)}</b>
                <span className="sub">la toate materiile</span>
              </div>
              <div className="tile">
                <span className="lbl-sm">Scor la flashcards</span>
                <b data-testid="t-score">{percent(score)}</b>
                <span className="sub">
                  <span className="ok">{sel.ok} corecte</span> · <span className="bad">{sel.bad} greșite</span>
                </span>
              </div>
            </div>
          </div>
        </section>

        {!hasAnyData ? (
          <section className="empty glass" aria-labelledby="pg-empty">
            <h3 id="pg-empty">Încă nu ai progres de arătat</h3>
            <p>Studiază un plan sau rezolvă un set de flashcard-uri și statisticile apar aici, separat pentru fiecare materie.</p>
            <div className="done-actions">
              <Link className="btn" to="/flashcards">
                Începe flashcard-urile
              </Link>
              <Link className="btn-ghost" to="/plan">
                Generează un plan
              </Link>
            </div>
          </section>
        ) : (
          <>
            {/* ===== AZI / SĂPTĂMÂNA ACEASTA ===== */}
            <section className="pg-block" aria-labelledby="pg-now">
              <h2 id="pg-now">Azi și săptămâna aceasta</h2>
              <div className="pg-periods">
                <div className="glass period">
                  <span className="lbl-sm">Azi</span>
                  <b data-testid="p-today-time">{formatDuration(todayTotals.sec)}</b>
                  <span className="sub" data-testid="p-today-score">
                    {accuracy(todayTotals) === null ? 'fără răspunsuri' : `${accuracy(todayTotals)}% corecte (${todayTotals.ok}/${todayTotals.ok + todayTotals.bad})`}
                  </span>
                </div>
                <div className="glass period">
                  <span className="lbl-sm">Săptămâna aceasta</span>
                  <b data-testid="p-week-time">{formatDuration(thisWeek.sec)}</b>
                  <span className="sub" data-testid="p-week-score">
                    {accuracy(thisWeek) === null ? 'fără răspunsuri' : `${accuracy(thisWeek)}% corecte (${thisWeek.ok}/${thisWeek.ok + thisWeek.bad})`}
                  </span>
                </div>
                <div className="glass period">
                  <span className="lbl-sm">Față de săptămâna trecută</span>
                  <b
                    data-testid="p-delta"
                    className={weekDelta === null ? '' : weekDelta >= 0 ? 'up' : 'down'}
                  >
                    {weekDelta === null ? '–' : `${signed(weekDelta)} puncte`}
                  </b>
                  <span className="sub">
                    {lastWeek.sec > 0 || thisWeek.sec > 0
                      ? `${signed(Math.round((thisWeek.sec - lastWeek.sec) / 60))} min timp activ`
                      : 'încă fără date de comparat'}
                  </span>
                </div>
              </div>
            </section>

            {/* ===== EVOLUȚIA ===== */}
            <section className="pg-block glass pg-chart-card" aria-labelledby="pg-evo">
              <div className="pg-chart-head">
                <h2 id="pg-evo">Evoluția</h2>
                <div className="tabs pg-tabs" role="group" aria-label="Perioadă">
                  <button type="button" className="tab" aria-pressed={range === 'days'} onClick={() => setRange('days')}>
                    Pe zile
                  </button>
                  <button type="button" className="tab" aria-pressed={range === 'weeks'} onClick={() => setRange('weeks')}>
                    Pe săptămâni
                  </button>
                </div>
              </div>
              <ProgressChart series={series} unit={range === 'days' ? 'ultimele 14 zile' : 'ultimele 8 săptămâni'} />
            </section>

            {/* ===== PE SĂPTĂMÂNI DE PLAN ===== */}
            <section className="pg-block glass pg-weeks" aria-labelledby="pg-weeks">
              <h2 id="pg-weeks">Pe săptămânile din plan</h2>
              {planWeeks.length ? (
                <ul>
                  {planWeeks.map((w) => (
                    <li key={w.week}>
                      <span className="wk">Săptămâna {w.week}</span>
                      <span className="meter" aria-hidden="true">
                        <i style={{ width: (w.score ?? 0) + '%' }} />
                      </span>
                      <span className="val">
                        {percent(w.score)} <small>({w.ok} din {w.ok + w.bad})</small>
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="pg-hint">Scorul pe săptămâni apare după ce răspunzi la carduri generate pentru o săptămână din plan.</p>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  )
}

function SubjectCard({ checked, onPick, title, sec, score }) {
  return (
    <label className={'subj' + (checked ? ' on' : '')}>
      <input type="radio" name="subject" checked={checked} onChange={onPick} />
      <span className="subj-title">{title}</span>
      <span className="subj-meta">
        {formatDuration(sec)} · {score === null ? 'fără scor' : `${score}% corecte`}
      </span>
    </label>
  )
}
