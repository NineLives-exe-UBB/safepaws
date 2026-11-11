# Architecture Decision Log (ADR)

This document records the architectural, technological, and procedural decisions made for SafePaws.

---

## ADR 001: Backend Architecture — Java & Spring Boot
- **Status:** Accepted
- **Context:** The application needs a robust, scalable backend framework with strong typing, dependency injection, and native relational database support.
- **Decision:** Use Java 21 (LTS) with Spring Boot 4.1 (Spring Web for the REST API, Spring Data JPA, Bean Validation).
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
- **Decision:** Use PostgreSQL 17 as the relational database engine, with the schema managed by Flyway migrations (`backend/src/main/resources/db/migration`). The schema has two tables:
  - `announcement` — one lost or found report. The location is stored as columns on this table: `area` (required text, "Neighborhood, City") and the optional `latitude` / `longitude` pair, which must be set together. The reporter's contact details (`contact_name`, `contact_phone`) are also columns here; there is no users table.
  - `photo` — the images of an announcement (`announcement_id` foreign key with `ON DELETE CASCADE`), at most one with `is_main = true` per announcement. See ADR 007.
- **Consequences:** Solid transaction guarantees, performant indexing, and native compatibility with Spring Data JPA and Hibernate. Users or separate locations would need new tables in a new migration.

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
- **Decision:** Enforce a minimum 70% automated test coverage requirement on PRs (JUnit 5, Mockito and Testcontainers with JaCoCo for the backend, Vitest for the frontend), checked by GitHub Actions on every pull request.
- **Consequences:** PRs that lower test coverage below the threshold will not be merged.

---

## ADR 006: Version Control & Project Management Protocol
- **Status:** Accepted
- **Context:** Coordinate distributed contributions cleanly between Trello and GitHub.
- **Decision:** Link Trello cards to GitHub feature branches (`feature/`, `fix/`, `docs/`, `test/`, `chore/`) with a strict PR review process requiring at least 1 peer approval.
- **Consequences:** Clean Git history, isolated changes, and traceable feature delivery.

---

## ADR 007: Photo Storage — Images in the Database
- **Status:** Accepted
- **Context:** Announcements need one or more photos, which have to be uploaded, shown on the cards and map popups, and printed on the poster.
- **Decision:** Store the image bytes in PostgreSQL, in the `photo.data` column (`BYTEA`), not on disk or in an external storage service.
  - Only `image/jpeg` and `image/png` are accepted (database `CHECK` constraint, plus a magic-byte check in `PhotoService`), max 5 MB per file and 25 MB per request (`application.properties`).
  - The first photo uploaded to an announcement without a main photo becomes the main one; a partial unique index (`uq_photo_main_per_announcement`) allows at most one main photo per announcement.
  - Lists never load the bytes: `PhotoRepository.findMetadataByAnnouncementIds` selects only the metadata (`PhotoMetadata`) for all listed announcements in one query, and responses link to each image as `/photos/{id}`. `GET /photos/{id}` returns the bytes with the stored content type.
  - Demo photos are loaded by the Java Flyway migration `V4__Seed_demo_photos.java` from `backend/src/main/resources/db/demo-photos/`.
- **Rationale:** Per `database.md`: storing images in the database keeps the development setup portable, makes a container reset (`docker compose down -v`) reset the photos too, and keeps migrations self-contained. **Rationale: TODO** — confirm with the team whether there were other reasons.
- **Consequences:** No file storage to configure and no files that can get out of sync with the rows; deleting an announcement deletes its photos (`ON DELETE CASCADE`). The database grows with every image, and every image request goes through the backend and the database.

---

## ADR 008: Search — Case- and Diacritic-Insensitive Text Filters
- **Status:** Accepted
- **Context:** `GET /announcements` filters by `color`, `breed` and `area` with free text. Romanian names are often typed without diacritics (`Manastur` for `Mănăștur`).
- **Decision:** Match the text filters as a "contains" that ignores case and diacritics, in the database:
  - `V3__search_support.sql` enables PostgreSQL's `unaccent` extension.
  - `AnnouncementSpecifications.contains` builds `unaccent(lower(column)) LIKE unaccent(lower('%text%'))` for each text filter that was sent. `%`, `_` and `\` typed by the user are escaped, so they match literally.
  - Blank text counts as not sent and spaces at the ends are removed (`AnnouncementFilter`). Filters combine with AND.
  - Only case and accents are ignored, so different spellings stay different: `grey` finds `Grey` and `dark grey`, but not `Gray`.
- **Rationale:** From `V3__search_support.sql`: the search should ignore diacritics, so "Manastur" also finds "Mănăștur". **Rationale: TODO** — add why this was done in PostgreSQL (`unaccent`) rather than another way, if there was a reason.
- **Consequences:** Users find announcements however they type the diacritics, including the older cedilla letters (`ş`, `ţ`), which `SearchSupportMigrationTest` checks. `unaccent` is `STABLE`, not `IMMUTABLE`, so it cannot be used directly in an index and the text filters scan the table (acceptable at the current data size, per `decisions/matei-decisions.md`). Different spellings of a word (`grey` / `gray`) are not treated as equal.

---

## ADR 009: Maps — Leaflet with OpenStreetMap
- **Status:** Accepted
- **Context:** The form needs a map to pick where a pet was lost or found, and the `/map` page shows a pin for each announcement.
- **Decision:** Use Leaflet (1.9.4) with OpenStreetMap tiles, wrapped in one reusable component, `<app-map>` (`frontend/src/app/components/map`).
- **Rationale:** From `decisions/luca-decisions.md`: it is free and needs no API key, so there is no key to hide or to pay for (chosen over Google Maps).
- **Consequences:** No account or billing setup. The map page is lazy-loaded so Leaflet is not part of the initial bundle, and Leaflet's marker images are copied by `angular.json` so the default pins show in the Angular build.
