# SafePaws 🐾

Developed by **NineLives.exe** for the Collective Project course at Babeș-Bolyai University.

SafePaws is a collaborative web platform designed to help communities report, track, and reunite lost and found pets.

---

## 🛠️ Tech Stack

- **Backend:** Java 21, Spring Boot 4.1 (Spring Web, Spring Data JPA, Bean Validation), SpringDoc OpenAPI (Swagger UI), OpenPDF (posters)
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
   *(Access the app at `http://localhost:4200`). For full setup details, see the [Usage & Setup Guide](docs/usage-guide.md).*

---

## 📚 Project Documentation

All design records, specifications, and project management files are organized in the [`docs/`](./docs) directory:

- [Team Workflow & Guidelines](./docs/workflow.md) — Git branching, PR rules, commit conventions, and clean code standards.
- [Architecture Decisions (ADRs)](./docs/decisions.md) — Tech stack choices, testing thresholds, photo storage, search, and maps.
- [System Architecture](./docs/architecture.md) — High-level architecture, data flows, module organization, and the CI pipeline.
- [Database Design](./docs/database.md) — Tables, relationships, indexes, and Flyway migrations.
- [API Specification](./docs/api.md) — REST endpoints, request/response models, and standard error formats.
- [Testing & QA](./docs/testing.md) — Automated tests, coverage thresholds (70%+), manual test scenarios, and bug reporting.
- [Usage & Setup Guide](./docs/usage-guide.md) — Using the app, local environment setup, and running the tests.
- [Meeting Notes](./docs/meetings.md) — Team standups and mentor review records.

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
