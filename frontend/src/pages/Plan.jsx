import { useEffect, useRef, useState } from 'react'
import { createPlan, getPlan } from '../api.js'
import { IconSave, IconUpload } from '../components/icons.jsx'
import PlanView from '../components/PlanView.jsx'
import Spinner from '../components/Spinner.jsx'
import StatusBanner from '../components/StatusBanner.jsx'
import usePageTitle from '../hooks/usePageTitle.js'
import { useRouter } from '../router/context.js'
import { useToast } from '../toast/context.js'

const MAX_BYTES = 20 * 1024 * 1024 // limita din backend (spring.servlet.multipart.max-file-size)

function checkPdf(file) {
  if (!/\.pdf$/i.test(file.name)) return 'Fișierul trebuie să fie PDF.'
  if (file.size > MAX_BYTES) return 'Fișierul e prea mare (maxim 20 MB).'
  return ''
}

/** Pagina "Plan": programa (PDF) -> plan pe săptămâni. /plan/:id deschide un plan salvat. */
export default function Plan({ planId }) {
  usePageTitle('Plan de învățare')
  const toast = useToast()
  const { navigate } = useRouter()

  const [file, setFile] = useState(null)
  const [startDate, setStartDate] = useState('')
  const [over, setOver] = useState(false)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')

  const [details, setDetails] = useState(null) // { plan, content }
  const [loadErr, setLoadErr] = useState(null) // { id, message }
  const scrollAfter = useRef(false)
  const resultRef = useRef(null)

  // Deschide un plan salvat (/plan/:id)
  const loadedId = details?.plan.id
  useEffect(() => {
    if (!planId || loadedId === planId) return
    let alive = true
    getPlan(planId)
      .then((d) => alive && setDetails(d))
      .catch((e) => alive && setLoadErr({ id: planId, message: e.message }))
    return () => {
      alive = false
    }
  }, [planId, loadedId])

  // Pe ecrane mici, după generare derulează la rezultat
  useEffect(() => {
    if (loadedId && scrollAfter.current) {
      scrollAfter.current = false
      if (window.innerWidth <= 960) resultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [loadedId])

  const shown = planId && loadedId === planId ? details : null
  const failed = planId && loadErr?.id === planId ? loadErr.message : ''
  const loading = Boolean(planId) && !shown && !failed

  function pick(f) {
    setErr('')
    if (!f) return
    const problem = checkPdf(f)
    if (problem) {
      setFile(null)
      setErr(problem)
      return
    }
    setFile(f)
  }

  async function generate() {
    setErr('')
    if (!file) {
      setErr('Alege PDF-ul cu programa materiei.')
      return
    }
    setBusy(true)
    try {
      const d = await createPlan(file, startDate)
      scrollAfter.current = true
      setDetails(d)
      navigate(`/plan/${d.plan.id}`)
      toast('Plan generat și salvat în bibliotecă')
    } catch (e) {
      setErr(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page-app">
      <div className="bg-glows" aria-hidden="true">
        <div className="glow g1" />
        <div className="glow g2" />
        <div className="glow g3" />
      </div>

      <main className="wrap">
        <section className="intro">
          <div className="eyebrow">Plan</div>
          <h1>
            Pregătește-ți <span>semestrul.</span>
          </h1>
          <p>
            Încarcă PDF-ul cu programa materiei, iar AI-ul îți construiește un plan pe săptămâni, cu teme, sarcini și
            evaluări. Planul se salvează automat în bibliotecă.
          </p>
        </section>

        <StatusBanner />

        <div className="layout">
          {/* ===== PANOU SURSĂ ===== */}
          <aside className="panel glass" aria-labelledby="plan-src-title">
            <h2 id="plan-src-title">Programa</h2>

            <div className="field">
              <span className="lbl">PDF cu programa și evaluările</span>
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
            </div>

            <div className="field">
              <label htmlFor="start-date">
                Data de început <span className="opt">(opțional)</span>
              </label>
              <input id="start-date" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
              <p className="help">Dacă nu alegi, planul începe azi. Data se rotunjește la lunea săptămânii alese.</p>
            </div>

            <button className="btn" type="button" onClick={generate} disabled={busy}>
              {busy && <Spinner />}
              <span>{busy ? 'Se generează… poate dura puțin' : 'Generează planul'}</span>
            </button>
            <p className="err" role="alert">
              {err}
            </p>
            <p className="saved-note">
              <IconSave />
              Fiecare plan se salvează automat în bibliotecă.
            </p>
          </aside>

          {/* ===== REZULTAT ===== */}
          <div ref={resultRef} className="result-col">
            {shown ? (
              <PlanView details={shown} />
            ) : failed ? (
              <div className="empty glass">
                <h3>Nu am putut deschide planul</h3>
                <p>{failed}</p>
              </div>
            ) : loading ? (
              <div className="empty glass" role="status">
                <h3>Se încarcă planul…</h3>
              </div>
            ) : busy ? (
              <div className="empty glass" role="status">
                <h3>Se generează planul…</h3>
                <p>AI-ul citește programa și o împarte pe săptămâni. Poate dura până la câteva minute.</p>
              </div>
            ) : (
              <div className="empty glass">
                <h3>Niciun plan încă</h3>
                <p>Încarcă programa materiei din panoul „Programa”, apoi apasă „Generează planul”.</p>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}
