# Prieteni, clasament săptămânal și like / dislike — API

Toate endpoint-urile cer `Authorization: Bearer <token>`. Erorile au formatul obișnuit (`ProblemDetail`), cu mesajul pentru utilizator în `detail`.

## 1. Sincronizarea progresului (din pagina Progres)

Progresul e ținut în browser (localStorage), deci frontendul trimite totalurile **pe zile**, adunate pe toate materiile. Din ele serverul calculează clasamentul pe săptămâni (luni–duminică).

`PUT /api/progress/days`

```json
{ "days": [
  { "date": "2026-11-14", "correct": 12, "wrong": 3, "seconds": 1830.5 },
  { "date": "2026-11-13", "correct": 30, "wrong": 6, "seconds": 4200 }
] }
```

Răspuns: `{ "saved": 2 }`.

- De unde vin valorile în frontend: pentru fiecare zi din `loadProgress(user.id).days` → `dayTotals(progress, day)` → `{ ok, bad, sec }` → `{ date: day, correct: ok, wrong: bad, seconds: sec }`.
- Zilele trimise sunt înlocuite; cele netrimise rămân neschimbate. Poți trimite tot istoricul (la login sau la deschiderea paginii Prieteni) sau doar ziua de azi (după fiecare răspuns la un card).
- Limite: maximum 1000 de zile pe cerere, nicio dată din viitor, aceeași zi o singură dată pe cerere, iar `seconds` e plafonat la 86400.

## 2. Prieteni (panoul din stânga)

| Metodă | URL | Corp | Răspuns |
|---|---|---|---|
| `POST` | `/api/friends/requests` | `{ "email": "radu.ilie@exemplu.ro" }` | 201 `{ relation: "REQUEST_SENT", request }` sau 200 `{ relation: "FRIENDS", request }` |
| `GET` | `/api/friends/requests` | – | `{ incoming: [...], outgoing: [...] }` → „Cereri primite” și „Cereri trimise” |
| `POST` | `/api/friends/requests/{requestId}/accept` | – | `Friend` (butonul „Acceptă”) |
| `DELETE` | `/api/friends/requests/{requestId}` | – | 204 (butonul „✕” la cereri primite, „Anulează” la cereri trimise) |
| `GET` | `/api/friends` | – | `[Friend]` → „Prietenii tăi” |
| `DELETE` | `/api/friends/{friendId}` | – | 204 (butonul „Șterge”; dispar și reacțiile dintre voi) |
| `GET` | `/api/friends/search?email=...` | – | `{ user, relation, requestId }` (opțional, înainte de „Adaugă”) |

- `user` = `{ id, username, email, gender }` (avatarul după `gender`); `request` = `{ id, user, createdAt }`; `Friend` = `{ user, friendsSince }`.
- Dacă adaugi pe cineva care îți trimisese deja o cerere, ea se acceptă automat (`relation: "FRIENDS"`), la fel ca în design.
- Mesajele de eroare se potrivesc cu cele din design: emailul lipsă sau invalid și auto-adăugarea dau 400; „Nu există niciun cont cu acest email.” dă 404; „Sunteți deja prieteni.” și „I-ai trimis deja o cerere de prietenie.” dau 409.

## 3. Clasamentul

`GET /api/friends/leaderboard?week=0` — `week`: 0 = săptămâna aceasta, -1 = săptămâna trecută, ... (maximum 52 de săptămâni în urmă).

```json
{
  "week": { "offset": 0, "start": "2026-11-09", "end": "2026-11-15" },
  "canGoBack": true,
  "canGoForward": false,
  "myRank": 2,
  "entries": [
    {
      "rank": 1,
      "user": { "id": "...", "username": "Dan Ionescu", "email": "dan.ionescu@exemplu.ro", "gender": "MALE" },
      "me": false,
      "stats": { "correct": 38, "wrong": 4, "answered": 42, "score": 90, "studySeconds": 9600 },
      "reactions": { "likes": 3, "dislikes": 0 },
      "myReaction": "LIKE"
    },
    {
      "rank": 2,
      "user": { "id": "...", "username": "Ana Pop", "email": "ana.pop@exemplu.ro", "gender": "FEMALE" },
      "me": true,
      "stats": { "correct": 42, "wrong": 9, "answered": 51, "score": 82, "studySeconds": 11100 },
      "reactions": { "likes": 4, "dislikes": 1 },
      "myReaction": null
    }
  ]
}
```

- `stats.score` = procentajul săptămânii (rotunjit ca în pagina Progres) sau `null` dacă persoana nu a rezolvat carduri („–”, „nu a rezolvat carduri”).
- Subtitlul din design: `${correct} din ${answered} corecte · ${durată(studySeconds)}`.
- Ordinea: procentajul, apoi numărul de răspunsuri, apoi timpul de studiu, apoi alfabetic. Cei fără răspunsuri sunt la final.
- `rank`: cei cu același procentaj au același loc (1, 2, 2, 4), ca în design. Podiumul = primele 3 rânduri cu `score` diferit de `null`.
- Butoanele „‹” / „›”: `canGoBack` (există date mai vechi la tine sau la prieteni) și `canGoForward` (nu e săptămâna curentă).

## 4. Like / dislike

| Metodă | URL | Corp | Răspuns |
|---|---|---|---|
| `PUT` | `/api/friends/{friendId}/reaction?week=0` | `{ "type": "LIKE" }` sau `{ "type": "DISLIKE" }` | rândul actualizat (`LeaderboardEntry`) |
| `DELETE` | `/api/friends/{friendId}/reaction?week=0` | – | rândul actualizat (`LeaderboardEntry`) |
| `GET` | `/api/friends/reactions/received` | – | `[{ from, type, weekStart, at }]` |

- Reacțiile sunt per săptămână: like-ul dat săptămâna aceasta nu apare la săptămâna trecută.
- O singură reacție per prieten pe săptămână; apăsarea celuilalt buton o înlocuiește.
- În UI: click pe butonul deja activ (`myReaction === kind`) → `DELETE`; altfel → `PUT`. Răspunsul conține numerele actualizate pentru rândul respectiv.
- Pe rândul tău butoanele sunt dezactivate (arată doar ce ai primit). Serverul refuză reacțiile la tine (400) și la cei care nu îți sunt prieteni (404).

## 5. Hint la flashcards

Există deja și se potrivește cu designul: `POST /api/flashcards/{id}/hint` cu `{ "previousHint": "..." }` (opțional, pentru „Alt hint”) → `{ "hint": "..." }`. Hint-urile nu se salvează pe server; designul le ține în browser.
