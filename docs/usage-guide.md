# Usage & Setup Guide

## 1. Prerequisites
- **Java:** JDK 21 (Required by `pom.xml` and CI)
- **Node.js:** Node 22 LTS and npm (>= 10 / 11)
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
   - **With Mock Data (Default dev mode):**
     ```bash
     npm start
     ```
     *(Uses local mock data service; useful when backend is not running).*
   - **With Live Backend Connection (Production config):**
     ```bash
     npm start -- --configuration production
     ```
     *(Connects directly to the Spring Boot REST API at `http://localhost:8080`).*

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
