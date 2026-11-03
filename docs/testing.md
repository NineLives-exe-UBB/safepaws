# Testing Strategy & Guidelines

This document outlines the testing strategy, quality standards, and structure for **SafePaws**.

---

## 1. Quality Standards
- **Minimum Code Coverage:** 70% required on all code contributions.
- **PR Requirement:** Pull Requests will only be merged if all test suites pass and total branch coverage meets or exceeds 70%.

---

## 2. Backend Testing (Spring Boot)
- **Frameworks:** JUnit 5, Mockito, Spring Boot Test.
- **Location:** `backend/src/test/java/.../safepaws/`.
- **Convention:** The test package and directory tree mirrors `backend/src/main/java/.../safepaws/`:
  - `controller/` — WebMvc slice tests (`@WebMvcTest`) validating status codes, JSON serialization, and input validation.
  - `service/` — Unit tests with mocked repositories verifying core business logic.
  - `repository/` — Data JPA slice tests (`@DataJpaTest`) with test database containers.

Run backend tests:
```bash
cd backend
./mvnw clean test
```

---

## 3. Frontend Testing (Angular)
- **Frameworks:** Jasmine & Karma.
- **Scope:** Unit tests for services, pipes, and component interaction logic.

Run frontend tests:
```bash
cd frontend
npm test
```
