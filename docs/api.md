# REST API Specification & Contract

## 1. Overview & Assignments

SafePaws exposes a RESTful web API for lost and found pets, photo attachments, and printable PDF posters.

### Endpoint Overview & Team Assignments

| Method | Endpoint | Description | Implemented by | Used by |
|---|---|---|---|---|
| `POST` | `/announcements` | Creates a new announcement | Antonia | Victor (form) |
| `GET` | `/announcements` | Returns all announcements (newest first) | Antonia | Luca (list) |
| `GET` | `/announcements/{id}` | Returns one announcement with attached photos | Antonia | Luca |
| `POST` | `/announcements/{id}/photos` | Uploads photos to an announcement | Patrick | Victor (upload) |
| `GET` | `/photos/{id}` | Returns raw image (JPG/PNG) | Patrick | Luca |
| `GET` | `/announcements/{id}/poster` | Downloads announcement poster as PDF | Alexandru | Luca |

---

## 2. General Conventions
- **Base URL:** `http://localhost:8080/api/v1` (endpoints documented below are relative to this base).
- **Field Names:** All JSON identifiers are in English, using `camelCase` (e.g., `contactPhone`, `distinctiveMarks`, `eventDate`).
- **Date-Time Format:** ISO 8601 string format: `YYYY-MM-DDTHH:mm:ss` (e.g., `2026-11-01T14:30:00`).
- **Fixed Values (Enums):** Fixed domain attributes are strictly uppercase strings:
  - Announcement Type: `LOST`, `FOUND`
  - Status: `ACTIVE`, `RESOLVED`
  - Species: `CAT`, `DOG`, `OTHER`
- **Privacy Notice:** `contactPhone` is temporarily exposed in read endpoints to enable public contact and poster generation during Demo 1. It will be restricted once in-app messaging and user authentication are added.

---

## 3. Endpoints & Payload Details

### 3.1 Announcements

#### `POST /announcements`
Creates a new lost or found announcement.

##### Request Body (`application/json`)
```json
{
  "type": "LOST",
  "species": "Cat",
  "petName": "Misi",
  "breed": "European",
  "color": "Grey with white paws",
  "distinctiveMarks": "Red collar, small scar on the left ear",
  "eventDate": "2026-10-30T18:00:00",
  "latitude": 46.7712,
  "longitude": 23.6236,
  "area": "Mănăștur, Cluj-Napoca",
  "contactName": "Ana Pop",
  "contactPhone": "0712345678"
}
```

##### Response (`201 Created`)
```json
{
  "id": 15,
  "type": "LOST",
  "species": "Cat",
  "petName": "Misi",
  "breed": "European",
  "color": "Grey with white paws",
  "distinctiveMarks": "Red collar, small scar on the left ear",
  "eventDate": "2026-10-30T18:00:00",
  "latitude": 46.7712,
  "longitude": 23.6236,
  "area": "Mănăștur, Cluj-Napoca",
  "contactName": "Ana Pop",
  "contactPhone": "0712345678",
  "status": "ACTIVE",
  "createdAt": "2026-11-01T14:30:00",
  "photos": []
}
```

---

#### `GET /announcements`
Returns all announcements in descending order by creation date (newest first). Excludes heavy raw binary image bytes, referencing photo metadata and URLs.

##### Response (`200 OK`)
```json
[
  {
    "id": 15,
    "type": "LOST",
    "species": "Cat",
    "petName": "Misi",
    "breed": "European",
    "color": "Grey with white paws",
    "distinctiveMarks": "Red collar, small scar on the left ear",
    "eventDate": "2026-10-30T18:00:00",
    "latitude": 46.7712,
    "longitude": 23.6236,
    "area": "Mănăștur, Cluj-Napoca",
    "contactName": "Ana Pop",
    "contactPhone": "0712345678",
    "status": "ACTIVE",
    "createdAt": "2026-11-01T14:30:00",
    "photos": [
      {
        "id": 3,
        "url": "/photos/3",
        "isMain": true
      }
    ]
  }
]
```

---

#### `GET /announcements/{id}`
Returns a single announcement by its unique identifier, including all associated photos.

##### Response (`200 OK`)
```json
{
  "id": 15,
  "type": "LOST",
  "species": "Cat",
  "petName": "Misi",
  "breed": "European",
  "color": "Grey with white paws",
  "distinctiveMarks": "Red collar, small scar on the left ear",
  "eventDate": "2026-10-30T18:00:00",
  "latitude": 46.7712,
  "longitude": 23.6236,
  "area": "Mănăștur, Cluj-Napoca",
  "contactName": "Ana Pop",
  "contactPhone": "0712345678",
  "status": "ACTIVE",
  "createdAt": "2026-11-01T14:30:00",
  "photos": [
    {
      "id": 3,
      "url": "/photos/3",
      "isMain": true
    },
    {
      "id": 4,
      "url": "/photos/4",
      "isMain": false
    }
  ]
}
```

---

### 3.2 Photos & Media

#### `POST /announcements/{id}/photos`
Uploads one or more photos and attaches them to the specified announcement.

##### Request Format
- **Content-Type:** `multipart/form-data`
- **Form Field:** `files` (one or more binary image files, JPG/PNG, max 5 MB per file)

##### Response (`201 Created`)
```json
[
  {
    "id": 3,
    "url": "/photos/3",
    "isMain": true
  },
  {
    "id": 4,
    "url": "/photos/4",
    "isMain": false
  }
]
```

---

#### `GET /photos/{id}`
Retrieves raw image bytes stored in the database (`BYTEA`).

##### Response (`200 OK`)
- **Headers:** `Content-Type: image/jpeg` or `image/png`
- **Body:** Raw binary stream. Can be used directly in HTML templates: `<img src="/api/v1/photos/{id}">`.

---

### 3.3 Poster Generation

#### `GET /announcements/{id}/poster`
Generates and downloads a printable missing/found pet poster as a PDF document.

##### Response (`200 OK`)
- **Headers:** `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="poster_{id}.pdf"`
- **Body:** Binary PDF byte stream downloaded directly by the browser.

---

## 4. Standard Error Response Format

All API errors return a consistent JSON structure so the client can display clear, standardized error messages:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Some fields are invalid",
  "fieldErrors": [
    {
      "field": "species",
      "message": "Species is required"
    }
  ],
  "timestamp": "2026-11-01T14:30:00"
}
```

### Error Schema Fields
| Field | Type | Description |
|---|---|---|
| `status` | `number` | HTTP status code (e.g., `400`, `404`, `500`). |
| `error` | `string` | Short error name (e.g., `"Bad Request"`, `"Not Found"`). |
| `message` | `string` | Human-readable explanation of what went wrong. |
| `fieldErrors` | `array` | Validation errors specifying the invalid `field` and descriptive `message`. Empty array for non-validation errors. |
| `timestamp` | `string` | ISO 8601 timestamp when the error occurred. |
