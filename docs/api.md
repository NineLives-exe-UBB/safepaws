# REST API Specification & Contract

## 1. Overview & Assignments

SafePaws exposes a RESTful web API for lost and found pets, photo attachments, and printable PDF posters.

### Endpoint Overview & Team Assignments

| Method | Endpoint | Description | Implemented by | Used by (frontend) |
|---|---|---|---|---|
| `POST` | `/announcements` | Creates a new announcement | Antonia | Announcement form |
| `GET` | `/announcements` | Returns announcements matching optional filters (summary projection, active and newest event first by default) | Antonia | List page, map page |
| `GET` | `/announcements/{id}` | Returns one announcement with full details and photo metadata | Antonia | Details page |
| `PUT` | `/announcements/{id}` | Edits an announcement (replaces every form field) | Antonia | Edit page |
| `PATCH` | `/announcements/{id}/status` | Marks an announcement as resolved or active again | Patrick | `Mark as resolved` / `Mark as active` on the details page |
| `POST` | `/announcements/{id}/photos` | Uploads photos to an announcement (multipart) | Patrick | Announcement form |
| `GET` | `/photos/{id}` | Returns raw image stream (JPG/PNG) | Patrick | Announcement cards, map popups, details page |
| `GET` | `/announcements/{id}/poster` | Downloads announcement poster as PDF | Alexandru | `Download poster` on each card and on the details page |
| `POST` | `/auth/register` | Creates a user account | Antonia | Register page |

There is no authentication: all endpoints are public, so anyone can also change the status of an announcement. User accounts are planned for DEMO 2.

---

## 2. General Conventions
- **Base URL:** `http://localhost:8080` (all endpoints documented below are relative to this root). The frontend calls the backend on the same host it was opened from, so from a phone on the same Wi-Fi it is `http://<laptop IP>:8080`.
- **CORS:** only the frontend addresses in `CORS_ALLOWED_ORIGINS` (`.env`, comma-separated, default `http://localhost:4200`) may call the API from a browser.
- **Interactive Documentation:**
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
  - OpenAPI 3 Specification (JSON): `http://localhost:8080/v3/api-docs`
- **Field Names:** All JSON identifiers are in English, using `camelCase` (e.g., `contactPhone`, `distinctiveMarks`, `eventDate`).
- **Date-Time Format:** ISO 8601 string format without zone offset: `YYYY-MM-DDTHH:mm:ss` (e.g., `2026-11-01T14:30:00`).
- **Fixed Values (Enums):** Fixed domain attributes are strictly uppercase strings:
  - Announcement Type: `LOST`, `FOUND`
  - Status: `ACTIVE`, `RESOLVED`
  - Species: `CAT`, `DOG`, `OTHER`
- **Contact Details:** `contactName` and `contactPhone` are returned by `GET /announcements/{id}` and in the `POST /announcements` and `PATCH /announcements/{id}/status` responses, and printed on the poster. They are not part of `GET /announcements`.

---

## 3. Endpoints & Payload Details

### 3.1 Announcements

#### `POST /announcements`
Creates a new lost or found announcement.

**Headers Required:**
- `Authorization: Bearer <token>`

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
| `area` | `string` | Yes | "Neighborhood, City", for example `Mănăștur, Cluj-Napoca`. Not blank, max 150 characters. |
| `contactName` | `string` | Yes | Not blank, max 100 characters. |
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
**Error Responses:**
- `401 Unauthorized` — `"Authentication is required"`
- `403 Forbidden` — `"You can only change your own announcements"`


---

#### `GET /announcements`
Returns the announcements that match the optional query parameters, as a lightweight summary projection (`AnnouncementSummaryResponse`) without detailed descriptions or contact information. All parameters combine with AND.

**Without any parameter:** only `ACTIVE` announcements, newest `eventDate` first (ties by `id`, highest first).

##### Query Parameters (all optional)
| Parameter | Type | Rules |
|---|---|---|
| `type` | `string` | `LOST` or `FOUND`, exact match. |
| `species` | `string` | `CAT`, `DOG` or `OTHER`, exact match. |
| `status` | `string` | `ACTIVE` or `RESOLVED`, exact match. Default: `ACTIVE` (there is no value for "all statuses"). |
| `color` | `string` | Part of the color, max 100 characters. Ignores case, diacritics and spaces at the ends (`grey` finds `Dark grey`). |
| `breed` | `string` | Part of the breed, max 100 characters. Same matching as `color`. |
| `area` | `string` | Part of the area, max 150 characters. Same matching as `color` (`manastur` finds `Mănăștur, Cluj-Napoca`). |
| `lat` | `number` | User latitude, `-90` to `90`. Must be sent together with `lng`. |
| `lng` | `number` | User longitude, `-180` to `180`. Must be sent together with `lat`. |
| `radiusKm` | `number` | `1` to `100`. Requires `lat` and `lng`. Default when a location is sent: `10`. |
| `sort` | `string` | `newest` (default) or `oldest`, by `eventDate`; or `closest`, which requires `lat` and `lng`. |

Blank text parameters are ignored. In `color`, `breed` and `area`, `%` and `_` are matched literally, not as wildcards.

**When `lat` and `lng` are sent:** the distance to each announcement is calculated (great-circle / haversine), announcements without coordinates or farther than `radiusKm` are left out, and each result has `distanceKm` (rounded to one decimal). With `sort=closest`, results are ordered by distance; equal distances keep the newest-first order.

##### Example
`GET /announcements?species=CAT&area=manastur&lat=46.77&lng=23.59&radiusKm=5&sort=closest`

##### Response (`200 OK`)
```json
[
  {
    "id": 15,
    "type": "LOST",
    "petName": "Misi",
    "species": "CAT",
    "area": "Mănăștur, Cluj-Napoca",
    "latitude": 46.7712,
    "longitude": 23.6236,
    "distanceKm": 2.6,
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

`distanceKm` is only present when `lat` and `lng` were sent. `latitude` and `longitude` are `null` for an announcement without coordinates. Photos are ordered with the main photo first.

##### Error Responses
- `400 Bad Request` (`message: Some fields are invalid`) with one `fieldErrors` entry per broken rule, for example:
  - `type`, `species`, `status`, `lat`, `lng`, `radiusKm`: `Invalid value 'xyz'` (for enums followed by `, allowed values: ...`) when the value cannot be converted
  - `color`: `color must be at most 100 characters` (same for `breed`, and `area` with 150)
  - `lat` / `lng`: `Latitude and longitude must be provided together`, `Latitude must be between -90 and 90`, `Longitude must be between -180 and 180`
  - `radiusKm`: `radiusKm requires lat and lng`, `radiusKm must be between 1 and 100`
  - `sort`: `Invalid value 'xyz', allowed values: newest, oldest, closest`, `sort=closest requires lat and lng`

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

#### `PUT /announcements/{id}`
Edits an announcement. Every field of the form is replaced with the body, so a field left out (for example `petName`) is cleared, not kept.

The status, the photos, the owner and `createdAt` do not change here: the status has `PATCH /announcements/{id}/status`, and photos are added with `POST /announcements/{id}/photos`.

> **Owner only (ADR 014), coming in a separate PR:** once the owner check is on main, editing needs a token and only the owner may edit. Without a token → `401 Unauthorized` (`Authentication is required`); another user → `403 Forbidden` (`You can only change your own announcements`). Until then, this endpoint is public like the rest of the API.

**Headers Required:**
- `Authorization: Bearer <token>`

##### Request Body (`application/json`)
The same body and the same field constraints as `POST /announcements` (see `AnnouncementRequest` above).

##### Response (`200 OK`)
The whole updated announcement, in the same format as `GET /announcements/{id}`.

##### Error Responses
- `400 Bad Request`: the same validation errors as `POST /announcements` (missing required fields, too long text, future `eventDate`, invalid phone, only one coordinate, invalid enum value). The body is checked first, so an invalid body returns `400` even when the ID does not exist.
- `401 Unauthorized` — `"Authentication is required"`
- `403 Forbidden` — `"You can only change your own announcements"`
- `404 Not Found`: When no announcement exists with the requested ID.
---

#### `PATCH /announcements/{id}/status`
Changes the status of an announcement: `RESOLVED` when the pet is back home, `ACTIVE` to show it again in the default list. Sending the status it already has is not an error.

**Headers Required:**
- `Authorization: Bearer <token>`

##### Request Body (`application/json`)
```json
{
  "status": "RESOLVED"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `status` | `string` | Yes | Must be `ACTIVE` or `RESOLVED`. |

##### Response (`200 OK`)
The whole updated announcement, in the same format as `GET /announcements/{id}`, with the new `status`.

##### Error Responses
- `400 Bad Request`: `status` is missing (`status: Status is required`) or has another value (`status: Invalid value 'DONE', allowed values: ACTIVE, RESOLVED`).
- `401 Unauthorized` — `"Authentication is required"`
- `403 Forbidden` — `"You can only change your own announcements"`
- `404 Not Found`: When no announcement exists with the requested ID.

---

### 3.2 Photos & Media

#### `POST /announcements/{id}/photos`
Uploads one or more binary photos and attaches them to the specified announcement.

**Headers Required:**
- `Authorization: Bearer <token>`

##### Request Format
- **Content-Type:** `multipart/form-data`
- **Form Field:** `files` (array of `MultipartFile`)

##### Constraints
- **File Types:** Only `image/jpeg` and `image/png` are accepted (validated by inspecting magic byte headers).
- **File Size Limits:** Max 5 MB per file (`413 Payload Too Large`), max 25 MB total request payload.
- **Filename:** Max 255 characters.
- **Main Photo Behavior:** If the announcement does not yet have a main photo, the first uploaded photo is automatically marked `isMain: true`. All other photos get `isMain: false`.
- **Unknown Announcement:** `404 Not Found` (`Announcement not found with id: {id}`).

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

**Error Responses:**
- `401 Unauthorized` — `"Authentication is required"`
- `403 Forbidden` — `"You can only change your own announcements"`

---

#### `GET /photos/{id}`
Retrieves the raw binary image stream stored in PostgreSQL (`BYTEA`).

##### Response (`200 OK`)
- **Headers:** `Content-Type: image/jpeg` or `image/png`, and `Cache-Control: max-age=86400`, so the browser keeps a photo for one day instead of downloading it again (a photo never changes once uploaded).
- **Body:** Raw binary byte stream. Can be used directly in HTML templates: `<img src="http://localhost:8080/photos/{id}">` or `<img src="/photos/{id}">`.

##### Error Responses
- `404 Not Found`: Photo ID not found.

---

### 3.3 Poster Generation

#### `GET /announcements/{id}/poster`
Generates and downloads a printable lost/found pet poster as a one-page A4 PDF.

The poster shows the type (`LOST` / `FOUND`, plus `RESOLVED` when resolved) as large badges, a question as the title ("Have you seen Luna?", "Is this your cat?"), the area, the main photo (grey when resolved, a placeholder when there is none), the filled-in details, the contact and a QR code. The QR code opens the announcement's details page, at the address in `FRONTEND_URL` (`.env`, default `http://localhost:4200`).

##### Response (`200 OK`)
- **Headers:** `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="announcement-{id}-poster.pdf"`
- **Body:** Binary PDF byte stream downloaded directly by the browser.

##### Error Responses
- `404 Not Found`: When no announcement exists with the requested ID.

---

### 3.4 Accounts

User accounts follow ADR 014 (`decisions.md`). The full DEMO 2 contract, including login and who may change what, is in `decisions/vanessza-decisions.md`.

#### `POST /auth/register`
Creates a user account. Public: no token is needed. Registering does not log the user in, so the frontend calls `POST /auth/login` right after.

##### Request Body (`application/json`)
```json
{
  "email": "ana.pop@example.com",
  "password": "SafePaws1!",
  "displayName": "Ana Pop"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `email` | `string` | Yes | Valid email address, max 255 characters. Spaces at the start and end are removed and it is saved in lowercase, so `" Ana@Example.com "` becomes `ana@example.com`. |
| `password` | `string` | Yes | At least 8 characters, with at least one uppercase letter, one digit and one special character (anything that is not a letter or a digit). At most 72 characters (counted in bytes, the BCrypt limit: a letter like `ă` counts as 2). |
| `displayName` | `string` | Yes | Not blank, max 50 characters. Spaces at the start and end are removed. |

The password is stored only as a BCrypt hash and is never returned by any endpoint.

##### Response (`201 Created`)
```json
{
  "id": 4,
  "email": "ana.pop@example.com",
  "displayName": "Ana Pop"
}
```

##### Error Responses
- `400 Bad Request`, with one `fieldErrors` entry per broken rule:
  - `email`: `Email is required`, `Email must be a valid email address`, `Email must be at most 255 characters`
  - `password`: `Password is required`, `Password must have at least 8 characters, one uppercase letter, one digit and one special character`, `Password must be at most 72 characters`
  - `displayName`: `Display name is required`, `Display name must be at most 50 characters`
  - A missing or blank field only gets its `... is required` message.
- `409 Conflict`: `Email is already registered`, also when the same email was registered with different capital letters.

### `POST /auth/login`

Logs in a user with email and password and returns a 24-hour JWT token.

**Request Body:**
```json
{
  "email": "ana@safepaws.dev",
  "password": "SafePaws1!"
}
```

**Response `200 OK`:**
```json
{
  "token": "<JWT>",
  "expiresAt": "2026-11-17T00:15:00",
  "user": {
    "id": 1,
    "email": "ana@safepaws.dev",
    "displayName": "Ana Pop"
  }
}
```

**Error Responses:**
- `401 Unauthorized` — `"Invalid email or password"` (returned for both unknown email and wrong password)

---

### `GET /auth/me`

Returns the currently authenticated user. Requires `Authorization: Bearer <token>`.

**Response `200 OK`:**
```json
{
  "id": 1,
  "email": "ana@safepaws.dev",
  "displayName": "Ana Pop"
}
```

**Error Responses:**
- `401 Unauthorized` — `"Authentication is required"` (missing, invalid, or expired token)


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

### Real Error Messages Reference Table

The table below catalogs the exact HTTP status codes, reason phrases, triggers, and error messages returned by `GlobalExceptionHandler` and domain services:

| Status Code | Reason Phrase (`error`) | Exception / Trigger | Message (`message`) | `fieldErrors` Payload |
|---|---|---|---|---|
| `400 Bad Request` | `Bad Request` | Validation failure (`MethodArgumentNotValidException`) | `Some fields are invalid` | Array of `{field, message}` (e.g., `type: "Type is required"`, `species: "Species is required"`, `petName: "Pet name must be at most 50 characters"`, `eventDate: "Event date cannot be in the future"`, `area: "Area is required"`, `contactName: "Contact name is required"`, `contactPhone: "Contact phone must be a valid phone number"`, `coordinates: "Latitude and longitude must be provided together"`, `status: "Status is required"`), sorted alphabetically by field name |
| `400 Bad Request` | `Bad Request` | Validation failure (`MethodArgumentNotValidException`) | `Some fields are invalid` | Array of `{field, message}` (e.g., `type: "Type is required"`, `species: "Species is required"`, `petName: "Pet name must be at most 50 characters"`, `eventDate: "Event date cannot be in the future"`, `area: "Area is required"`, `contactName: "Contact name is required"`, `contactPhone: "Contact phone must be a valid phone number"`, `coordinates: "Latitude and longitude must be provided together"`, `status: "Status is required"`, `email: "Email must be a valid email address"`, `password: "Password must have at least 8 characters, one uppercase letter, one digit and one special character"`), sorted alphabetically by field name |
| `400 Bad Request` | `Bad Request` | Invalid enum value (`HttpMessageNotReadableException`) | `Some fields are invalid` | `[{"field": "<fieldName>", "message": "Invalid value '<val>', allowed values: <ALLOWED_VALUES>"}]` |
| `400 Bad Request` | `Bad Request` | Malformed JSON request body (`HttpMessageNotReadableException`) | `Malformed request body` | `[]` (empty) |
| `400 Bad Request` | `Bad Request` | Path or query parameter type mismatch (`MethodArgumentTypeMismatchException`, e.g. `/announcements/abc` or `?species=cat`) | `Some fields are invalid` | `[{"field": "id", "message": "Invalid value 'abc'"}]` (for enums followed by `, allowed values: ...`) |
| `400 Bad Request` | `Bad Request` | Invalid `GET /announcements` filter combination (`InvalidFilterException`) | `Some fields are invalid` | Array of `{field, message}`, see `GET /announcements` above |
| `400 Bad Request` | `Bad Request` | No files provided in upload (`PhotoService`) | `No files provided` | `[]` (empty) |
| `400 Bad Request` | `Bad Request` | Uploaded file is empty (`PhotoService`) | `File is empty` | `[]` (empty) |
| `400 Bad Request` | `Bad Request` | Non-JPG/PNG mime type (`PhotoService`) | `Only JPG and PNG images are allowed` | `[]` (empty) |
| `400 Bad Request` | `Bad Request` | File name exceeds 255 characters (`PhotoService`) | `File name is too long` | `[]` (empty) |
| `400 Bad Request` | `Bad Request` | Corrupted image data / invalid magic bytes (`PhotoService`) | `Invalid image file` | `[]` (empty) |
| `404 Not Found` | `Not Found` | Announcement ID not found (`AnnouncementNotFoundException`) | `Announcement not found with id: {id}` | `[]` (empty) |
| `404 Not Found` | `Not Found` | Photo ID not found (`PhotoService`) | `Photo not found with id: {id}` | `[]` (empty) |
| `409 Conflict` | `Conflict` | Email already has an account (`EmailAlreadyRegisteredException`) | `Email is already registered` | `[]` (empty) |
| `413 Content Too Large` | `Content Too Large` | Uploaded file exceeds 5 MB (`PhotoService`) | `Maximum file size is 5 MB` | `[]` (empty) |
| `415 Unsupported Media Type` | `Unsupported Media Type` | Incorrect media type (e.g. `text/plain` on `POST`) | Spring Web default detail message | `[]` (empty) |
| `500 Internal Server Error` | `Internal Server Error` | Unexpected server runtime exception | `An unexpected error occurred` | `[]` (empty) |