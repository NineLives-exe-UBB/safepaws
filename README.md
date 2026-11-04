# SafePaws 🐾

Developed by **NineLives.exe** for the Collective Project course at Babeș-Bolyai University.

SafePaws is a collaborative web platform designed to help communities report, track, and reunite lost and found pets.

---

## 🛠️ Tech Stack

- **Backend:** Java 21, Spring Boot (REST API, Spring Data JPA; Spring Security planned for Milestone 2)
- **Frontend:** Angular, TypeScript, HTML5 / CSS
- **Database & Migrations:** PostgreSQL, Flyway, Docker Compose
- **Testing & Quality:** JUnit 5, Mockito, Testcontainers, Vitest (Strict 70%+ code coverage requirement)
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
- [Architecture Decisions (ADRs)](./docs/decisions.md) — Tech stack choices, testing thresholds, and architectural rationale.
- [System Architecture](./docs/architecture.md) — High-level architecture, module organization, and security structure.
- [Database Design](./docs/database.md) — Domain entities, relationships, and migration plans.
- [API Specification](./docs/api.md) — REST endpoints, request/response models, and standard error formats.
- [Testing & QA](./docs/testing.md) — Automated tests, coverage thresholds (70%+), manual test scenarios, and bug reporting.
- [Usage & Setup Guide](./docs/usage-guide.md) — Local environment setup and execution instructions.
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
