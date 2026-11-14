# Study Forge – frontend

React + Vite. Designul (paleta Holst, sticlă, bara de sus, cardurile 3D) vine din paginile statice
`design-reference/index.html` și `design-reference/flashcards.html`, portate în React și legate la backend-ul Spring Boot.

## Pornire

```bash
# 1. backend (portul 8080) – din rădăcina proiectului
docker compose up        # sau rulezi BackendApplication din IntelliJ

# 2. frontend
cd frontend
npm install
npm run dev              # http://localhost:5173
```

Vite trimite toate cererile `/api/*` către `http://localhost:8080` (proxy în `vite.config.js`), deci nu ai probleme de CORS.
Alte setări (opțional) în `.env.example`: `VITE_PROXY_TARGET` (alt backend) sau `VITE_API_URL` (apel direct, fără proxy).

Pentru generare trebuie setat `GEMINI_API_KEY` în `.env`-ul backend-ului. Dacă lipsește, paginile arată un avertisment
(din `GET /api/status`).

## Pagini și endpoint-uri

| Rută               | Ce face                                              | Backend                                   |
|--------------------|------------------------------------------------------|-------------------------------------------|
| `/`                | pagina principală                                    | –                                         |
| `/plan`            | încarci programa (PDF) → plan pe săptămâni           | `POST /api/plans` (`file`, `startDate`)   |
| `/plan/:id`        | un plan salvat (săptămâni, evaluări, sfaturi, PDF)   | `GET /api/plans/{id}`, `/pdf`             |
| `/flashcards`      | plan salvat sau PDF → set de carduri                 | `POST /api/quizzes` (`file`, `week`, `count`), `GET /api/plans` |
| `/flashcards/:id`  | un set salvat, de studiat                            | `GET /api/quizzes/{id}`, `/pdf`           |
| `/biblioteca`      | toate planurile și seturile, filtru după plan        | `GET /api/library`, `GET /api/quizzes?planId=` |

La „Din plan salvat”, frontend-ul descarcă PDF-ul planului (`/api/plans/{id}/pdf`) și îl retrimite la `POST /api/quizzes`,
iar backend-ul îl recunoaște după metadate/hash și folosește conținutul săptămânii alese.

## Structura

```
src/
  api.js              clientul pentru backend (fetch, erori ProblemDetail -> mesaj)
  App.jsx             rutele
  router/             router minimal (History API), fără dependențe
  pages/              Home, Plan, Flashcards, Library, NotFound
  components/         Topbar, PlanView, PdfLinks, StatusBanner, iconițe
  styles/             home.css, app.css (generate din designul static), pages.css (completări)
  index.css           paleta + bara de sus
```

## Limitări față de mockup

- Backend-ul returnează doar întrebare + răspuns: nu există topic, dificultate, explicație sau exemplu pe card.
- Nu există notițe de curs, text lipit sau „generează încă 5”: `POST /api/quizzes` primește doar PDF, săptămână și număr de carduri.
- „Știut / de repetat” nu are endpoint (câmpul `mastered` există în baza de date, dar nu poate fi modificat din API),
  deci marcajele se păstrează în `localStorage`, per set.


## Progres și indicii (flashcards)

| Rută / element | Ce face | Backend |
|---|---|---|
| `/progres` | alegi materia (plan salvat sau fișier) și vezi timpul activ, scorul la flashcards, evoluția pe zile / săptămâni și mascota cu dispoziție (fericită / indiferentă / tristă) | `GET /api/library` (doar pentru lista de planuri) |
| buton cu mascota pe fața cardului (sau tasta **H**) | norișor cu un indiciu; „Alt indiciu” cere altul | `POST /api/flashcards/{id}/hint` (`previousHint` opțional) |

- Progresul se păstrează în `localStorage`, separat pentru fiecare cont (`src/progress/`); backend-ul nu are încă endpoint-uri de statistici.
  Timpul se numără doar cât fila e vizibilă și ai făcut o acțiune în ultimul minut (pe pagina unui plan și în setul de carduri).
  Scorul vine din bifa / X de pe carduri.
- Dacă generarea indiciului eșuează (de ex. lipsește `GEMINI_API_KEY`), se afișează un indiciu local, derivat din răspuns.
