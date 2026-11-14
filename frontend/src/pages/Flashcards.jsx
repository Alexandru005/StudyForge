import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { flushSync } from 'react-dom'
import { createQuiz, fetchPdfAsFile, getPlan, getQuiz, listPlans, getHint } from '../api.js'
import {
  IconCheck,
  IconCross,
  IconFlip,
  IconFlipBack,
  IconNext,
  IconPrev,
  IconSave,
  IconSaved,
  IconUpload,
  IconWarn,
} from '../components/icons.jsx'
import { useAuth } from '../auth/context.js'
import PdfLinks from '../components/PdfLinks.jsx'
import Spinner from '../components/Spinner.jsx'
import StatusBanner from '../components/StatusBanner.jsx'
import { countLabel, fileSlug } from '../format.js'
import usePageTitle from '../hooks/usePageTitle.js'
import { useTilt } from '../hooks/useTilt.js'
import { GUEST_KEY, addAttempt, subjectOf } from '../progress/store.js'
import { useStudyTimer } from '../progress/useStudyTimer.js'
import { Link } from '../router/Router.jsx'
import { useRouter } from '../router/context.js'
import { useToast } from '../toast/context.js'

const MAX_BYTES = 20 * 1024 * 1024 // limita din backend
const MIN_COUNT = 5 // QuizService.MIN_COUNT
const MAX_COUNT = 40 // QuizService.MAX_COUNT
const MAX_WEEK = 52 // QuizService.MAX_WEEK_WITHOUT_PLAN
const NO_CARDS = []

/* Indiciul unui card: dacă backend-ul trimite `hint`, îl folosim; altfel îl derivăm din răspuns
   (câte cuvinte are și cu ce începe), fără să-l dăm pe tot. */
function hintFor(card) {
  const given = typeof card.hint === 'string' ? card.hint.trim() : ''
  if (given) return given
  const words = String(card.answer ?? '').trim().split(/\s+/).filter(Boolean)
  if (!words.length) return 'Gândește-te la ideea principală din întrebare.'
  const first = words[0].replace(/[.,;:!?]+$/, '')
  if (words.length === 1) return `Răspunsul e un singur cuvânt și începe cu „${first.slice(0, 2)}…”.`
  return `Răspunsul are ${words.length} cuvinte și începe cu „${first}…”.`
}

/* Backend-ul nu are (încă) endpoint pentru "știut / de repetat", deci marcajele se păstrează în browser. */
const marksKey = (quizId) => `studyforge:marks:${quizId}`

function readMarks(quizId, cards) {
  let saved = {}
  try {
    saved = JSON.parse(localStorage.getItem(marksKey(quizId)) || '{}')
  } catch {
    // date corupte -> pornim de la zero
  }
  const marks = {}
  for (const c of cards) {
    if (saved[c.id] === 'ok' || saved[c.id] === 'bad') marks[c.id] = saved[c.id]
    else if (c.mastered) marks[c.id] = 'ok'
  }
  return marks
}

function writeMarks(quizId, marks) {
  try {
    localStorage.setItem(marksKey(quizId), JSON.stringify(marks))
  } catch {
    // spațiu de stocare indisponibil -> marcajele rămân doar în sesiune
  }
}

function checkPdf(file) {
  if (!/\.pdf$/i.test(file.name)) return 'Fișierul trebuie să fie PDF.'
  if (file.size > MAX_BYTES) return 'Fișierul e prea mare (maxim 20 MB).'
  return ''
}

/** Pagina "Flashcards": plan salvat sau PDF -> set de carduri; /flashcards/:quizId deschide un set salvat. */
export default function Flashcards({ quizId }) {
  usePageTitle('Flashcards')
  const toast = useToast()
  const { location, navigate } = useRouter()
  const { user, checking: authLoading } = useAuth()
  const userKey = user?.id ?? GUEST_KEY

  // parametrii din Plan (?plan=...&week=...), citiți o singură dată
  const [initial] = useState(() => {
    const q = new URLSearchParams(location.search)
    const w = parseInt(q.get('week') ?? '', 10)
    return { plan: q.get('plan'), week: w >= 1 ? w : 1 }
  })

  /* ---------- sursă ---------- */
  const [tab, setTab] = useState('plan')
  const [plans, setPlans] = useState(null) // null = se încarcă
  const [plansErr, setPlansErr] = useState('')
  const [planId, setPlanId] = useState('')
  const [planDetails, setPlanDetails] = useState({}) // planId -> { plan, content }
  const [week, setWeek] = useState(initial.week)
  const [file, setFile] = useState(null)
  const [fileWeek, setFileWeek] = useState('1')
  const [count, setCount] = useState('15')
  const [over, setOver] = useState(false)
  const [busy, setBusy] = useState(false)
  const [genErr, setGenErr] = useState('')

  /* ---------- set + studiu ---------- */
  const [set, setSet] = useState(null) // { quiz, cards }
  const [setErr, setSetErr] = useState(null) // { id, message }
  const [marks, setMarks] = useState({}) // cardId -> 'ok' | 'bad'
  const [onlyIds, setOnlyIds] = useState(null) // Set de id-uri la reluarea celor greșite
  const [index, setIndex] = useState(0)
  const [flipped, setFlipped] = useState(false)
  const [hintOpenFor, setHintOpenFor] = useState(null) // id-ul cardului cu norișorul deschis
  const [hints, setHints] = useState({}) // cardId -> { text, loading, local }
  const [instant, setInstant] = useState(false) // dezactivează animația de întoarcere
  const [slide, setSlide] = useState('') // clase pentru animația de alunecare
  const [finished, setFinished] = useState(false)

  const cardRef = useRef(null)
  const slideRef = useRef(null)
  const sceneRef = useRef(null)
  const tiltRef = useRef(null)
  const doneRef = useRef(null)
  const titleRef = useRef(null)
  const animating = useRef(false)
  const timers = useRef([])
  const latest = useRef({})
  const keyHandler = useRef(null)
  const scrollAfter = useRef(false)

  /* ---------- date derivate ---------- */
  const cards = set?.cards ?? NO_CARDS
  const view = useMemo(() => (onlyIds ? cards.filter((c) => onlyIds.has(c.id)) : cards), [cards, onlyIds])
  const card = view[index]
  const status = card ? marks[card.id] : undefined
  const okCount = view.filter((c) => marks[c.id] === 'ok').length
  const badCount = view.filter((c) => marks[c.id] === 'bad').length
  const hasDeck = Boolean(card) && !finished
  const subject = set ? subjectOf(set.quiz) : null
  // timpul activ petrecut la materia acestui set, pentru pagina Progres
  useStudyTimer({ userKey, subjectKey: subject?.key, subjectTitle: subject?.title, enabled: hasDeck && !authLoading })
  // norișorul se vede doar pe fața cardului pentru care a fost deschis (se închide singur la întoarcere / schimbare de card)
  const hintOpen = Boolean(card) && hintOpenFor === card.id && !flipped
  const hintState = card ? hints[card.id] : undefined
  const hintText = hintState?.loading && !hintState.text ? 'Mă gândesc la un indiciu…' : (hintState?.text ?? (card ? hintFor(card) : ''))
  // indiciile lungi primesc o literă mai mică, ca să încapă mereu în nor
  const hintSize = hintText.length > 140 ? 'xs' : hintText.length > 90 ? 'sm' : 'md'

  const selectedPlan = plans?.find((p) => p.id === planId)
  const weekOptions = useMemo(() => {
    const weeks = planDetails[planId]?.content?.weeks?.filter(Boolean)
    if (weeks?.length) {
      return weeks.map((w) => ({
        n: w.weekNumber,
        label: `Săptămâna ${w.weekNumber}${w.focus ? ' · ' + w.focus : ''}`,
      }))
    }
    return Array.from({ length: selectedPlan?.weekCount ?? 0 }, (_, i) => ({ n: i + 1, label: `Săptămâna ${i + 1}` }))
  }, [planDetails, planId, selectedPlan])
  const effectiveWeek = weekOptions.some((o) => o.n === week) ? week : (weekOptions[0]?.n ?? 1)

  useTilt(sceneRef, { max: 6, targetRef: tiltRef, enabled: hasDeck })

  // încarcă un set (primit de la API) în zona de studiu
  const applySet = useCallback((d) => {
    setSet({ quiz: d.quiz, cards: d.flashcards })
    setMarks(readMarks(d.quiz.id, d.flashcards))
    setOnlyIds(null)
    setIndex(0)
    setFlipped(false)
    setSlide('')
    setFinished(false)
  }, [])

  /* ---------- încărcări ---------- */
  // planurile salvate (pentru lista din panou)
  useEffect(() => {
    let alive = true
    listPlans()
      .then((list) => {
        if (!alive) return
        setPlans(list)
        setPlanId((prev) => prev || (list.find((p) => p.id === initial.plan) ?? list[0])?.id || '')
      })
      .catch((e) => {
        if (!alive) return
        setPlans([])
        setPlansErr(e.message)
      })
    return () => {
      alive = false
    }
  }, [initial])

  // conținutul planului ales (ca să arătăm titlurile săptămânilor)
  useEffect(() => {
    if (!planId || planDetails[planId]) return
    let alive = true
    getPlan(planId)
      .then((d) => alive && setPlanDetails((prev) => ({ ...prev, [planId]: d })))
      .catch(() => {
        // fără detalii, lista de săptămâni rămâne numerică
      })
    return () => {
      alive = false
    }
  }, [planId, planDetails])

  // un set salvat (/flashcards/:quizId)
  const loadedId = set?.quiz.id
  useEffect(() => {
    if (!quizId || loadedId === quizId) return
    let alive = true
    getQuiz(quizId)
      .then((d) => alive && applySet(d))
      .catch((e) => alive && setSetErr({ id: quizId, message: e.message }))
    return () => {
      alive = false
    }
  }, [quizId, loadedId, applySet])

  // pe ecrane mici, după generare derulează la set
  useEffect(() => {
    if (loadedId && scrollAfter.current) {
      scrollAfter.current = false
      if (window.innerWidth <= 960) titleRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [loadedId])

  // ecranul final primește focus
  useEffect(() => {
    if (finished) doneRef.current?.focus({ preventScroll: true })
  }, [finished])

  // curățenie la ieșirea din pagină
  useEffect(() => {
    const list = timers.current
    return () => list.forEach(clearTimeout)
  }, [])

  const later = (fn, ms) => {
    timers.current.push(setTimeout(fn, ms))
  }

  /* ---------- generare ---------- */
  function pick(f) {
    setGenErr('')
    if (!f) return
    const problem = checkPdf(f)
    if (problem) {
      setFile(null)
      setGenErr(problem)
      return
    }
    setFile(f)
  }

  async function generate() {
    setGenErr('')
    const n = Number(count)
    if (!Number.isInteger(n) || n < MIN_COUNT || n > MAX_COUNT) {
      setGenErr(`Numărul de carduri trebuie să fie între ${MIN_COUNT} și ${MAX_COUNT}.`)
      return
    }
    if (tab === 'plan' && !selectedPlan) {
      setGenErr('Alege un plan salvat sau folosește tabul „PDF”.')
      return
    }
    const w = tab === 'plan' ? effectiveWeek : Number(fileWeek)
    if (tab === 'file') {
      if (!file) {
        setGenErr('Încarcă PDF-ul planului.')
        return
      }
      if (!Number.isInteger(w) || w < 1 || w > MAX_WEEK) {
        setGenErr(`Săptămâna trebuie să fie între 1 și ${MAX_WEEK}.`)
        return
      }
    }

    setBusy(true)
    try {
      // din planul salvat: descărcăm PDF-ul lui și îl retrimitem, backend-ul îl recunoaște după metadate/hash
      const source =
        tab === 'plan'
          ? await fetchPdfAsFile(selectedPlan.pdfUrl, `${fileSlug(selectedPlan.title)}.pdf`)
          : file
      const d = await createQuiz({ file: source, week: w, count: n })
      scrollAfter.current = true
      applySet(d)
      navigate(`/flashcards/${d.quiz.id}`, { replace: true })
      toast('Set generat și salvat în bibliotecă')
    } catch (e) {
      setGenErr(e.message)
    } finally {
      setBusy(false)
    }
  }

  /* ---------- navigare între carduri ---------- */
  // Schimbă cardul curent fără animație (și, opțional, alte stări în același pas).
  function swapTo(i, slideClass, extra) {
    flushSync(() => {
      extra?.()
      setIndex(i)
      setFlipped(false)
      setFinished(false)
      setInstant(true)
      if (slideClass !== undefined) setSlide(slideClass)
    })
    if (cardRef.current) void cardRef.current.offsetWidth
    if (slideRef.current) void slideRef.current.offsetWidth
    setInstant(false)
    setSlide('')
  }

  function go(target, dir) {
    const n = view.length
    if (!n) return
    const i = Math.max(0, Math.min(target, n - 1))
    if (!dir) {
      swapTo(i)
      return
    }
    if (animating.current || i === index) return
    animating.current = true
    setSlide(dir === 'next' ? 'out-left' : 'out-right')
    later(() => {
      swapTo(i, `no-anim ${dir === 'next' ? 'out-right' : 'out-left'}`)
      later(() => {
        animating.current = false
      }, 280)
    }, 280)
  }

  function mark(result) {
    if (!card || animating.current) return
    const next = { ...marks, [card.id]: result }
    setMarks(next)
    writeMarks(set.quiz.id, next)
    addAttempt(userKey, subject, set.quiz.weekNumber, result)
    // trecem la următorul card fără răspuns
    later(() => {
      const { view: v, index: i, go: goTo } = latest.current
      const after = [...v.slice(i + 1), ...v.slice(0, i + 1)]
      const target = after.find((c) => !next[c.id])
      if (!target) setFinished(true)
      else goTo(v.indexOf(target), 'next')
    }, 550)
  }

  function retryWrong() {
    const wrong = view.filter((c) => marks[c.id] === 'bad')
    const next = { ...marks }
    wrong.forEach((c) => delete next[c.id])
    setMarks(next)
    writeMarks(set.quiz.id, next)
    swapTo(0, undefined, () => setOnlyIds(new Set(wrong.map((c) => c.id))))
  }

  function retryAll() {
    const next = { ...marks }
    view.forEach((c) => delete next[c.id])
    setMarks(next)
    writeMarks(set.quiz.id, next)
    swapTo(0, undefined, () => setOnlyIds(null))
  }

  function toggleFlip() {
    setFlipped((f) => !f)
    setHintOpenFor(null) // norișorul nu reapare la întoarcerea cardului
  }

  // Indiciul vine de la backend (AI). Dacă cererea eșuează, folosim indiciul local derivat din card.
  async function loadHint(target, again = false) {
    const previous = again ? hints[target.id]?.text : undefined
    setHints((h) => ({ ...h, [target.id]: { text: h[target.id]?.text ?? '', loading: true } }))
    try {
      const { hint } = await getHint(target.id, previous)
      setHints((h) => ({ ...h, [target.id]: { text: hint, loading: false } }))
    } catch {
      setHints((h) => ({ ...h, [target.id]: { text: hintFor(target), loading: false, local: true } }))
    }
  }

  function toggleHint() {
    if (!card) return
    const opening = hintOpenFor !== card.id
    setHintOpenFor(opening ? card.id : null)
    if (opening && !hints[card.id]) loadHint(card)
  }

  function showAllCards() {
    swapTo(0, undefined, () => setOnlyIds(null))
  }

  /* ---------- tastatură ---------- */
  useEffect(() => {
    latest.current = { view, index, go }
    keyHandler.current = (e) => {
      const el = e.target instanceof Element ? e.target : null
      if (el?.closest('input, textarea, select') || e.metaKey || e.ctrlKey || e.altKey) return
      if (!hasDeck) return
      if (e.key === ' ' || e.key === 'Enter') {
        if (el?.closest('button, a') && el !== cardRef.current) return
        e.preventDefault()
        toggleFlip()
      } else if (e.key === 'ArrowRight') go(index + 1, 'next')
      else if (e.key === 'ArrowLeft') go(index - 1, 'prev')
      else if ((e.key === 'h' || e.key === 'H') && !flipped) toggleHint()
      else if (e.key === 'Escape' && hintOpen) setHintOpenFor(null)
      else if (e.key === '1' && flipped) mark('ok')
      else if (e.key === '2' && flipped) mark('bad')
    }
  })
  useEffect(() => {
    const onKey = (e) => keyHandler.current?.(e)
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [])

  /* ---------- randare ---------- */
  const tabs = ['plan', 'file']
  const quiz = set?.quiz
  const setFailed = quizId && setErr?.id === quizId ? setErr.message : ''
  const setLoading = Boolean(quizId) && loadedId !== quizId && !setFailed
  const topicLabel = quiz?.weekNumber ? `Săptămâna ${quiz.weekNumber}` : 'Flashcard'
  const cardClass = [
    'fc',
    flipped && 'flipped',
    instant && 'no-anim',
    status === 'ok' && 'mark-ok',
    status === 'bad' && 'mark-bad',
  ]
    .filter(Boolean)
    .join(' ')

  const pct = (x) => (view.length ? (x / view.length) * 100 + '%' : '0')

  return (
    <div className="page-app">
      <div className="bg-glows" aria-hidden="true">
        <div className="glow g1" />
        <div className="glow g2" />
        <div className="glow g3" />
      </div>

      <main className="wrap">
        <section className="intro">
          <div className="eyebrow">Flashcards</div>
          <h1>
            Exersează <span>activ.</span>
          </h1>
          <p>
            Alege planul și săptămâna, iar AI-ul îți pregătește cardurile. Întoarce cardul, verifică-te și marchează ce
            ai știut.
          </p>
        </section>

        <StatusBanner />

        <div className="layout">
          {/* ===== PANOU SURSĂ ===== */}
          <aside className="panel glass" aria-labelledby="src-title">
            <h2 id="src-title">Sursa</h2>

            <div className="tabs" role="tablist" aria-label="De unde vin cardurile">
              {[
                ['plan', 'Din plan salvat'],
                ['file', 'PDF'],
              ].map(([id, label], i) => (
                <button
                  key={id}
                  className="tab"
                  role="tab"
                  id={`tab-${id}`}
                  type="button"
                  aria-controls={`pane-${id}`}
                  aria-selected={tab === id}
                  tabIndex={tab === id ? 0 : -1}
                  onClick={() => {
                    setTab(id)
                    setGenErr('')
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
                      const other = tabs[1 - i]
                      setTab(other)
                      setGenErr('')
                      document.getElementById(`tab-${other}`)?.focus()
                    }
                  }}
                >
                  {label}
                </button>
              ))}
            </div>

            <div id="pane-plan" role="tabpanel" aria-labelledby="tab-plan" hidden={tab !== 'plan'}>
              {plans === null ? (
                <p className="help">Se încarcă planurile…</p>
              ) : plansErr ? (
                <p className="err">{plansErr}</p>
              ) : plans.length === 0 ? (
                <p className="help">
                  Nu ai niciun plan salvat. <Link to="/plan">Generează unul</Link> sau folosește tabul „PDF”.
                </p>
              ) : (
                <>
                  <div className="field">
                    <label htmlFor="plan">Plan</label>
                    <select id="plan" value={planId} onChange={(e) => setPlanId(e.target.value)}>
                      {plans.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.subject || p.title}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="field">
                    <label htmlFor="week">Săptămâna</label>
                    <select id="week" value={effectiveWeek} onChange={(e) => setWeek(Number(e.target.value))}>
                      {weekOptions.map((o) => (
                        <option key={o.n} value={o.n}>
                          {o.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </>
              )}
            </div>

            <div id="pane-file" role="tabpanel" aria-labelledby="tab-file" hidden={tab !== 'file'}>
              <div className="field">
                <span className="lbl">PDF-ul planului</span>
                <label
                  className={'drop' + (over ? ' over' : '')}
                  onDragEnter={(e) => {
                    e.preventDefault()
                    setOver(true)
                  }}
                  onDragOver={(e) => {
                    e.preventDefault()
                    setOver(true)
                  }}
                  onDragLeave={(e) => {
                    e.preventDefault()
                    setOver(false)
                  }}
                  onDrop={(e) => {
                    e.preventDefault()
                    setOver(false)
                    pick(e.dataTransfer.files[0])
                  }}
                >
                  <IconUpload />
                  <span>
                    <b>Alege un PDF</b> sau trage-l aici
                  </span>
                  <span>{file ? file.name : 'Maxim 20 MB'}</span>
                  <input type="file" accept="application/pdf,.pdf" onChange={(e) => pick(e.target.files[0])} />
                </label>
                <p className="help">Încarcă PDF-ul planului generat pe pagina Plan (sau un PDF cu materia).</p>
              </div>
              <div className="field">
                <label htmlFor="file-week">Săptămâna în care ești</label>
                <input
                  id="file-week"
                  type="number"
                  min={1}
                  max={MAX_WEEK}
                  inputMode="numeric"
                  value={fileWeek}
                  onChange={(e) => setFileWeek(e.target.value)}
                />
              </div>
            </div>

            <div className="field">
              <label htmlFor="count">Număr de carduri</label>
              <input
                id="count"
                type="number"
                min={MIN_COUNT}
                max={MAX_COUNT}
                inputMode="numeric"
                value={count}
                onChange={(e) => setCount(e.target.value)}
              />
              <p className="help">
                Între {MIN_COUNT} și {MAX_COUNT}.
              </p>
            </div>

            <button className="btn" type="button" onClick={generate} disabled={busy}>
              {busy && <Spinner />}
              <span>{busy ? 'Se generează… poate dura puțin' : 'Generează flashcards'}</span>
            </button>
            <p className="err" role="alert">
              {genErr}
            </p>
            <p className="saved-note">
              <IconSave />
              Fiecare set se salvează automat în bibliotecă.
            </p>
          </aside>

          {/* ===== ZONA DE STUDIU ===== */}
          <section className="study" aria-labelledby="set-title">
            {setFailed ? (
              <div className="empty glass">
                <h3>Nu am putut deschide setul</h3>
                <p>{setFailed}</p>
                <div className="done-actions">
                  <Link className="btn-ghost" to="/biblioteca">
                    Înapoi la bibliotecă
                  </Link>
                </div>
              </div>
            ) : setLoading ? (
              <div className="empty glass" role="status">
                <h3>Se încarcă setul…</h3>
              </div>
            ) : !quiz ? (
              <div className="empty glass">
                <h3>Niciun card încă</h3>
                <p>Alege un plan și o săptămână din panoul „Sursa”, apoi apasă „Generează flashcards”.</p>
              </div>
            ) : (
              <>
                <div className="study-head">
                  <div>
                    <div className="course">{quiz.planTitle || quiz.sourceFileName || ''}</div>
                    <h2 id="set-title" ref={titleRef}>
                      {quiz.learningStage}
                    </h2>
                    <div className="meta">
                      <span>{countLabel(cards.length, 'card', 'carduri')}</span>
                      <span className="pill-saved">
                        <IconSaved />
                        Salvat în bibliotecă
                      </span>
                    </div>
                  </div>
                  <PdfLinks pdfUrl={quiz.pdfUrl} downloadUrl={quiz.downloadUrl} />
                </div>

                <div className="warn" role="note">
                  <IconWarn />
                  <span>Cardurile sunt generate de AI. Verifică informațiile cu materialul de la curs.</span>
                </div>

                {onlyIds && (
                  <div className="chips" aria-label="Filtru">
                    <button
                      type="button"
                      className="chip-btn"
                      aria-pressed="true"
                      title="Arată toate cardurile"
                      onClick={showAllCards}
                    >
                      Doar cele greșite ✕
                    </button>
                  </div>
                )}

                <div className="stats" aria-live="polite">
                  <div className="stats-row">
                    <span className="s-ok">
                      Corecte: <b>{okCount}</b>
                    </span>
                    <span className="s-bad">
                      Greșite: <b>{badCount}</b>
                    </span>
                    <span>
                      Rămase: <b>{view.length - okCount - badCount}</b>
                    </span>
                  </div>
                  <div className="bar" aria-hidden="true">
                    <i className="b-ok" style={{ width: pct(okCount) }} />
                    <i className="b-bad" style={{ width: pct(badCount) }} />
                  </div>
                </div>

                {finished ? (
                  <div className="done glass">
                    <h3 ref={doneRef} tabIndex={-1}>
                      Ai terminat setul!
                    </h3>
                    <p>
                      {badCount === 0
                        ? 'Le-ai știut pe toate. Poți genera un set nou sau trece la altă săptămână.'
                        : `Mai ai ${badCount === 1 ? 'un card' : badCount + ' carduri'} de repetat. Reia-le acum, cât le ai proaspete în minte.`}
                    </p>
                    <div className="score">
                      <div className="sc-ok">
                        <b>{okCount}</b>
                        <span>corecte</span>
                      </div>
                      <div className="sc-bad">
                        <b>{badCount}</b>
                        <span>greșite</span>
                      </div>
                    </div>
                    <div className="done-actions">
                      {badCount > 0 && (
                        <button className="btn" type="button" onClick={retryWrong}>
                          Reia cardurile greșite
                        </button>
                      )}
                      <button className="btn-ghost" type="button" onClick={retryAll}>
                        Reia tot setul
                      </button>
                    </div>
                  </div>
                ) : card ? (
                  <div>
                    <div className="fc-scene" ref={sceneRef}>
                      <div className="fc-tilt" ref={tiltRef}>
                        <div className={'fc-slide' + (slide ? ' ' + slide : '')} ref={slideRef}>
                          <div
                            className={cardClass}
                            ref={cardRef}
                            tabIndex={0}
                            role="group"
                            aria-roledescription="flashcard"
                            aria-label={
                              flipped
                                ? 'Spatele cardului: răspuns. Apasă pentru întrebare.'
                                : 'Fața cardului: întrebare. Apasă pentru răspuns.'
                            }
                            onClick={(e) => {
                              if (e.target instanceof Element && e.target.closest('button')) return
                              if (window.getSelection()?.toString()) return // nu întoarce când se selectează text
                              toggleFlip()
                            }}
                          >
                            {/* FAȚA: întrebarea */}
                            <div className="face front" inert={flipped}>
                              <div className="fc-top">
                                <span className="topic">
                                  <i />
                                  <span>{topicLabel}</span>
                                </span>
                              </div>
                              <p className="q">{card.question}</p>
                              <div className="fc-bottom">
                                <span className="fc-hint">
                                  <IconFlip />
                                  Apasă pe card pentru răspuns
                                </span>
                                <div className="hint-wrap">
                                  {hintOpen && (
                                    <div
                                      className="hint-cloud"
                                      id="hint-cloud"
                                      data-size={hintSize}
                                      role="status"
                                      title="Apasă ca să închizi"
                                      onClick={(e) => {
                                        e.stopPropagation()
                                        setHintOpenFor(null)
                                      }}
                                    >
                                      <svg className="hint-top" viewBox="0 28 488 187" aria-hidden="true">
                                        <path d="M0 215 C0 190 30 150 72 145 Q82 143 82 138 C82 80 115 34 170 32 C215 30 258 50 270 82 Q280 80 305 78 C345 74 385 105 388 150 Q392 172 405 174 C450 176 488 190 488 215Z" />
                                      </svg>
                                      <div className="hint-body">
                                        <div className="hint-inner">
                                          <span className="hint-lbl">Indiciu</span>
                                          <p>{hintText}</p>
                                          {hintState && !hintState.local && (
                                            <button
                                              className="hint-more"
                                              type="button"
                                              disabled={hintState.loading}
                                              onClick={(e) => {
                                                e.stopPropagation()
                                                loadHint(card, true)
                                              }}
                                            >
                                              {hintState.loading ? 'Se încarcă…' : 'Alt indiciu'}
                                            </button>
                                          )}
                                        </div>
                                      </div>
                                      <i className="hc d1" aria-hidden="true" />
                                      <i className="hc d2" aria-hidden="true" />
                                    </div>
                                  )}
                                  <button
                                    className="hint-btn"
                                    type="button"
                                    aria-expanded={hintOpen}
                                    aria-controls={hintOpen ? 'hint-cloud' : undefined}
                                    aria-label="Arată un indiciu"
                                    title="Indiciu (H)"
                                    onClick={(e) => {
                                      e.stopPropagation()
                                      toggleHint()
                                    }}
                                  >
                                    <img src="/hint-mascot.svg" alt="" width="44" height="44" />
                                  </button>
                                </div>
                              </div>
                            </div>
                            {/* SPATELE: răspuns + bifă / X */}
                            <div className="face back" inert={!flipped}>
                              <div className="fc-top">
                                <span className="topic">
                                  <i />
                                  <span>{topicLabel}</span>
                                </span>
                                {status && (
                                  <span className={'tag ' + status}>{status === 'ok' ? 'Știut' : 'De repetat'}</span>
                                )}
                              </div>
                              <div className="back-body">
                                <div className="lbl-sm">Răspuns</div>
                                <p className="answer">{card.answer}</p>
                              </div>
                              <div className="fc-bottom">
                                <span className="fc-hint">
                                  <IconFlipBack />
                                  Înapoi la întrebare
                                </span>
                                <div className="judge">
                                  <button
                                    className="j ok"
                                    type="button"
                                    aria-pressed={status === 'ok'}
                                    aria-label="Am răspuns corect"
                                    title="Am răspuns corect (1)"
                                    onClick={(e) => {
                                      e.stopPropagation()
                                      mark('ok')
                                    }}
                                  >
                                    <IconCheck />
                                  </button>
                                  <button
                                    className="j bad"
                                    type="button"
                                    aria-pressed={status === 'bad'}
                                    aria-label="Am greșit"
                                    title="Am greșit (2)"
                                    onClick={(e) => {
                                      e.stopPropagation()
                                      mark('bad')
                                    }}
                                  >
                                    <IconCross />
                                  </button>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="controls">
                      <button
                        className="btn-ghost"
                        type="button"
                        aria-label="Cardul anterior"
                        disabled={index === 0}
                        onClick={() => go(index - 1, 'prev')}
                      >
                        <IconPrev />
                        Înapoi
                      </button>
                      <div className="counter" aria-live="polite">
                        {index + 1} <span>/ {view.length}</span>
                      </div>
                      <button
                        className="btn-ghost"
                        type="button"
                        aria-label="Cardul următor"
                        disabled={index === view.length - 1}
                        onClick={() => go(index + 1, 'next')}
                      >
                        Înainte
                        <IconNext />
                      </button>
                    </div>
                    <div className="dots" aria-label="Toate cardurile">
                      {view.map((c, i) => {
                        const st = marks[c.id]
                        return (
                          <button
                            key={c.id}
                            type="button"
                            className={'dot' + (st ? ' ' + st : '') + (i === index ? ' cur' : '')}
                            aria-label={`Cardul ${i + 1}` + (st === 'ok' ? ', corect' : st === 'bad' ? ', greșit' : '')}
                            aria-current={i === index ? 'true' : undefined}
                            onClick={() => go(i, i > index ? 'next' : 'prev')}
                          >
                            {i + 1}
                          </button>
                        )
                      })}
                    </div>
                    <p className="keys">
                      <kbd>Spațiu</kbd> întoarce · <kbd>←</kbd> <kbd>→</kbd> navighează · <kbd>1</kbd> corect ·{' '}
                      <kbd>2</kbd> greșit
                    </p>
                  </div>
                ) : null}
              </>
            )}
          </section>
        </div>
      </main>
    </div>
  )
}
