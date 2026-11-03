# System Architecture

## 1. System Overview
SafePaws is a collaborative web-based platform designed to assist pet owners and communities in tracking, reporting, and reuniting lost pets, as well as managing adoption announcements.

---

## 2. High-Level Architecture
The system follows a decoupled client-server multi-tier architecture:
- **Client Tier:** Angular Single Page Application (SPA) communicating via RESTful JSON over HTTP/HTTPS.
- **Application Tier:** Spring Boot REST API handling business logic, validation, and transactions.
- **Persistence Tier:** PostgreSQL database managed via Spring Data JPA, Hibernate, and Flyway.

---

## 3. Component Communication & Tier Architecture

The diagram below illustrates the end-to-end communication flow across all system tiers:

```mermaid
flowchart TD
    subgraph ClientTier["Client Tier (Browser)"]
        UI["Angular SPA\n(Components, Services, Reactive Forms)"]
    end

    subgraph AppTier["Application Tier (Spring Boot 3)"]
        Controller["REST Controllers\n(Request Validation & HTTP Mapping)"]
        Service["Service Layer\n(Domain Business Logic & Transactions)"]
        Repo["Data Access Layer\n(Spring Data JPA / Hibernate Repositories)"]
        Hikari["HikariCP\n(JDBC Connection Pool)"]
    end

    subgraph DataTier["Persistence Tier (Docker Container)"]
        PG[("PostgreSQL 17 Database\n(announcement & photo tables)")]
    end

    UI -->|"HTTP / REST (JSON & Multipart)"| Controller
    Controller -->|"DTOs"| Service
    Service -->|"Entities"| Repo
    Repo -->|"SQL Operations"| Hikari
    Hikari -->|"TCP / Port 5432"| PG
```

---

## 4. End-to-End Data Flow Scenarios

### Scenario 1: Listing Announcements
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Angular as Angular Client
    participant Controller as AnnouncementController
    participant Service as AnnouncementService
    participant Repo as AnnouncementRepository
    participant DB as PostgreSQL 17

    User->>Angular: Navigates to Announcement Feed
    Angular->>Controller: GET /api/v1/announcements
    Controller->>Service: findAll()
    Service->>Repo: findAllProjected()
    Repo->>DB: SELECT id, type, species, pet_name, area, ... FROM announcement
    DB-->>Repo: Result rows (lightweight projection excluding BYTEA)
    Repo-->>Service: Announcement List
    Service-->>Controller: List<AnnouncementResponseDto>
    Controller-->>Angular: 200 OK (JSON array without image bytes)
    Angular-->>User: Renders announcement cards with metadata & cover photo URLs
```

*Key Characteristic:* Heavy raw image bytes (`BYTEA`) are explicitly omitted during list queries. Images are only loaded individually on demand via `/photos/{id}`.

---

### Scenario 2: Uploading Announcement & Attached Photos
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Angular as Angular Client
    participant Controller as AnnouncementController
    participant PhotoController as PhotoController
    participant Service as Domain Services
    participant DB as PostgreSQL 17

    User->>Angular: Fills announcement form & selects photos
    Angular->>Controller: POST /api/v1/announcements (JSON payload)
    Controller->>Service: createAnnouncement(dto)
    Service->>DB: INSERT INTO announcement (...) VALUES (...)
    DB-->>Service: Saved announcement record
    Service-->>Controller: Created entity (id=15)
    Controller-->>Angular: 201 Created {"id": 15, ...}

    opt When photos are attached
        Angular->>PhotoController: POST /api/v1/announcements/15/photos (multipart/form-data)
        PhotoController->>Service: uploadPhotos(announcementId=15, files)
        Service->>DB: INSERT INTO photo (announcement_id, data, content_type, ...) VALUES (...)
        DB-->>Service: Saved photo records
        Service-->>PhotoController: List<PhotoMetadataDto>
        PhotoController-->>Angular: 201 Created [{"id": 3, "url": "/photos/3", "isMain": true}]
    end

    Angular-->>User: Displays confirmation and navigates to announcement view
```

---

## 5. Repository & Module Structure

The project structure cleanly separates client, server, database migrations, and documentation:

```text
safepaws/
├── backend/                              # Spring Boot (Java)
│   ├── src/main/java/.../safepaws/
│   │   ├── controller/                   # REST Endpoints
│   │   ├── service/                      # Business & application logic
│   │   ├── repository/                   # Spring Data JPA database access
│   │   ├── model/                        # JPA Entities (Announcement, Photo)
│   │   ├── dto/                          # API Request/Response Transfer Objects
│   │   ├── exception/                    # Global exception handling & custom errors
│   │   └── config/                       # Configuration beans (Swagger, CORS, etc.)
│   ├── src/main/resources/
│   │   ├── application.properties        # Application configuration settings
│   │   └── db/migration/                 # Flyway SQL versioned migrations
│   └── src/test/                         # Tests mirroring src/main structure
├── frontend/                             # Angular SPA
│   └── src/
│       ├── app/
│       │   ├── pages/                    # Views/pages (List, Form)
│       │   ├── components/               # Reusable UI elements (Announcement Card, etc.)
│       │   ├── services/                 # HTTP services calling the backend
│       │   ├── models/                   # TypeScript interfaces (Announcement, Photo)
│       │   └── shared/                   # Shared components, directives, pipes
│       └── environments/                 # Environment configuration (Backend API URLs)
├── docs/                                 # Project documentation
├── docker-compose.yml                    # Starts PostgreSQL 17 database
└── README.md                             # Project overview
```

---

## 6. Security & Authentication
- **Milestone 1 (Demo 1):** Unauthenticated open submission with contact details stored directly on announcement records.
- **Milestone 2 (Target):** Stateless token-based authentication using Spring Security and JWT; role-based authorization for administrative vs. regular user capabilities.
