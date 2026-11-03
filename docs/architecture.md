# System Architecture

## 1. System Overview
SafePaws is a web-based platform designed to assist pet owners and communities in tracking, reporting, and reuniting lost pets, as well as managing adoption announcements.

## 2. High-Level Architecture
The system follows a decoupled client-server architecture:
- **Client Tier:** Angular Single Page Application (SPA) communicating via RESTful JSON over HTTP/HTTPS.
- **Application Tier:** Spring Boot REST API handling business logic, validation, and authentication.
- **Persistence Tier:** PostgreSQL database managed via Spring Data JPA and Hibernate.

## 3. Backend Module Structure (Spring Boot)
- `controller/`: REST API endpoints and HTTP request/response mapping.
- `service/`: Core business logic and transaction management.
- `repository/`: Spring Data JPA data access interfaces.
- `model/` / `entity/`: JPA database entities.
- `dto/`: Request and response data transfer objects.
- `config/`: Security, CORS, and infrastructure configuration beans.

## 4. Frontend Module Structure (Angular)
- `core/`: Singleton services, authentication guards, and HTTP interceptors.
- `shared/`: Reusable UI components, custom pipes, and utility directives.
- `features/`: Domain-specific feature modules (e.g., Announcements, Pet Profiles, Map, User Profile).

## 5. Security & Authentication
- Stateless token-based authentication using Spring Security and JWT.
- Role-based authorization for administrative vs. regular user capabilities.
