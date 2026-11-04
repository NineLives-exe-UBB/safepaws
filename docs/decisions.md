# Architecture Decision Log (ADR)

This document records the architectural, technological, and procedural decisions made for SafePaws.

---

## ADR 001: Backend Architecture — Java & Spring Boot
- **Status:** Accepted
- **Context:** The application needs a robust, scalable backend framework with strong typing, dependency injection, and native relational database support.
- **Decision:** Use Java (LTS) with Spring Boot (REST API, Spring Data JPA; Spring Security planned for Milestone 2).
- **Consequences:** Reliable enterprise ecosystem with mature tooling; requires strict modular layering (Controller, Service, Repository).

---

## ADR 002: Frontend Architecture — Angular
- **Status:** Accepted
- **Context:** SafePaws requires an opinionated Single Page Application (SPA) architecture with modular organization and strong TypeScript integration.
- **Decision:** Use Angular as the client-side framework.
- **Consequences:** Provides standardized routing, reactive forms, and HTTP services; enforces consistent TypeScript typing across all components.

---

## ADR 003: Database Engine — PostgreSQL
- **Status:** Accepted
- **Context:** SafePaws stores structured relational entities (announcements with embedded location attributes, and photo attachments) requiring ACID compliance and schema enforcement.
- **Decision:** Use PostgreSQL as the relational database engine.
- **Consequences:** Solid transaction guarantees, performant indexing, and native compatibility with Spring Data JPA and Hibernate.

---

## ADR 004: Development Tooling & IDE Standardization
- **Status:** Accepted
- **Context:** Ensure team consistency and avoid tooling discrepancies across different operating systems.
- **Decision:**
  - Backend: IntelliJ IDEA
  - Frontend: Visual Studio Code
- **Consequences:** Standardized formatting configurations (`Ctrl+Alt+L` / `Shift+Alt+F`) and shared run profiles.

---

## ADR 005: Quality Assurance — Minimum 70% Test Coverage
- **Status:** Accepted
- **Context:** Ensure code reliability and prevent regressions across sprint deliverables.
- **Decision:** Enforce a minimum 70% automated test coverage requirement on PRs (JUnit 5 and Mockito for backend, Vitest for frontend).
- **Consequences:** PRs that lower test coverage below the threshold will not be merged.

---

## ADR 006: Version Control & Project Management Protocol
- **Status:** Accepted
- **Context:** Coordinate distributed contributions cleanly between Trello and GitHub.
- **Decision:** Link Trello cards to GitHub feature branches (`feature/`, `fix/`, `docs/`, `test/`) with a strict PR review process requiring at least 1 peer approval.
- **Consequences:** Clean Git history, isolated changes, and traceable feature delivery.
