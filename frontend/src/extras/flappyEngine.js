import anvil from '../assets/flappy-anvil.svg'

/*
 * Flappy Forge: nicovala cu aripi zboară printre teancuri de cărți. Motorul e luat din designul demo
 * (canvas 380×520). createFlappy(canvas, { onBest }) -> { start(), stop(), flap() }.
 */

const W = 380
const H = 520
const G = 0.4
const JUMP = -7
const GAP = 200
const PW = 62
const SPEED = 2.3
const BOOK = ['#b23c59', '#8e2b44', '#e07a94', '#6d1d32', '#c36a7e', '#f6c2ce', '#a44d5e']
const KEY = 'studyforge.flappy.best'

export function readBest() {
  try {
    return parseInt(window.localStorage.getItem(KEY) || '0', 10) || 0
  } catch {
    return 0
  }
}

function saveBest(n) {
  try {
    window.localStorage.setItem(KEY, String(n))
  } catch {
    // stocare indisponibilă: recordul ține doar cât e deschisă pagina
  }
}

// sprite-ul se pregătește o singură dată (oglindit, ca nicovala să privească spre dreapta)
const SW = 104
const SH = 69
let sprite = null
function getSprite() {
  if (sprite) return sprite
  sprite = document.createElement('canvas')
  sprite.width = SW * 2
  sprite.height = SH * 2
  const img = new Image()
  img.onload = () => {
    const c = sprite.getContext('2d')
    c.translate(SW * 2, 0)
    c.scale(-1, 1)
    c.drawImage(img, 0, 0, SW * 2, SH * 2)
  }
  img.src = anvil
  return sprite
}

export function createFlappy(cv, { onBest } = {}) {
  const ctx = cv.getContext('2d')
  const spr = getSprite()
  let bird
  let pipes
  let score
  let best = readBest()
  let state = 'ready'
  let raf = 0
  let frame = 0
  let newBest = false

  function books(n) {
    const a = []
    for (let i = 0; i < n; i++) {
      a.push({
        h: 15 + Math.floor(Math.random() * 10),
        dx: Math.round((Math.random() - 0.5) * 12),
        w: PW - 2 + Math.round(Math.random() * 12),
        c: BOOK[Math.floor(Math.random() * BOOK.length)],
        band: Math.random() < 0.6,
        side: Math.random() < 0.5,
      })
    }
    return a
  }
  function addPipe() {
    const top = 50 + Math.random() * (H - GAP - 130)
    pipes.push({ x: W + 10, top, passed: false, tb: books(30), bb: books(30) })
  }
  function reset() {
    newBest = false
    bird = { x: 96, y: H / 2 - 40, v: 0 }
    pipes = []
    score = 0
    frame = 0
    state = 'ready'
    addPipe()
    pipes[0].x = 230
    pipes[0].top = 120
    addPipe()
    pipes[1].x = 470
    pipes[1].top = 190
  }
  function flap() {
    if (state === 'over') {
      reset()
      return
    }
    if (state === 'ready') state = 'play'
    bird.v = JUMP
  }
  function rr(x, y, w, h, r) {
    ctx.beginPath()
    ctx.moveTo(x + r, y)
    ctx.arcTo(x + w, y, x + w, y + h, r)
    ctx.arcTo(x + w, y + h, x, y + h, r)
    ctx.arcTo(x, y + h, x, y, r)
    ctx.arcTo(x, y, x + w, y, r)
    ctx.closePath()
  }
  function drawBook(b, x, y) {
    const bx = x + b.dx - (b.w - PW) / 2
    ctx.fillStyle = 'rgba(0,0,0,.35)'
    rr(bx + 2, y + 2, b.w, b.h, 3)
    ctx.fill()
    ctx.fillStyle = b.c
    rr(bx, y, b.w, b.h, 3)
    ctx.fill()
    ctx.fillStyle = '#f3e4d6'
    if (b.side) ctx.fillRect(bx + b.w - 7, y + 3, 4, b.h - 6)
    else ctx.fillRect(bx + 3, y + 3, 4, b.h - 6)
    if (b.band) {
      ctx.fillStyle = 'rgba(255,233,168,.75)'
      ctx.fillRect(bx + (b.side ? 12 : 16), y + b.h / 2 - 1.5, b.w - 30, 3)
    }
    ctx.strokeStyle = 'rgba(23,6,10,.55)'
    ctx.lineWidth = 1.2
    rr(bx + 0.5, y + 0.5, b.w - 1, b.h - 1, 3)
    ctx.stroke()
  }
  function drawStacks(p) {
    let y = p.top
    let i = 0
    while (y > -30 && i < p.tb.length) {
      const b = p.tb[i++]
      y -= b.h
      drawBook(b, p.x, y)
    }
    y = p.top + GAP
    i = 0
    while (y < H - 40 && i < p.bb.length) {
      const c = p.bb[i++]
      drawBook(c, p.x, y)
      y += c.h
    }
  }
  function panel(a, b) {
    const py = H - 170
    ctx.fillStyle = 'rgba(23,6,10,.82)'
    rr(30, py - 52, W - 60, 104, 18)
    ctx.fill()
    ctx.strokeStyle = 'rgba(247,214,220,.2)'
    ctx.lineWidth = 1
    ctx.stroke()
    ctx.fillStyle = '#f7d6dc'
    ctx.font = '700 26px "Space Grotesk",sans-serif'
    ctx.fillText(a, W / 2, py - 6)
    ctx.fillStyle = '#e07a94'
    ctx.font = '500 15px Inter,sans-serif'
    ctx.fillText(b, W / 2, py + 26)
  }
  function draw() {
    const g = ctx.createLinearGradient(0, 0, 0, H)
    g.addColorStop(0, '#3a1019')
    g.addColorStop(1, '#17060a')
    ctx.fillStyle = g
    ctx.fillRect(0, 0, W, H)
    ctx.fillStyle = 'rgba(224,122,148,.18)'
    for (let i = 0; i < 14; i++) {
      let sx = (i * 67 - frame * 0.3) % W
      if (sx < 0) sx += W
      ctx.beginPath()
      ctx.arc(sx, ((i * 97) % (H - 120)) + 20, 1.6, 0, 7)
      ctx.fill()
    }
    pipes.forEach(drawStacks)
    ctx.fillStyle = '#3a1a14'
    ctx.fillRect(0, H - 40, W, 40)
    ctx.fillStyle = '#5a2c20'
    ctx.fillRect(0, H - 40, W, 8)
    ctx.fillStyle = 'rgba(0,0,0,.25)'
    for (let k = 0; k < W / 48 + 1; k++) ctx.fillRect((((k * 48 - frame * SPEED) % W) + W) % W, H - 30, 2, 30)

    ctx.save()
    ctx.translate(bird.x, bird.y)
    ctx.rotate(Math.max(-0.4, Math.min(0.9, bird.v / 12)))
    const flapS = state === 'play' ? 1 + Math.max(0, -bird.v) * 0.015 : 1 + Math.sin(frame / 6) * 0.03
    ctx.scale(1, flapS)
    ctx.shadowColor = 'rgba(247,214,220,.35)'
    ctx.shadowBlur = 18
    ctx.drawImage(spr, -SW / 2, -SH / 2, SW, SH)
    ctx.restore()

    ctx.textAlign = 'center'
    ctx.fillStyle = '#f7d6dc'
    ctx.font = '700 44px "Space Grotesk",sans-serif'
    if (state !== 'ready') {
      ctx.save()
      ctx.shadowColor = 'rgba(0,0,0,.8)'
      ctx.shadowBlur = 10
      ctx.lineWidth = 5
      ctx.strokeStyle = '#17060a'
      ctx.strokeText(score, W / 2, 70)
      ctx.fillText(score, W / 2, 70)
      ctx.restore()
    }
    if (state === 'ready') panel('Flappy Forge', 'Apasă Spațiu sau click ca să zbori')
    if (state === 'over') {
      panel(
        newBest ? 'Record nou: ' + score + '!' : 'Ai obținut ' + score + (score === 1 ? ' punct' : ' puncte'),
        newBest ? 'Apasă din nou ca să-l bați' : 'Record: ' + best + ' · apasă ca să reîncepi',
      )
    }
  }
  function over() {
    if (state !== 'play') return
    state = 'over'
    newBest = score > best && score > 0
    if (score > best) {
      best = score
      saveBest(best)
      onBest?.(best)
    }
  }
  function step() {
    frame++
    if (state === 'play') {
      bird.v += G
      bird.y += bird.v
      if (frame % 105 === 0) addPipe()
      pipes.forEach((p) => {
        p.x -= SPEED
        if (!p.passed && p.x + PW < bird.x) {
          p.passed = true
          score++
        }
        if (bird.x + 14 > p.x - 8 && bird.x - 14 < p.x + PW + 8 && (bird.y - 14 < p.top || bird.y + 16 > p.top + GAP)) over()
      })
      pipes = pipes.filter((p) => p.x > -PW - 20)
      if (bird.y + 20 > H - 40 || bird.y < -40) over()
    } else if (state === 'ready') {
      bird.y = H / 2 - 40 + Math.sin(frame / 12) * 8
    }
    draw()
    raf = requestAnimationFrame(step)
  }

  return {
    start() {
      reset()
      cancelAnimationFrame(raf)
      raf = requestAnimationFrame(step)
    },
    stop() {
      cancelAnimationFrame(raf)
    },
    flap,
  }
}
