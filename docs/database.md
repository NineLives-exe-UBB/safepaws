# Database Design & Schema

## 1. Database Engine
- **Engine:** PostgreSQL
- **ORM:** Hibernate / Spring Data JPA

## 2. Entity-Relationship Overview
Detailed ERD and table mappings will be populated as feature schemas are finalized.

### Core Domain Entities
- **Users:** Account credentials, roles, contact details, and preferences.
- **Pets:** Pet profiles including species, breed, distinctive marks, and image URLs.
- **Announcements:** Lost, found, and adoption listings associated with pets and owners.
- **Locations:** Coordinates and address data linked to announcements for geographic mapping.

## 3. Schema Migrations & Seeding
- Version-controlled schema migrations are managed under `backend/src/main/resources/db/migration`.
- Test seed data scripts must be maintained for local setup and integration testing.
