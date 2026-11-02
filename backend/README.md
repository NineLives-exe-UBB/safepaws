# Backend

This folder contains the backend of SafePaws, built with Java.

It handles the application logic, the API used by the frontend and the connection to the PostgreSQL database.

## How to run
## Prerequisites
* **Java 21**
* **Docker & Docker Compose** (for the PostgreSQL database)

## How to Run

### 1. Environment Variables
Copy the `.env.example` template to a new `.env` file in the **root** of the repository:
```bash
cp ../.env.example ../.env
```
Ensure the database variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) match your local setup.

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
Flyway will automatically run the SQL migrations on startup and seed the test announcements. The API runs on `http://localhost:8080`.

## Testing & Code Coverage
Tests run against a real PostgreSQL instance using Testcontainers. To run the test suite and verify the 70% JaCoCo coverage threshold:
```bash
# Windows
.\mvnw.cmd clean verify

# Mac/Linux
./mvnw clean verify
```
