# Vanessza's decisions and messages

Decisions and team messages for the cards "Announcement details page", "CSS variables", "UI polish and poster redesign", and for the fixes to the existing frontend and backend code before DEMO 1.

## Decisions

### Announcement details page (`/announcements/:id`)

- **Route:** a lazy route placed after `announcements/new`, so `/announcements/new` still opens the form. The browser tab shows the pet name, or the species when there is no name, for example "Misi | SafePaws".
- **States:** loading, not found and error. An id that is not a positive whole number (`abc`, `0`, `-3`) shows "Announcement not found" without calling the backend. A 404 from the backend shows the same page, and any other error shows "Could not load the announcement".
- **Gallery:** the main photo is shown first, like on the card. The photo is shown whole inside a 4:3 frame (`object-fit: contain`), so portrait and landscape photos are neither cropped nor stretched. Thumbnails only appear when there are 2 or more photos. They are buttons, so they also work with the keyboard.
- **Details:** only the fields that are filled in are shown, so there is no empty "Breed". The date label is "Lost on" or "Found on".
- **Contact:** the phone number is a `tel:` link, so it can be called directly from a phone.
- **Status:** "Mark as resolved" and "Mark as active" use `PATCH /announcements/{id}/status`. The button is disabled while saving, so a double click sends one request. The page shows the announcement returned by the backend, so the "Resolved" badge only appears if the change was saved. On an error the status stays as it was.
- **Map:** `<app-map>` with one pin, zoom 15, and no popup. It is hidden when the announcement has no coordinates.
- **Clickable cards:** the species on the card is a link to the details page, stretched over the whole card with `::after`. The "Download poster" button stays on top and works on its own, and the keyboard focus outlines the whole card.
- **After posting:** the form opens the new announcement's details page after 1.5 s (`REDIRECT_DELAY_MS`), so the success message can be read first. "View announcement" in the message goes to the same page.

### CSS variables (`frontend/src/styles.css`)

- **Every color, shadow and corner radius is a variable**, named by its role, not by how it looks:
  - brand: `--color-primary`, `--color-primary-dark`, `--color-primary-light`, `--color-on-primary`
  - announcement types: `--color-lost`, `--color-found`
  - text: `--color-text`, `--color-text-muted`, `--color-text-subtle`
  - surfaces: `--color-background`, `--color-surface`, `--color-surface-muted`, `--color-border`
  - messages: `--color-error`, `--color-error-light`, `--color-error-light-hover`, `--color-success`, `--color-success-light`
  - shadows: `--shadow-card`, `--shadow-card-hover`
  - corners: `--radius-control` (6px, buttons and fields), `--radius-card` (8px, cards and boxes)
- **`--color-error` is separate from `--color-lost`:** they have the same red today, but an error message is not a LOST announcement, so changing one must not change the other.
- **Near-identical greys and shadows were merged:** four border greys became `--color-border`, and the card shadows became `--shadow-card`.
- **Kept as plain values on purpose:** the white parts of the map pin (they are part of the icon), the focus ring of the form fields and the active header link (each used once).
- **Style budget:** the `anyComponentStyle` warning is 6 kB instead of 4 kB, because `var(--color-primary)` is longer than `#2f5d62`. The error limit is still 8 kB.

### UI polish

- **One date format:** `DATE_FORMAT` in `frontend/src/app/shared/date-format.ts` ("30 Oct 2026, 18:00"). The card, the popup and the details page use it. The poster uses the same format.
- **Readable type labels:** the LOST / FOUND labels come from `TYPE_LABELS` ("Lost", "Found") and are uppercased with CSS. The text in the page is "Lost", so tests check "Lost", not "LOST".
- **Wording:** "lost pet" for LOST, "found animal" for FOUND, and "animal" when a text is about both. The area field is called "Area" everywhere, including its error messages. Every message ends with a full stop.
- **Buttons:** all buttons and fields have the same corners (`--radius-control`) and bold text (600). Labels such as LOST, FOUND and Resolved keep 4px corners, so they look different from buttons.
- **Resolved cards:** a grey "Resolved" badge in the top right corner, a grey photo and a muted background, like the details page.
- **Phone screens (below 600px):** the header links become 3 equal tabs on one row, the page margins are smaller, and the filter bar has 2 columns. The filter fields have `min-width: 0`, so a long option cannot push the page wider than the screen.

### Poster redesign (`PosterService`, on top of Alexandru's card)

- **Same behavior as before:** the same endpoint, the main photo (or the first one), the QR code from `QrCodeService`, long texts cut with "...", a fallback font and always one page.
- **Drawn directly on the page:** the poster is drawn with `PdfContentByte` instead of tables, so it can have rounded corners and exact positions.
- **Layout:** large LOST / FOUND / RESOLVED badges in the app colors, a question as the title ("Have you seen Luna?", "Is this your cat?", "Luna is back home!"), the location line, a large rounded photo, the details in two columns, and the contact card with the QR code at the bottom.
- **Equal spacing:** the badge, the title and the location line are the same visible distance apart (`TITLE_GAP`), measured from the letters, not from the line height.
- **The photo fills the free space:** the height of every other part is measured first, and the photo gets what is left (up to 300 pt), so the page is always full.
- **Fonts:** `DejaVuSans.ttf` (regular) was added next to `DejaVuSans-Bold.ttf`, both in `backend/src/main/resources/fonts/`. Both have the Romanian letters.
- **Resolved:** a resolved announcement gets a grey photo, like its card in the app.
- **Long phone numbers** get a smaller font, so they always stay on one line.

### Opening the app from a phone

- **The frontend finds the backend by itself:** `apiUrl` is `` `http://${window.location.hostname}:8080` `` in both environments, so a phone that opens `http://<laptop IP>:4200` calls `http://<laptop IP>:8080`.
- **Two new variables in `.env`:** `FRONTEND_URL` (the address in the poster QR code) and `CORS_ALLOWED_ORIGINS` (the addresses allowed to call the API, comma-separated). Both default to `http://localhost:4200`. The laptop IP only goes in `.env`, never in `.env.example`.
- **Location on a phone:** browsers only give the location on HTTPS or `localhost`, so "Closest first" and the radius filter don't work from a phone over `http://<IP>`. Show them on the laptop.

### Fixes to the existing frontend code

- **Filters in the URL:** names that every JavaScript object has (`constructor`, `toString`...) are ignored, using `Object.hasOwn`.
- **Form:**
  - every field is locked while the announcement is being sent, and map clicks are ignored;
  - the map pin follows the chosen type (red or green) and the values stay when the type changes;
  - names, breeds and colors accept real values such as "Mix (50/50)" or "Ana-Maria O'Brien";
  - the contact name cannot be only spaces, and the date cannot be in the future;
  - validation errors from the backend (`fieldErrors`) are shown under the matching field.

## Messages

### Everyone

> Everything for DEMO 1 is on main. After pulling, run `npm install` in `frontend`. The `.env` has two optional new variables, `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS`. You only need them to open the app from a phone: put `http://<your laptop IP>:4200` in `FRONTEND_URL` and add it to `CORS_ALLOWED_ORIGINS`, restart the backend, and start the frontend with `npm start -- --host 0.0.0.0`. The phone has to be on the same Wi-Fi.
>
> The next migration is still `V6`.

### Luca and Victor

> From now on, please use only the CSS variables from `src/styles.css` instead of hex codes or pixel values for colors, shadows and corners (`var(--color-primary)`, `var(--color-text-muted)`, `var(--radius-control)`...). If you need a new color, add a variable there first.
>
> Dates: use `DATE_FORMAT` from `shared/date-format.ts` with the `date` pipe. Types and species: use `TYPE_LABELS` and `SPECIES_LABELS`, never the raw `LOST` or `CAT`.
>
> Wording: "lost pet" for LOST, "found animal" for FOUND, "animal" for both, "Area" for the area field, and a full stop at the end of every message.

### Luca

> The details page is on main at `/announcements/:id`, so "View details" in the map popup works now and there is nothing to change. The page uses `<app-map>` with one pin, `[zoom]="15"` and no popup.

### Jessica

> New numbers for `testing.md`: 214 frontend tests, about 95% line coverage. 127 backend tests, 97% instruction coverage.
>
> New manual scenarios:
> - **Details page:** click a card (the photo, the date, anywhere except "Download poster"), and the details page opens. Announcements 9, 13 and 21 have 2 photos: clicking a thumbnail changes the big photo. The phone number is a link. "← Back to announcements" goes back to the list.
> - **Status:** "Mark as resolved" shows the "Resolved" badge and a grey photo, and it stays after a reload. "Mark as active" removes it. With the backend stopped, the button shows "Could not update the status. Please try again." and the status does not change.
> - **Not found:** `/announcements/9999` and `/announcements/abc` show "Announcement not found".
> - **After posting:** the success message shows, and after about 1.5 s the new announcement's details page opens.
> - **Resolved in the list:** choose Status "Resolved", and the cards have the "Resolved" badge and a grey photo.
> - **Keyboard (Chrome):** Tab through the list, and the whole card is outlined; Enter opens the details page.
> - **Phone (DevTools, iPhone SE):** no horizontal scrolling on any page, the filters are in 2 columns, the header links fit on one row.
> - **Poster:** check a LOST one, a FOUND one without a name, and a resolved one. Everything fits on one page, and the QR code opens the details page.
>
> H11 is not blocked anymore.

### Alexandru

> I redesigned the poster on top of your card #33, so it looks like the app. The behavior is the same: the main photo, the QR code, long texts cut with "..." and one page. The texts changed ("Have you seen Oscar?", "If you see Oscar, please call", "Recognize this cat? Please call"), so `PosterServiceTest` was updated and has 2 new tests. There is a new font file, `DejaVuSans.ttf`, next to the bold one.

### Patrick

> The details page uses your `PATCH /announcements/{id}/status` for "Mark as resolved" and "Mark as active", and shows the announcement your endpoint returns.

## Documentation updated

- `architecture.md`: the colors are CSS variables in `frontend/src/styles.css`, named by role, and components use them with `var(--name)`, never hex codes. The frontend tree should list `pages/announcement-details` and `shared/date-format.ts`.
- `usage-guide.md`:
  - **Viewing an announcement:** click any card in the list, or "View details" on a map pin, to open the full announcement: photos, details, the contact (tap the phone number to call), the location on the map and the printable poster.
  - **Marking it as resolved:** when the pet is back home, press "Mark as resolved". The announcement gets a "Resolved" badge and leaves the default list. "Mark as active" reverts it.
- `decisions.md`: ADR 010 to ADR 013 (no accounts in DEMO 1, poster, CSS variables, configuration and phone access).
- `testing.md`: the numbers and scenarios in the message for Jessica.
- `frontend/README.md`: Node 22.22.3+ or 24.15.0+, and remove the mentions of the map and of `shared/`.
- `backend/README.md`: `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS`.

---

## DEMO 2: user accounts

Decisions and team messages for the cards until 18.11. The reasons are in ADR 014 (`decisions.md`).

### Endpoints

All requests and responses use JSON. Errors use the usual `ErrorResponse` format (see `api.md`), and messages have no full stop at the end, like the rest of the API.

**`POST /auth/register`** (public) – Antonia

```json
{ "email": "ana@safepaws.dev", "password": "SafePaws1!", "displayName": "Ana Pop" }
```

- `201 Created` → the new user (never the password):
  ```json
  { "id": 1, "email": "ana@safepaws.dev", "displayName": "Ana Pop" }
  ```
- `400` with `fieldErrors`:
  - `email`: "Email is required", "Email must be a valid email address", "Email must be at most 255 characters"
  - `password`: "Password is required", "Password must have at least 8 characters, one uppercase letter, one digit and one special character", "Password must be at most 72 characters"
  - `displayName`: "Display name is required", "Display name must be at most 50 characters"
- `409 Conflict` → "Email is already registered"
- The email is trimmed and saved in lowercase. A special character is anything that is not a letter or a digit.
- Registering does not log the user in: the frontend calls `/auth/login` right after.

**`POST /auth/login`** (public) – Alexandru

```json
{ "email": "ana@safepaws.dev", "password": "SafePaws1!" }
```

- `200 OK`:
  ```json
  {
    "token": "<JWT>",,
    "expiresAt": "2026-11-16T14:30:00",
    "user": { "id": 1, "email": "ana@safepaws.dev", "displayName": "Ana Pop" }
  }
  ```
- `401 Unauthorized` → "Invalid email or password" (the same message for an unknown email and for a wrong password, so nobody can find out which emails have an account)

**`GET /auth/me`** (logged in) – Alexandru

- `200 OK` → the user, same shape as `user` above
- `401 Unauthorized` → "Authentication is required"

**`PUT /announcements/{id}`** (owner only) – Antonia

- The body is the same as for `POST /announcements`, with the same validations. Every field is replaced.
- The status and the photos do not change here: the status has its own endpoint, and photos are added with `POST /announcements/{id}/photos`.
- `200 OK` → the announcement, same shape as `GET /announcements/{id}`

### Who can do what

| Endpoint | Without an account | Another user | Owner |
|---|---|---|---|
| `GET /announcements`, `GET /announcements/{id}`, photos, posters | ✅ | ✅ | ✅ |
| `POST /announcements` | 401 | ✅ (becomes the owner) | – |
| `PUT /announcements/{id}` | 401 | 403 | ✅ |
| `PATCH /announcements/{id}/status` | 401 | 403 | ✅ |
| `POST /announcements/{id}/photos` | 401 | 403 | ✅ |

- **401** → "Authentication is required" (no token, or the token is invalid or expired)
- **403** → "You can only change your own announcements"
- The checks run in this order: 401, then 400 (validation), then 404, then 403.
- On public endpoints an invalid or expired token is ignored: the request works as if there were no token, so the list never breaks.
- `GET /announcements/{id}` and the responses of `POST`, `PUT` and `PATCH` include `"ownerId": 1`. The list (`GET /announcements`) does not need it.

### Token

- Header: `Authorization: Bearer <token>`
- HS256, signed with `JWT_SECRET` from `.env`, valid for 24 hours. The token holds the user id.
- `CORS_ALLOWED_ORIGINS` must allow the `Authorization` header.

### Demo accounts (V6, Matei)

| Email | Display name | Password |
|---|---|---|
| `ana@safepaws.dev` | Ana Pop | `SafePaws1!` |
| `mihai@safepaws.dev` | Mihai Rus | `SafePaws1!` |
| `elena@safepaws.dev` | Elena Moldovan | `SafePaws1!` |

The existing announcements are shared between these three accounts.

### Messages

**Everyone**

> The cards until 18.11 are on Trello, and the contract is above. Please use exactly these endpoints, field names and messages, so the frontend and the backend fit together on the first try. Every PR needs one approval from someone other than its author.
>
> Order: Matei (V6, V7) → Antonia (PR 1) → Alexandru → Patrick → Antonia (PR 2) and Matei (V8). Luca and Victor can start right away from the contract. When your PR is merged, write it in the chat, because someone is waiting for it.
>
> Only Matei writes migrations: V6 and V7 now, V8 after Patrick.

**Antonia and Alexandru**

> The password hashing (`PasswordEncoder`, BCrypt) is defined once, by Alexandru in the security config. Antonia uses it for register, Alexandru for login. Antonia, please push the `User` entity and repository in a separate commit first, so Alexandru can start the login.

**Patrick and Antonia**

> Patrick writes the owner check once (for the status and the photos). Antonia reuses it for editing in her second PR.

**Luca and Victor**

> Luca, please open a small PR with only the `AuthService` first (it can return test data until the backend is ready), because Victor’s pages use it. Victor, “Mark as resolved” compares `ownerId` with the logged-in user’s `id`; until Patrick’s PR is merged, you can test it with a hand-written `ownerId`.

**Tudor**

> `JWT_SECRET` has to be in `.env.example` together with Alexandru’s PR, otherwise nobody’s backend starts after pulling. The request file is needed by Jessica for testing the editing.