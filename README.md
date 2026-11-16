# SafePaws 🐾

Developed by **NineLives.exe** for the Collective Project course at Babeș-Bolyai University.

SafePaws is a collaborative web platform designed to help communities report, track, and reunite lost and found pets.

---

## ✨ Features

- **Browse announcements:** lost and found pets as cards, with filters (type, species, status, color, breed, area) and distance from you ("Closest first", radius).
- **Map:** every announcement as a pin, red for lost and green for found.
- **Post an announcement:** details, photos and the place picked on the map.
- **Announcement page:** photo gallery, details, the contact with a tap-to-call number, the location on the map, and "Mark as resolved" when the pet is back home.
- **Printable poster:** a one-page PDF with the photo, the details and a QR code that opens the announcement online.
- **Accounts:** register and log in; reading is public, while posting an announcement and changing it need an account. *Planned, not on main yet:* only the owner can change an announcement.
- **Works on phones:** the pages adapt to small screens, and the app can be opened from a phone on the same Wi-Fi.

---

## 🛠️ Tech Stack

- **Backend:** Java 21, Spring Boot 4.1 (Spring Web, Spring Data JPA, Bean Validation, Spring Security with JWT login), SpringDoc OpenAPI (Swagger UI), OpenPDF (posters), ZXing (QR codes)
- **Frontend:** Angular 22, TypeScript, HTML, CSS, Leaflet with OpenStreetMap tiles (maps)
- **Database & Migrations:** PostgreSQL 17, Flyway
- **Docker:** Docker Compose runs the local PostgreSQL database; Testcontainers starts a PostgreSQL container for the backend tests
- **Testing & Quality:** JUnit 5, Mockito, Testcontainers, JaCoCo (backend); Vitest with jsdom (frontend). Both fail under 70% coverage. GitHub Actions runs the tests on every pull request.
- **Tooling:** IntelliJ IDEA (Backend), VS Code (Frontend), Git, Trello

---

## 🚀 How to Run

1. **Database:**
   ```bash
   cp .env.example .env
   docker compose up -d
   ```
   *(See [Database Guide](docs/database.md) for connection settings and troubleshooting port conflicts).*

2. **Backend:**
   ```bash
   cd backend
   ./mvnw clean spring-boot:run
   ```
   *(Flyway runs automatically to create tables and load seed data).*

3. **Frontend:**
   ```bash
   cd frontend
   npm install
   npm start
   ```
   *(Access the app at `http://localhost:4200`). For full setup details, opening the app from a phone, and resetting the data before a demo, see the [Usage & Setup Guide](docs/usage-guide.md).*

---

## 🔑 Demo accounts

Flyway seeds three demo accounts (`V6__users_and_owners.sql`). They exist only in your local database and are for demos and testing; never use this password for a real account.

| Email | Display name | Password |
|---|---|---|
| `ana@safepaws.dev` | Ana Pop | `SafePaws1!` |
| `mihai@safepaws.dev` | Mihai Rus | `SafePaws1!` |
| `elena@safepaws.dev` | Elena Moldovan | `SafePaws1!` |

The 22 demo announcements are shared between the three accounts (V6 sets `owner_id`); announcement 1 (Mitzi) belongs to Ana. Log in at `http://localhost:4200/login`, or use the ready-made requests in [`backend/auth.http`](backend/auth.http) (register, log in, me, create, edit and resolve an announcement). A `docker compose down -v` recreates the accounts and removes any account registered by hand.

---

## 📚 Project Documentation

All design records, specifications, and project management files are organized in the [`docs/`](./docs) directory:

- [Team Workflow & Guidelines](./docs/workflow.md) — Git branching, PR rules, commit conventions, and clean code standards.
- [Architecture Decisions (ADRs)](./docs/decisions.md) — Tech stack choices, testing thresholds, photo storage, search, maps, user accounts with JWT, posters, styling, and configuration.
- [System Architecture](./docs/architecture.md) — High-level architecture, data flows, module organization, and the CI pipeline.
- [Database Design](./docs/database.md) — Tables, relationships, indexes, and Flyway migrations.
- [API Specification](./docs/api.md) — REST endpoints, request/response models, and standard error formats.
- [Testing & QA](./docs/testing.md) — Automated tests, coverage thresholds (70%+), manual test scenarios, and bug reporting.
- [Usage & Setup Guide](./docs/usage-guide.md) — Using the app, local environment setup, and running the tests.
- [Meeting Notes](./docs/meetings.md) — Team standups and mentor review records.
- [Team Decisions](./docs/decisions/) — Each member's decisions and messages for the team.

---

## 👥 The Team (NineLives.exe)

| Name | Role |
|---|---|
| **Vanessza** | Team Lead |
| **Antonia** | Backend |
| **Patrick** | Backend |
| **Alexandru** | Backend |
| **Victor** | Frontend |
| **Luca** | Frontend |
| **Matei** | Databases |
| **Jessica** | Testing |
| **Tudor** | Documentation |