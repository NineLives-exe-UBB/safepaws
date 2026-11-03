# Usage & Setup Guide

## 1. Prerequisites
- **Java:** JDK 17 or JDK 21
- **Node.js:** Node LTS and npm
- **Database:** PostgreSQL instance running locally or via Docker
- **Git**

## 2. Local Setup

### Database (Docker)
PostgreSQL 17 runs containerized via Docker Compose.

1. Create your local environment file from the template:
   ```bash
   cp .env.example .env
   ```
   Note: `.env` is ignored by Git and must never be committed.
2. Start the PostgreSQL container:
   ```bash
   docker compose up -d
   ```
3. Confirm that the container is healthy:
   ```bash
   docker compose ps
   ```
   (The STATUS column should indicate healthy).
4. Launch the Spring Boot backend. Flyway will automatically execute migrations and load seed data on the first run.

#### Troubleshooting & Commands
- **Port Conflict (Port 5432 in use):** If a local PostgreSQL instance is already running on port 5432, update `DB_PORT=5433` in your `.env` file before starting the container.
- **Stop Database (Retain Data):**
  ```bash
  docker compose down
  ```
- **Full Database Reset (Wipe Data & Re-run Migrations):**
  ```bash
  docker compose down -v
  ```

### Backend (Spring Boot)
1. Navigate to the backend directory.
2. Verify connection properties in `application.yml` or set environment variables:
   - `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/safepaws`
   - `SPRING_DATASOURCE_USERNAME=<your_username>`
   - `SPRING_DATASOURCE_PASSWORD=<your_password>`
3. Run the application:
   ```bash
   ./mvnw clean spring-boot:run
   ```

### Frontend (Angular)
1. Navigate to the frontend directory.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the local development server:
   ```bash
   npm start
   ```
4. Access the web interface at `http://localhost:4200`.

## 3. Running Automated Tests
- **Backend:** `./mvnw test` (enforcing >= 70% coverage requirement).
- **Frontend:** `npm test`
