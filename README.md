# SafePaws 🐾

Developed by **NineLives.exe** for the Collectiv Project course at Babeș-Bolyai University.

SafePaws is a collaborative web platform designed to help communities report, track, and reunite lost pets, as well as streamline pet adoptions.

---

## 🛠️ Tech Stack

- **Backend:** Java 17+ / 21, Spring Boot (REST API, Spring Data JPA, Spring Security)
- **Frontend:** Angular, TypeScript, HTML5 / SCSS
- **Database:** PostgreSQL
- **Testing & Quality:** JUnit 5, Mockito, Jasmine / Karma (Strict 70%+ code coverage requirement)
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
- [API Specification](./docs/api.md) — REST endpoints, authentication protocols, and error payload definitions.
- [Testing Strategy](./docs/testing.md) — Test suites, framework setup, and 70% coverage requirements.
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
