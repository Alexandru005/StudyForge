import { useEffect, useRef, useState } from 'react'
import * as api from '../api.js'
import { avatarFor } from '../auth/avatars.js'
import { useAuth } from '../auth/context.js'
import Spinner from '../components/Spinner.jsx'
import { IconWarn } from '../components/icons.jsx'
import usePageTitle from '../hooks/usePageTitle.js'
import { formatDuration } from '../progress/format.js'
import { syncProgress } from '../progress/sync.js'
import { useToast } from '../toast/context.js'

/*
 * Pagina „Prieteni”: adaugi prieteni după email, accepți cereri și vedeți cine a avut cel mai bun
 * procentaj la flashcards într-o săptămână (luni–duminică, toate materiile). Like / dislike la procentajul fiecăruia.
 */

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/

const weekTitle = (offset) =>
  offset === 0 ? 'Săptămâna aceasta' : offset === -1 ? 'Săptămâna trecută' : `Acum ${-offset} săptămâni`

/** "2026-11-09" -> "9.11" (fără conversii de fus orar). */
const shortDay = (iso) => {
  const [, m, d] = iso.split('-')
  return `${Number(d)}.${m}`
}

/** Intervalul unei săptămâni calculat local, cât timp clasamentul ei încă se încarcă. */
function localWeek(offset) {
  const d = new Date()
  d.setHours(12, 0, 0, 0)
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7) + offset * 7)
  const e = new Date(d)
  e.setDate(d.getDate() + 6)
  const f = (x) => `${x.getDate()}.${String(x.getMonth() + 1).padStart(2, '0')}`
  return `${f(d)} – ${f(e)}`
}

/** Rândul după un like / dislike, înainte de răspunsul serverului. */
function withReaction(entry, mine) {
  const count = (kind, n) => n - (entry.myReaction === kind ? 1 : 0) + (mine === kind ? 1 : 0)
  return {
    ...entry,
    myReaction: mine,
    reactions: { likes: count('LIKE', entry.reactions.likes), dislikes: count('DISLIKE', entry.reactions.dislikes) },
  }
}

export default function Friends() {
  usePageTitle('Prieteni')
  const { user } = useAuth()
  const toast = useToast()

  /* ---------- prieteni și cereri ---------- */
  const [requests, setRequests] = useState(null) // { incoming, outgoing }
  const [friends, setFriends] = useState(null)
  const [peopleErr, setPeopleErr] = useState('')
  const [peopleVersion, setPeopleVersion] = useState(0) // +1 = reîncarcă listele
  const [busy, setBusy] = useState(() => new Set()) // butoane cu o cerere în curs

  /* ---------- formularul „Adaugă” ---------- */
  const [email, setEmail] = useState('')
  const [addMsg, setAddMsg] = useState(null) // { text, ok }
  const [adding, setAdding] = useState(false)

  /* ---------- clasament ---------- */
  const [week, setWeek] = useState(0)
  const [board, setBoard] = useState(null)
  const [boardErr, setBoardErr] = useState('')
  const [boardLoading, setBoardLoading] = useState(true)
  const [boardVersion, setBoardVersion] = useState(0) // +1 = reîncarcă clasamentul
  const [pop, setPop] = useState(null) // butonul de reacție animat
  const synced = useRef(null)
  const userId = user?.id

  useEffect(() => {
    let alive = true
    Promise.all([api.getFriendRequests(), api.getFriends()])
      .then(([r, f]) => {
        if (!alive) return
        setRequests(r)
        setFriends(f)
        setPeopleErr('')
      })
      .catch((e) => alive && setPeopleErr(e.message))
    return () => {
      alive = false
    }
  }, [peopleVersion])

  useEffect(() => {
    let alive = true
    // progresul tău local ajunge întâi pe server, ca procentajul din clasament să fie la zi
    synced.current ??= syncProgress(userId).catch(() => {})
    synced.current
      .then(() => api.getLeaderboard(week))
      .then((b) => {
        if (!alive) return
        setBoard(b)
        setBoardErr('')
      })
      .catch((e) => alive && setBoardErr(e.message))
      .finally(() => alive && setBoardLoading(false))
    return () => {
      alive = false
    }
  }, [week, boardVersion, userId])

  const reloadPeople = () => setPeopleVersion((v) => v + 1)
  const refreshBoard = () => {
    setBoardLoading(true)
    setBoardVersion((v) => v + 1)
  }

  // când revii în filă: cereri noi, reacții noi
  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState !== 'visible') return
      setPeopleVersion((v) => v + 1)
      setBoardVersion((v) => v + 1)
    }
    document.addEventListener('visibilitychange', onVisible)
    return () => document.removeEventListener('visibilitychange', onVisible)
  }, [])

  function changeWeek(delta) {
    setBoardLoading(true)
    setWeek((w) => Math.min(0, w + delta))
  }

  const mark = (key, on) =>
    setBusy((b) => {
      const next = new Set(b)
      if (on) next.add(key)
      else next.delete(key)
      return next
    })

  /** Acceptă / refuză / anulează / șterge, apoi reîncarcă listele (și clasamentul, dacă se schimbă prietenii). */
  async function act(key, run, message, affectsBoard) {
    if (busy.has(key)) return
    mark(key, true)
    try {
      await run()
      if (message) toast(message)
      if (affectsBoard) refreshBoard()
    } catch (e) {
      toast(e.message)
    } finally {
      reloadPeople()
      mark(key, false)
    }
  }

  async function onAdd(e) {
    e.preventDefault()
    const say = (text, ok = false) => setAddMsg({ text, ok })
    const value = email.trim().toLowerCase()
    if (!value) return say('Scrie emailul prietenului.')
    if (!EMAIL.test(value)) return say('Adresa de email nu are un format valid.')
    if (value === user?.email?.toLowerCase()) return say('Nu te poți adăuga pe tine ca prieten.')
    setAdding(true)
    try {
      const res = await api.sendFriendRequest(value)
      const name = res.request?.user?.username ?? value
      if (res.relation === 'FRIENDS') {
        say(`${name} îți trimisese deja o cerere, acum sunteți prieteni.`, true)
        refreshBoard()
      } else {
        say(`Cerere trimisă către ${name}.`, true)
      }
      setEmail('')
      reloadPeople()
    } catch (err) {
      say(err.message)
    } finally {
      setAdding(false)
    }
  }

  async function react(entry, kind) {
    const friendId = entry.user.id
    const key = 'react:' + friendId
    if (!board || entry.me || busy.has(key)) return
    const wk = board.week.offset
    const removing = entry.myReaction === kind
    const put = (next) =>
      setBoard((b) =>
        b && b.week.offset === wk
          ? { ...b, entries: b.entries.map((x) => (x.user.id === friendId ? next : x)) }
          : b,
      )

    put(withReaction(entry, removing ? null : kind))
    setPop(friendId + kind)
    mark(key, true)
    try {
      put(removing ? await api.removeReaction(friendId, wk) : await api.reactToFriend(friendId, kind, wk))
    } catch (e) {
      put(entry)
      toast(e.message)
    } finally {
      mark(key, false)
    }
  }

  /* ---------- afișare ---------- */
  const incoming = requests?.incoming ?? []
  const outgoing = requests?.outgoing ?? []
  const entries = board?.week.offset === week ? board.entries : (board?.entries ?? [])
  const shownWeek = board?.week.offset === week ? board.week : null
  const weekLabel = shownWeek ? `${shortDay(shownWeek.start)} – ${shortDay(shownWeek.end)}` : localWeek(week)
  const ranked = entries.filter((e) => e.stats.score !== null)
  const podium = ranked.length >= 3 ? [ranked[1], ranked[0], ranked[2]] : null

  return (
    <div className="page-app">
      <div className="bg-glows" aria-hidden="true">
        <div className="glow g1" />
        <div className="glow g2" />
        <div className="glow g3" />
      </div>

      <main className="wrap fr">
        <section className="intro">
          <div className="eyebrow">Prieteni</div>
          <h1>
            Învățați <span>împreună.</span>
          </h1>
          <p>
            Adaugă-ți prietenii după email și vedeți cine a avut cel mai bun procentaj săptămâna aceasta, la toate
            materiile. Dați like sau dislike la procentajul fiecăruia.
          </p>
        </section>

        <div className="fr-social">
          <aside className="fr-side">
            {/* ===== ADAUGĂ ===== */}
            <section className="glass fr-panel" aria-labelledby="fr-add-t">
              <h2 id="fr-add-t">Adaugă un prieten</h2>
              <p className="fr-sub">Scrie emailul cu care și-a făcut contul. Primește o cerere pe care o acceptă.</p>
              <form className="fr-add" onSubmit={onAdd} noValidate>
                <label className="sr-only" htmlFor="fr-email">
                  Emailul prietenului
                </label>
                <input
                  id="fr-email"
                  type="email"
                  placeholder="prieten@exemplu.ro"
                  autoComplete="off"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value)
                    if (addMsg) setAddMsg(null)
                  }}
                  aria-describedby="fr-add-msg"
                />
                <button className="btn fr-add-btn" type="submit" disabled={adding}>
                  {adding ? <Spinner /> : 'Adaugă'}
                </button>
              </form>
              <p className={'fr-msg' + (addMsg ? (addMsg.ok ? ' ok' : ' bad') : '')} id="fr-add-msg" role="status">
                {addMsg?.text}
              </p>
            </section>

            {peopleErr && (
              <div className="warn" role="alert">
                <IconWarn />
                <span>{peopleErr}</span>
              </div>
            )}

            {/* ===== CERERI PRIMITE ===== */}
            <section className="glass fr-panel" aria-labelledby="fr-req-t">
              <h2 id="fr-req-t">
                Cereri primite
                {incoming.length > 0 && (
                  <span className="fr-badge" aria-label={`${incoming.length} cereri noi`}>
                    {incoming.length}
                  </span>
                )}
              </h2>
              <p className="fr-sub">Acceptă ca să intre în clasamentul tău.</p>
              {requests === null ? (
                <Loading />
              ) : incoming.length ? (
                <ul className="fr-people">
                  {incoming.map((r) => (
                    <Person key={r.id} user={r.user}>
                      <button
                        type="button"
                        className="btn-ghost fr-sm fr-ok"
                        disabled={busy.has('req:' + r.id)}
                        onClick={() =>
                          act('req:' + r.id, () => api.acceptFriendRequest(r.id), `Acum ești prieten cu ${r.user.username}.`, true)
                        }
                      >
                        Acceptă
                      </button>
                      <button
                        type="button"
                        className="btn-ghost fr-sm"
                        aria-label={`Refuză cererea de la ${r.user.username}`}
                        disabled={busy.has('req:' + r.id)}
                        onClick={() => act('req:' + r.id, () => api.deleteFriendRequest(r.id), 'Cerere refuzată.')}
                      >
                        ✕
                      </button>
                    </Person>
                  ))}
                </ul>
              ) : (
                <p className="fr-empty">Nicio cerere nouă.</p>
              )}
            </section>

            {/* ===== PRIETENII TĂI ===== */}
            <section className="glass fr-panel" aria-labelledby="fr-list-t">
              <h2 id="fr-list-t">Prietenii tăi</h2>
              {friends !== null && (
                <p className="fr-sub">
                  {friends.length === 1 ? '1 prieten în clasament' : `${friends.length} prieteni în clasament`}
                </p>
              )}
              {friends === null ? (
                <Loading />
              ) : friends.length ? (
                <ul className="fr-people">
                  {friends.map((f) => (
                    <Person key={f.user.id} user={f.user}>
                      <button
                        type="button"
                        className="btn-ghost fr-sm"
                        aria-label={`Șterge prietenul ${f.user.username}`}
                        disabled={busy.has('fr:' + f.user.id)}
                        onClick={() =>
                          act(
                            'fr:' + f.user.id,
                            () => api.removeFriend(f.user.id),
                            `${f.user.username} nu mai e în lista ta.`,
                            true,
                          )
                        }
                      >
                        Șterge
                      </button>
                    </Person>
                  ))}
                </ul>
              ) : (
                <p className="fr-empty">Încă nu ai prieteni. Adaugă unul după email.</p>
              )}

              <h3 className="fr-out-t">Cereri trimise</h3>
              {requests === null ? null : outgoing.length ? (
                <ul className="fr-people">
                  {outgoing.map((r) => (
                    <Person key={r.id} user={r.user} note="în așteptare">
                      <button
                        type="button"
                        className="btn-ghost fr-sm"
                        disabled={busy.has('req:' + r.id)}
                        onClick={() => act('req:' + r.id, () => api.deleteFriendRequest(r.id), 'Cerere anulată.')}
                      >
                        Anulează
                      </button>
                    </Person>
                  ))}
                </ul>
              ) : (
                <p className="fr-empty">Nicio cerere trimisă.</p>
              )}
            </section>
          </aside>

          {/* ===== CLASAMENT ===== */}
          <section className="glass fr-lb" aria-labelledby="fr-lb-t" aria-busy={boardLoading}>
            <div className="fr-lb-head">
              <div>
                <div className="eyebrow">Clasament</div>
                <h2 id="fr-lb-t">{weekTitle(week)}</h2>
              </div>
              <div className="fr-week" role="group" aria-label="Alege săptămâna">
                <button
                  type="button"
                  aria-label="Săptămâna anterioară"
                  disabled={boardLoading || !(shownWeek && board.canGoBack)}
                  onClick={() => changeWeek(-1)}
                >
                  ‹
                </button>
                <span>{weekLabel}</span>
                <button
                  type="button"
                  aria-label="Săptămâna următoare"
                  disabled={boardLoading || week === 0}
                  onClick={() => changeWeek(1)}
                >
                  ›
                </button>
              </div>
            </div>
            <p className="fr-lb-note">
              Procentajul = răspunsuri corecte la flashcards, la toate materiile, de luni până duminică.
            </p>

            {boardErr && !board ? (
              <div className="warn" role="alert">
                <IconWarn />
                <span>{boardErr}</span>
                <button type="button" className="btn-ghost fr-sm fr-retry" onClick={refreshBoard}>
                  Reîncearcă
                </button>
              </div>
            ) : !board ? (
              <div className="fr-loading">
                <Spinner /> Se încarcă clasamentul…
              </div>
            ) : (
              <div className={'fr-board' + (boardLoading ? ' is-loading' : '')}>
                {boardErr && (
                  <div className="warn" role="alert">
                    <IconWarn />
                    <span>{boardErr}</span>
                  </div>
                )}
                {podium && (
                  <div className="fr-podium" aria-hidden="true">
                    {podium.map((e, i) => (
                      <div key={e.user.id} className={'fr-pod ' + ['p2', 'p1', 'p3'][i]}>
                        <div className="fr-av">
                          <img src={avatarFor(e.user.gender).src} alt="" />
                          <span className="fr-medal">{e.rank}</span>
                        </div>
                        <b>{e.me ? 'Tu' : e.user.username}</b>
                        <span className="fr-pc">{e.stats.score}%</span>
                      </div>
                    ))}
                  </div>
                )}
                <ol className="fr-rows" aria-label="Clasament">
                  {entries.map((e) => (
                    <Row
                      key={e.user.id}
                      entry={e}
                      busy={busy.has('react:' + e.user.id)}
                      pop={pop}
                      onPopEnd={() => setPop(null)}
                      onReact={(kind) => react(e, kind)}
                    />
                  ))}
                  {entries.length === 1 && (
                    <li className="fr-lb-empty">Adaugă prieteni ca să vă comparați procentajele.</li>
                  )}
                </ol>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  )
}

function Loading() {
  return (
    <div className="fr-loading">
      <Spinner /> Se încarcă…
    </div>
  )
}

function Person({ user, note, children }) {
  return (
    <li className="fr-person">
      <img src={avatarFor(user.gender).src} alt="" width="40" height="40" />
      <div className="fr-who">
        <b title={user.username}>{user.username}</b>
        <small title={user.email}>
          {note && <span className="fr-tag">{note}</span>}
          <span className="fr-mail">{user.email}</span>
        </small>
      </div>
      <div className="fr-acts">{children}</div>
    </li>
  )
}

const IconUp = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M7 10v11H3V10h4zM7 10l4-8c1.7 0 3 1.3 3 3v4h5.2a2 2 0 0 1 2 2.3l-1.4 8A2 2 0 0 1 17.8 21H7" />
  </svg>
)
const IconDown = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M17 14V3h4v11h-4zM17 14l-4 8c-1.7 0-3-1.3-3-3v-4H4.8a2 2 0 0 1-2-2.3l1.4-8A2 2 0 0 1 6.2 3H17" />
  </svg>
)

function Row({ entry: e, busy, pop, onPopEnd, onReact }) {
  const { stats } = e
  const top = stats.score !== null && e.rank <= 3 ? ' top' + e.rank : ''
  const sub =
    stats.score === null
      ? 'nu a rezolvat carduri'
      : `${stats.correct} din ${stats.answered} corecte · ${formatDuration(stats.studySeconds)}`

  const button = (kind, n, Icon, label) => (
    <button
      type="button"
      className={'fr-rbtn ' + (kind === 'LIKE' ? 'like' : 'dis') + (pop === e.user.id + kind ? ' pop' : '')}
      aria-pressed={e.myReaction === kind}
      disabled={e.me}
      aria-disabled={busy || undefined}
      aria-label={`${label} la procentajul lui ${e.user.username} (${n})`}
      title={e.me ? 'Reacțiile primite de tine' : label}
      onClick={() => !busy && onReact(kind)}
      onAnimationEnd={onPopEnd}
    >
      <Icon />
      <span className="n">{n}</span>
    </button>
  )

  return (
    <li className={'fr-row' + (e.me ? ' me' : '') + top}>
      <span className="fr-pos" aria-label={`Locul ${e.rank}`}>
        {e.rank}
      </span>
      <img src={avatarFor(e.user.gender).src} alt="" width="52" height="52" />
      <div className="fr-name">
        <b>
          {e.user.username}
          {e.me && <span className="fr-you">Tu</span>}
        </b>
        <small>{sub}</small>
      </div>
      <div className="fr-score">
        <b className={stats.score === null ? 'none' : ''}>{stats.score === null ? '–' : stats.score + '%'}</b>
        <span className="meter" aria-hidden="true">
          <i style={{ width: (stats.score ?? 0) + '%' }} />
        </span>
      </div>
      <div className="fr-react">
        {button('LIKE', e.reactions.likes, IconUp, 'Like')}
        {button('DISLIKE', e.reactions.dislikes, IconDown, 'Dislike')}
      </div>
    </li>
  )
}
