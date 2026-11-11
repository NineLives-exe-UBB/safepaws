# Usage & Setup Guide

This guide has two parts: how to use the app (section 0), and how to set it up and test it locally (sections 1 to 3).

## 0. Using SafePaws

Open `http://localhost:4200` (see section 2 to start it). The header has three links: **Announcements**, **Map** and **Post announcement**. Anyone can use every page; there are no accounts.

### Browse announcements (`/announcements`)
- Each card shows the main photo (or `No photo`), a LOST or FOUND label, the species, the pet name when there is one, the area and the date.
- By default you see only active announcements, newest event first.
- **Download poster** on a card downloads a printable PDF poster for that announcement.

### Filter and sort
The filter bar above the cards narrows the list:
- **Type**, **Species** and **Status** dropdowns. Status is either Active (the default) or Resolved.
- **Color**, **Breed** and **Area** text fields. They match part of the text and ignore upper/lower case and diacritics, so `manastur` finds `Mănăștur`. The list updates shortly after you stop typing.
- **Radius** (within 1, 5, 10, 25, 50 or 100 km) and **Sort by** (Newest first, Oldest first, Closest first). For a radius or Closest first, the browser asks for your location, and each card then shows how far away it is (`1.2 km away`). If you refuse the location, these two options are disabled.
- The filters are kept in the address, so you can reload the page or share a filtered list. **Clear filters** appears when a filter is active and removes them. If nothing matches, the page says `No announcements match your filters`.

### Map (`/map`)
- A pin for each announcement that has a location: red for lost, green for found.
- If you allow your location, the map starts around you; otherwise it zooms so that all the pins fit.
- Click a pin to see the photo, the name (or species), the area and the date.
- The filter bar works like the one on the list, and the same filters give the same announcements. Announcements without a location are counted in a note above the map.

### Post an announcement (`/announcements/new`)
1. Choose **Lost** or **Found**, and the **Species** (required).
2. Optionally add the pet name, breed, color and distinctive marks.
3. Enter the **date and time** (not in the future) and the **area**, then click the map to **pinpoint the location** (all required). If you allow your location, the map starts around you.
4. Enter **your name** and **phone number** (7–15 digits, optionally starting with `+`).
5. Optionally choose photos (JPG or PNG, max 5 MB each). The first one becomes the main photo; **Remove** takes a photo out.
6. Click **Submit announcement**. After the success message you are taken back to the list. If the announcement was created but the photos failed, fix or remove the photos and submit again: only the photos are sent again.

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

2. The application configuration (`src/main/resources/application.properties`) automatically imports database credentials from the root `.env` file (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`). Optional environment variables:
   - `CORS_ALLOWED_ORIGINS`: Comma-separated list of allowed frontend origins (defaults to `http://localhost:4200`).

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(Flyway runs automatically on startup to validate the schema and seed initial test data).*

4. **API & Documentation Endpoints:**
   - Base API URL: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - OpenAPI 3 Spec (JSON): `http://localhost:8080/v3/api-docs`

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
   *(Connects directly to the Spring Boot REST API at `http://localhost:8080`; requires the backend to be running).*

4. Access the web interface at `http://localhost:4200`.

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
