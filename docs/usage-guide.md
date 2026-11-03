# Usage & Setup Guide

## 1. Prerequisites
- **Java:** JDK 17 or JDK 21
- **Node.js:** Node LTS and npm
- **Database:** PostgreSQL instance running locally or via Docker
- **Git**

## 2. Local Setup

### Database
Create a local PostgreSQL database:
```sql
CREATE DATABASE safepaws;
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
