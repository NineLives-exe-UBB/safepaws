# Usage & Setup Guide

This guide has two parts: how to use the app (section 0), and how to set it up and test it locally (sections 1 to 3).

## 0. Using SafePaws

Open `http://localhost:4200` (see section 2 to start it). The header has **Announcements** and **Map**, plus **Log in** and **Register** when you are not logged in. Once you are logged in it shows **Post announcement**, your display name and **Log out** instead.

Reading is public: the list, the map, an announcement's page and the posters work without an account. Posting an announcement, adding photos and marking it resolved or active need you to be logged in. For a local demo you can use the demo accounts in the root `README.md` (for example `ana@safepaws.dev` / `SafePaws1!`).

### Creating an account (`/register`)
1. Click **Register** in the header (or **Create an account.** on the login page).
2. Enter your **email address**, a **display name** (shown in the header) and a **password**. The password needs at least 8 characters, one uppercase letter, one digit and one special character (anything that is not a letter or a digit); the checklist under the field ticks each rule as you type. The maximum is 72 bytes, so about 72 characters without diacritics.
3. Enter the password again in **Confirm password**. If they differ, the form says `Passwords do not match.`
4. Click **Create account**. You are logged in straight away and taken back to the page you came from (or to the announcements list).
- If the email already has an account (capital letters do not matter), the form says `This email address is already registered.`
- Your email is saved in lowercase, without spaces at the ends. The display name must be at most 50 characters.

### Logging in (`/login`)
1. Click **Log in** in the header.
2. Enter your **email address** and **password**, and click **Log in**.
3. You are taken back to the page you came from (or to the announcements list). Your display name appears in the header.
- A wrong email or password shows `Invalid email or password.` The message is the same for both, so it does not reveal which emails have an account.
- You stay logged in for **24 hours**, also after closing the browser (the session is kept in the browser's local storage). After that you are sent to the login page and brought back to the page you were on once you log in again.
- **Log out** in the header logs you out on this browser.

### Your announcements
- An announcement you post is yours.
- *Planned, not on main yet (owner check, Patrick and Antonia):* only you can edit your announcement, mark it resolved or active and add photos to it; **Mark as resolved** / **Mark as active** is shown only to the owner, and an **Edit** page lets the owner change the details. Until then, any logged-in user can change the status of any announcement, and there is no edit page in the app (editing is only possible through the API, `PUT /announcements/{id}`).

### Browse announcements (`/announcements`)
- Each card shows the main photo (or `No photo`), a LOST or FOUND label, the species, the pet name when there is one, the area and the date.
- By default you see only active announcements, newest event first. Resolved announcements (Status: Resolved) have a grey **Resolved** badge and a grey photo.
- Click anywhere on a card to open the announcement (see **View an announcement** below).
- **Download poster** on a card downloads a printable PDF poster for that announcement.

### Filter and sort
The filter bar above the cards narrows the list:
- **Type**, **Species** and **Status** dropdowns. Status is either Active (the default) or Resolved.
- **Color**, **Breed** and **Area** text fields. They match part of the text and ignore upper/lower case and diacritics, so `manastur` finds `Mănăștur`. The list updates shortly after you stop typing.
- **Radius** (within 1, 5, 10, 25, 50 or 100 km) and **Sort by** (Newest first, Oldest first, Closest first). For a radius or Closest first, the browser asks for your location, and each card then shows how far away it is (`1.2 km away`). If you refuse the location, these two options are disabled.
- The filters are kept in the address, so you can reload the page or share a filtered list. **Clear filters** appears when a filter is active and removes them. If nothing matches, the page says `No announcements match your filters.`

### Map (`/map`)
- A pin for each announcement that has a location: red for lost, green for found.
- If you allow your location, the map starts around you; otherwise it zooms so that all the pins fit.
- Click a pin to see the photo, the name (or species), the area and the date. **View details** opens the announcement.
- The filter bar works like the one on the list, and the same filters give the same announcements. Announcements without a location are counted in a note above the map.

### Post an announcement (`/announcements/new`)
You need to be logged in: the **Post announcement** link only appears in the header after you log in.

1. Choose **Lost pet** or **Found animal**, and the **Species** (required).
2. Optionally add the pet name, breed, color and distinctive marks.
3. Enter the **date and time** (not in the future) and the **area**, then click the map to **pinpoint the location** (all required). If you allow your location, the map starts around you.
4. Enter **your name** and **phone number** (7–15 digits, optionally starting with `+`).
5. Optionally choose photos (JPG or PNG, max 5 MB each). The first one becomes the main photo; **Remove** takes a photo out.
6. Click **Submit announcement**. The form is locked while it is being sent. After the success message, the new announcement opens. If the announcement was created but the photos failed, fix or remove the photos and submit again: only the photos are sent again.

### View an announcement (`/announcements/{id}`)
- Opened by clicking a card in the list, **View details** on a map pin, or the QR code on a poster.
- Shows the photos (click a thumbnail to see another one), the details that were filled in, the date it was lost or found, the area and when it was posted.
- **Contact:** the name and the phone number. On a phone, tap the number to call.
- **Location:** a small map with the pin, when the announcement has one.
- **Download poster:** the printable PDF poster.
- **Mark as resolved:** when the pet is back home (needs you to be logged in). The announcement gets a **Resolved** badge and a grey photo, and leaves the default list. **Mark as active** reverts it.
- A link to an announcement that does not exist shows `Announcement not found`, with a link back to the list.

### The poster
A one-page A4 PDF to print and put up in the area: a large LOST or FOUND label, a question as the title ("Have you seen Luna?", "Is this your cat?"), the photo, the details, the contact and a QR code. Scanning the QR code with a phone opens the announcement in SafePaws, so people always see if it is still active. A resolved announcement's poster says that the pet is back home (or back with its family, for a found animal).

---

## 1. Prerequisites
- **Java:** JDK 21 (Required by `pom.xml` and CI)
- **Node.js:** Node.js 22.22.3+ or 24.15.0+ and npm
- **Database / Containers:** Docker Desktop & Docker Compose (or PostgreSQL 17)
- **Git**

---

## 2. Local Setup

### Database (Docker)
PostgreSQL 17 runs containerized via Docker Compose.

1. Create your local environment file from the template:
   ```bash
   cp .env.example .env
   ```
   > **Note:** `.env` is ignored by Git and must never be committed.

2. Start the PostgreSQL container:
   ```bash
   docker compose up -d
   ```

3. Confirm that the container is healthy:
   ```bash
   docker compose ps
   ```
   *(The `STATUS` column should indicate `healthy`).*

#### Troubleshooting & Database Commands
- **Port Conflict (Port 5432 in use):** If a local PostgreSQL instance is already running on port 5432, update `DB_PORT=5433` in your `.env` file before starting the container.
- **Stop Database (Retain Data):**
  ```bash
  docker compose down
  ```
- **Full Database Reset (Wipe Data & Re-run Migrations):**
  ```bash
  docker compose down -v
  ```

---

### Backend (Spring Boot)
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. The application configuration (`src/main/resources/application.properties`) automatically imports database credentials from the root `.env` file (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) and the login token secret `JWT_SECRET`. `JWT_SECRET` is required: without it the backend does not start. If your `.env` is older than the login feature, copy the `JWT_SECRET` line from `.env.example` (see `backend/README.md`). Optional environment variables:
   - `CORS_ALLOWED_ORIGINS`: Comma-separated list of allowed frontend origins (defaults to `http://localhost:4200`).
   - `FRONTEND_URL`: The frontend address used in the poster QR code (defaults to `http://localhost:4200`).

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(Flyway runs automatically on startup to validate the schema and seed initial test data).*

4. **API & Documentation Endpoints:**
   - Base API URL: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html` (public, no login needed)
   - OpenAPI 3 Spec (JSON): `http://localhost:8080/v3/api-docs`
   - Ready-made requests with the demo accounts: `backend/auth.http`

   > **Known issue:** Swagger UI and `/v3/api-docs` currently return `401`, because `SecurityConfig` does not permit `/swagger-ui/**` and `/v3/api-docs/**`.

---

### Frontend (Angular)
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the local development server:
   ```bash
   npm start
   ```
   *(Calls the Spring Boot REST API on port `8080` of the same host the page was opened from, so `http://localhost:8080` on the laptop; requires the backend to be running).*

4. Access the web interface at `http://localhost:4200`.

---

### Opening the app from a phone
The phone must be on the same Wi-Fi as the laptop (or connect the laptop to the phone's hotspot if the network blocks devices from seeing each other).

1. Find the laptop IP, for example `ipconfig getifaddr en0` on macOS (`192.168.1.23`).
2. In `.env`, set:
   ```
   FRONTEND_URL=http://192.168.1.23:4200
   CORS_ALLOWED_ORIGINS=http://localhost:4200,http://192.168.1.23:4200
   ```
   and restart the backend. Keep the IP only in `.env`, never in `.env.example`.
3. Start the frontend so other devices can reach it:
   ```bash
   npm start -- --host 0.0.0.0
   ```
4. Open `http://192.168.1.23:4200` on the phone. Posters downloaded now have a QR code that the phone can open.

Browsers only give the location on HTTPS or `localhost`, so the radius filter and **Closest first** don't work from a phone over `http://`. Use them on the laptop.

---

### Before a demo
Run `docker compose down -v`, then `docker compose up -d` and start the backend shortly before the demo. The demo data is created again with recent dates, and anything added by hand is removed.

---

## 3. Running Automated Tests & Coverage

SafePaws enforces a strict **>= 70%** code coverage requirement across both backend and frontend.

### Backend Tests (JUnit 5, Mockito, Testcontainers)
Requires Docker running for PostgreSQL Testcontainers integration:
```bash
cd backend
./mvnw verify
```
- Executes slice tests (`@WebMvcTest`), unit tests with Mockito, and database tests with Testcontainers.
- Generates the JaCoCo coverage report under `backend/target/site/jacoco/index.html`.
- Fails the build if instruction coverage falls below 70%.

### Frontend Tests (Vitest, jsdom)
```bash
cd frontend
npm run test:coverage
```
- Executes unit specs with Vitest without requiring a browser.
- Fails the build if statements, branches, functions, or lines are under 70%.