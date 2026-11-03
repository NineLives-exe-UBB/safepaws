# Database Design & Schema

## 1. Overview & Technology Stack
SafePaws uses **PostgreSQL 17**, containerized via Docker Compose. Schema migrations and seed data are managed automatically on backend startup using **Flyway** from `backend/src/main/resources/db/migration`.

### Migration Ledger
| Migration | Description |
|---|---|
| `V1__create_tables.sql` | Initializes the core `announcement` and `photo` tables with strict constraints and indexes. |
| `V2__test_data.sql` | Seeds 14 realistic test announcements in Cluj-Napoca (2 of which include attached photos). |

---

## 2. Entity-Relationship Model

Below is the conceptual and physical ER diagram:

![Database Schema](db_stuff/SafePaws.drawio.png)

### Relationships
- **Announcement to Photos (`1 : N`):** An announcement can have zero or multiple photos; each photo belongs to exactly one announcement.
- **Cascade Deletion:** `ON DELETE CASCADE` is enforced on `photo.announcement_id`. Deleting an announcement automatically deletes all related photo records.

---

## 3. Table Definitions

### Table: `announcement`
Stores lost and found pet reports.

| Column | Type | Required | Constraints / Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | Yes | `GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Unique identifier. |
| `type` | `VARCHAR(10)` | Yes | `CHECK (type IN ('LOST', 'FOUND'))` | Announcement category. |
| `species` | `VARCHAR(20)` | Yes | `CHECK (species IN ('CAT', 'DOG', 'OTHER'))` | Animal species. |
| `pet_name` | `VARCHAR(50)` | No | — | Name of the pet (if known). |
| `breed` | `VARCHAR(100)` | No | — | Pet breed. |
| `color` | `VARCHAR(100)` | No | — | Distinctive pet coloration. |
| `distinctive_marks` | `VARCHAR(500)` | No | — | Scars, collar details, markings. |
| `event_date` | `TIMESTAMP` | Yes | — | Date/time the pet was lost or found. |
| `latitude` | `DOUBLE PRECISION`| No | Range: `[-90, 90]` | Coordinate latitude (set together with longitude). |
| `longitude` | `DOUBLE PRECISION`| No | Range: `[-180, 180]` | Coordinate longitude (set together with latitude). |
| `area` | `VARCHAR(150)` | Yes | — | Neighborhood/area (e.g., "Mănăștur, Cluj-Napoca"). |
| `contact_name` | `VARCHAR(100)` | Yes | — | Name of contact person. |
| `contact_phone` | `VARCHAR(20)` | Yes | Stored as text | Phone number (preserves leading `0` / `+40`). |
| `status` | `VARCHAR(10)` | Yes | `CHECK (status IN ('ACTIVE', 'RESOLVED'))`, Default: `'ACTIVE'` | Current status of the report. |
| `created_at` | `TIMESTAMP` | Yes | Default: `CURRENT_TIMESTAMP` | Record creation timestamp. |

---

### Table: `photo`
Stores binary image data attached to announcements.

| Column | Type | Required | Constraints / Default | Description |
|---|---|---|---|---|
| `id` | `BIGINT` | Yes | `GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | Unique identifier. |
| `announcement_id`| `BIGINT` | Yes | `FOREIGN KEY` references `announcement(id)` `ON DELETE CASCADE` | Associated announcement ID. |
| `data` | `BYTEA` | Yes | — | Raw binary image payload. |
| `content_type` | `VARCHAR(50)` | Yes | `CHECK (content_type IN ('image/jpeg', 'image/png'))` | MIME type of the uploaded file. |
| `file_size` | `BIGINT` | Yes | `CHECK (file_size > 0)` (in bytes) | Total image size. |
| `original_file_name` | `VARCHAR(255)`| No | — | Original upload filename. |
| `is_main` | `BOOLEAN` | Yes | Default: `false` | Primary photo flag (at most one per announcement via unique index). |
| `uploaded_at` | `TIMESTAMP` | Yes | Default: `CURRENT_TIMESTAMP` | Upload timestamp. |

---

## 4. Architectural & Schema Decisions

- **Demo 1 Scope (No Users / Locations Tables):** Because authentication is deferred to Milestone 2, reporter contact information (`contact_name`, `contact_phone`) is stored directly on the announcement. Coordinates (`latitude`, `longitude`) and descriptive `area` are also embedded directly on the record. Dedicated user and location entities can be introduced in subsequent migrations.
- **Binary Image Storage (`BYTEA`):** Images are stored directly in PostgreSQL to ensure portable development staging, straightforward container resets, and self-contained database migrations. Listing endpoints (`GET /api/v1/announcements`) omit image bytes; individual photos are retrieved via `GET /api/v1/photos/{id}`.
- **Optional Coordinates:** Geographic coordinates are optional in Demo 1 as the initial submission form lacks an interactive map picker; `area` remains strictly mandatory. If coordinates are provided, both latitude and longitude must be present together (`ck_announcement_coordinates`).
- **Conventions & Immutability:** 
  - Database schema uses strict `snake_case` (`contact_phone`, `pet_name`), mapping directly to `camelCase` in Java DTOs/Entities (`contactPhone`, `petName`).
  - Fixed domain attributes (`type`, `species`, `status`) use uppercase `VARCHAR` with SQL `CHECK` constraints to map cleanly to Spring Boot Java Enums.
  - Merged Flyway migrations must never be altered. Any subsequent schema updates must be introduced via incremental scripts (`V3__...`).

---

## 5. How to Run & Connect to PostgreSQL

### Local Setup
1. **Environment Configuration:**
   Copy the environment template:
   ```bash
   cp .env.example .env
   ```
   > **Note:** `.env` is ignored by Git and must never be committed.

2. **Start Database Container:**
   ```bash
   docker compose up -d
   ```

3. **Health Check:**
   Confirm that the container is healthy:
   ```bash
   docker compose ps
   ```
   *(The `STATUS` column should indicate `healthy`).*

4. **Launch Backend:**
   Start the Spring Boot backend. Flyway will automatically execute migrations and load seed data on startup.

### Connection Credentials
For GUI clients (IntelliJ Database Tool, DBeaver, pgAdmin) or direct connections:
- **Host:** `localhost`
- **Port:** `5432` *(or `5433` if fallback `DB_PORT=5433` is configured)*
- **Database:** `safepaws`
- **User:** `safepaws`
- **Password:** Configured in your local `.env` file (`POSTGRES_PASSWORD`)

### Lifecycle Commands
| Task | Command | Description |
|---|---|---|
| **Stop Database (Retain Data)** | `docker compose down` | Stops containers while preserving database volume data. |
| **Reset Database (Wipe & Re-seed)** | `docker compose down -v` | Wipes the volume data; Flyway recreates tables and seeds data on the next backend boot. |
| **Port Conflict Resolution** | Set `DB_PORT=5433` in `.env` | Use when local PostgreSQL is already occupying port `5432`. |
