# REST API Specification & Contract

## 1. Overview & Assignments

SafePaws exposes a RESTful web API for lost and found pets, photo attachments, and printable PDF posters.

### Endpoint Overview & Team Assignments

| Method | Endpoint | Description | Implemented by | Used by |
|---|---|---|---|---|
| `POST` | `/announcements` | Creates a new announcement | Antonia | Victor (form) |
| `GET` | `/announcements` | Returns all announcements (summary projection, newest first) | Antonia | Luca (list) |
| `GET` | `/announcements/{id}` | Returns one announcement with full details and attached photos | Antonia | Luca (details) |
| `POST` | `/announcements/{id}/photos` | Uploads photos to an announcement (multipart) | Patrick | Victor (upload) |
| `GET` | `/photos/{id}` | Returns raw image stream (JPG/PNG) | Patrick | Luca |
| `GET` | `/announcements/{id}/poster` | Downloads announcement poster as PDF | Alexandru | Luca |

---

## 2. General Conventions
- **Base URL:** `http://localhost:8080` (all endpoints documented below are relative to this root).
- **Interactive Documentation:**
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
  - OpenAPI 3 Specification (JSON): `http://localhost:8080/v3/api-docs`
- **Field Names:** All JSON identifiers are in English, using `camelCase` (e.g., `contactPhone`, `distinctiveMarks`, `eventDate`).
- **Date-Time Format:** ISO 8601 string format without zone offset: `YYYY-MM-DDTHH:mm:ss` (e.g., `2026-11-01T14:30:00`).
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
  "species": "CAT",
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

##### Field Constraints (`AnnouncementRequest`)
| Field | Type | Required | Constraints |
|---|---|---|---|
| `type` | `string` | Yes | Must be `LOST` or `FOUND`. |
| `species` | `string` | Yes | Must be `CAT`, `DOG`, or `OTHER`. |
| `petName` | `string` | No | Max 50 characters. |
| `breed` | `string` | No | Max 100 characters. |
| `color` | `string` | No | Max 100 characters. |
| `distinctiveMarks` | `string` | No | Max 500 characters. |
| `eventDate` | `string` | Yes | ISO 8601 datetime; cannot be in the future. |
| `latitude` | `number` | No | Range: `[-90.0, 90.0]`; must be provided together with `longitude`. |
| `longitude` | `number` | No | Range: `[-180.0, 180.0]`; must be provided together with `latitude`. |
| `area` | `string` | Yes | Neighborhood/city description (max 150 chars). |
| `contactName` | `string` | Yes | Max 100 characters. |
| `contactPhone` | `string` | Yes | Valid phone string (max 20 chars, regex: `^(\+?[0-9]{7,15})?$`). |

##### Response (`201 Created`)
- **Headers:** `Location: /announcements/{id}`
- **Body:**
```json
{
  "id": 15,
  "type": "LOST",
  "species": "CAT",
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
Returns all announcements in descending order by creation date and ID (newest first). Returns a lightweight summary projection (`AnnouncementSummaryResponse`) that omits detailed descriptions and contact information to optimize listing bandwidth.

##### Response (`200 OK`)
```json
[
  {
    "id": 15,
    "type": "LOST",
    "petName": "Misi",
    "species": "CAT",
    "area": "Mănăștur, Cluj-Napoca",
    "eventDate": "2026-10-30T18:00:00",
    "status": "ACTIVE",
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
Returns a single announcement by its unique identifier, including full details, coordinates, reporter contact info, and all associated photo metadata.

##### Response (`200 OK`)
```json
{
  "id": 15,
  "type": "LOST",
  "species": "CAT",
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
      "announcementId": 15,
      "contentType": "image/jpeg",
      "fileSize": 245120,
      "originalFileName": "misi_front.jpg",
      "isMain": true,
      "url": "/photos/3",
      "uploadedAt": "2026-11-01T14:31:00"
    },
    {
      "id": 4,
      "announcementId": 15,
      "contentType": "image/png",
      "fileSize": 312400,
      "originalFileName": "misi_side.png",
      "isMain": false,
      "url": "/photos/4",
      "uploadedAt": "2026-11-01T14:31:05"
    }
  ]
}
```

##### Error Responses
- `404 Not Found`: When no announcement exists with the requested ID.

---

### 3.2 Photos & Media

#### `POST /announcements/{id}/photos`
Uploads one or more binary photos and attaches them to the specified announcement.

##### Request Format
- **Content-Type:** `multipart/form-data`
- **Form Field:** `files` (array of `MultipartFile`)

##### Constraints
- **File Types:** Only `image/jpeg` and `image/png` are accepted (validated by inspecting magic byte headers).
- **File Size Limits:** Max 5 MB per file (`413 Payload Too Large`), max 25 MB total request payload.
- **Filename:** Max 255 characters.
- **Main Photo Behavior:** If the announcement does not yet have a main photo, the first uploaded photo is automatically marked `isMain: true`. Subsequent uploads default to `isMain: false`.

##### Response (`201 Created`)
```json
[
  {
    "id": 3,
    "announcementId": 15,
    "contentType": "image/jpeg",
    "fileSize": 245120,
    "originalFileName": "misi_front.jpg",
    "isMain": true,
    "url": "/photos/3",
    "uploadedAt": "2026-11-01T14:31:00"
  },
  {
    "id": 4,
    "announcementId": 15,
    "contentType": "image/png",
    "fileSize": 312400,
    "originalFileName": "misi_side.png",
    "isMain": false,
    "url": "/photos/4",
    "uploadedAt": "2026-11-01T14:31:05"
  }
]
```

---

#### `GET /photos/{id}`
Retrieves the raw binary image stream stored in PostgreSQL (`BYTEA`).

##### Response (`200 OK`)
- **Headers:** `Content-Type: image/jpeg` or `image/png`
- **Body:** Raw binary byte stream. Can be used directly in HTML templates: `<img src="http://localhost:8080/photos/{id}">` or `<img src="/photos/{id}">`.

##### Error Responses
- `404 Not Found`: Photo ID not found.

---

### 3.3 Poster Generation

#### `GET /announcements/{id}/poster`
Generates and downloads a printable missing/found pet poster as a PDF document.

##### Response (`200 OK`)
- **Headers:** `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="poster_{id}.pdf"`
- **Body:** Binary PDF byte stream downloaded directly by the browser.

---

## 4. Standard Error Response Format

All API errors return a consistent JSON structure defined by `ErrorResponse`:

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
| `status` | `number` | HTTP status code (e.g., `400`, `404`, `413`, `500`). |
| `error` | `string` | Short reason phrase (e.g., `"Bad Request"`, `"Not Found"`, `"Content Too Large"`). |
| `message` | `string` | Human-readable explanation of what went wrong. |
| `fieldErrors` | `array` | Validation errors specifying the invalid `field` and descriptive `message`. Empty array for non-validation errors. |
| `timestamp` | `string` | ISO 8601 timestamp truncated to seconds when the error occurred. |
