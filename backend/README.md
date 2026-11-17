# Backend

This folder contains the backend of SafePaws, built with Java.

It handles the application logic, the API used by the frontend, the printable PDF posters with a QR code, and the connection to the PostgreSQL database.

## Prerequisites
* **Java 21**
* **Docker & Docker Compose** (for the PostgreSQL database)

## How to Run

### 1. Environment Variables
Copy the `.env.example` template to a new `.env` file in the **root** of the repository:
```bash
cp ../.env.example ../.env
```
Ensure the database variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) match your local setup. The backend reads `.env` from `backend/` or from the repository root.

**`JWT_SECRET` (required):** the secret that signs the login tokens (HS256). `.env.example` contains a placeholder, so a fresh `cp .env.example .env` starts. If you created your `.env` before the login was added, add the line yourself, otherwise the backend does not start (the placeholder `${JWT_SECRET}` cannot be resolved, and a blank value fails with `JWT_SECRET must not be blank`):
```bash
JWT_SECRET=change-me-to-a-random-256-bit-secret-at-least-32-chars
```
For your own machine, replace it with a random value of at least 32 characters, for example the output of `openssl rand -base64 48`. Shorter values still work (they are hashed with SHA-256 first), but use at least 32. Never commit a real secret: it only goes in `.env`. Changing it logs everyone out, because existing tokens no longer verify.

Two optional variables, both with the default `http://localhost:4200`:

| Variable | What it is for |
|---|---|
| `CORS_ALLOWED_ORIGINS` | The frontend addresses allowed to call the API from a browser, comma-separated. |
| `FRONTEND_URL` | The frontend address in the QR code printed on the poster (`FRONTEND_URL/announcements/{id}`). |

To open the app from a phone on the same Wi-Fi, put the laptop IP in both, for example `FRONTEND_URL=http://192.168.1.23:4200` and `CORS_ALLOWED_ORIGINS=http://localhost:4200,http://192.168.1.23:4200`, then restart the backend. The IP only goes in `.env`, never in `.env.example`. The full steps are in `docs/usage-guide.md`.

### 2. Start the Database
From the repository root, start the PostgreSQL container:
```bash
docker compose up -d
```

### 3. Run the Spring Boot Server
Navigate to the `backend/` directory and start the application using the Maven wrapper:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Mac/Linux
./mvnw spring-boot:run
```
Flyway will automatically run the migrations on startup (SQL in `src/main/resources/db/migration`, Java in `src/main/java/db/migration`) and seed the test announcements and demo photos. The API runs on `http://localhost:8080`, with the interactive documentation at `http://localhost:8080/swagger-ui.html` (public, no login needed).

> **Known issue:** Swagger UI and `/v3/api-docs` currently return `401`, because `SecurityConfig` does not permit `/swagger-ui/**` and `/v3/api-docs/**`. Until that is fixed, use [`auth.http`](auth.http) or curl.

To start again from the demo data (and get recent demo dates before a demo), run `docker compose down -v`, then `docker compose up -d` and start the backend again.

## API

| Method | Endpoint | What it does |
|---|---|---|
| `POST` | `/auth/register` | Creates an account (public) |
| `POST` | `/auth/login` | Logs in and returns a JWT valid for 24 hours (public) |
| `GET` | `/auth/me` | The logged-in user (token) |
| `POST` | `/announcements` | Creates an announcement (token) |
| `GET` | `/announcements` | Lists announcements, with optional filters, distance and sorting |
| `GET` | `/announcements/{id}` | One announcement with all its details |
| `PUT` | `/announcements/{id}` | Edits an announcement (token) |
| `PATCH` | `/announcements/{id}/status` | Marks an announcement as `RESOLVED` or `ACTIVE` (token) |
| `POST` | `/announcements/{id}/photos` | Uploads photos, JPG or PNG, max 5 MB each (token) |
| `GET` | `/photos/{id}` | One photo |
| `GET` | `/announcements/{id}/poster` | The printable A4 PDF poster, with a QR code to the announcement |

"(token)" means the request needs `Authorization: Bearer <token>`, from `POST /auth/login`; without it the answer is `401`. Everything else is public. *Planned, not on main yet (owner check, Patrick and Antonia):* only the owner may edit an announcement, change its status or add photos; another user gets `403`.

The requests, responses and error messages are in `docs/api.md`. Ready-made requests with the demo accounts are in [`auth.http`](auth.http) (IntelliJ HTTP Client).

## Project structure

```
src/main/java/com/ninelives/safepaws/
├── controller/   REST endpoints
├── service/      business logic, the PDF poster (OpenPDF) and the QR code (ZXing)
├── repository/   database access and the list filters
├── model/        JPA entities and enums
├── dto/          request and response records
├── exception/    error handling (one JSON format for every error)
├── validation/   custom validation (password length in UTF-8 bytes)
└── config/       CORS, Spring Security and the JWT filter, BCrypt password encoder
src/main/resources/
├── application.properties
├── db/migration/     SQL migrations (V1, V2, V3, V5, V6, V7)
├── db/demo-photos/   demo images loaded by V4
└── fonts/            DejaVu Sans and DejaVu Sans Bold, for the Romanian letters on the poster
```

The next migration is `V6`. Merged migrations are never edited; a fix goes in a new migration. SQL and Java migrations share version numbers, so check both folders before choosing one.

## Testing & Code Coverage
The repository and migration tests run against a real PostgreSQL instance started by Testcontainers, so Docker must be running. To run the test suite and verify the 70% JaCoCo coverage threshold:
```bash
# Windows
.\mvnw.cmd clean verify

# Mac/Linux
./mvnw clean verify
```

If the repository and migration tests fail with `Failed to load ApplicationContext`, Docker Desktop is not running. The coverage report is in `target/site/jacoco/index.html`.