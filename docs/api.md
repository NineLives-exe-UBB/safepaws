# API Specification

## 1. Overview
SafePaws exposes a RESTful API communicating over HTTP with JSON payloads.

- **Base URL (Local):** `http://localhost:8080/api/v1`
- **Content Type:** `application/json`

## 2. Authentication & Headers
Protected endpoints require a Bearer token in the request header:
```
Authorization: Bearer <jwt-token>
```

## 3. Standard Error Structure
All failed responses adhere to the following payload format:
```json
{
  "timestamp": "2026-11-03T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error explanation",
  "path": "/api/v1/resource"
}
```

## 4. Endpoint Modules
- `/api/v1/auth`: Registration, login, and token refresh.
- `/api/v1/users`: User profile queries and updates.
- `/api/v1/pets`: Pet profile creation, update, and search.
- `/api/v1/announcements`: Lost/found listings, status updates, and search filters.
