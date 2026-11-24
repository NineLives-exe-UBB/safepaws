# REST API Specification & Contract

## 1. Overview & Assignments

SafePaws exposes a RESTful web API for lost and found pets, photo attachments, and printable PDF posters.

### Endpoint Overview & Team Assignments

| Method | Endpoint | Description | Auth | Implemented by | Used by (frontend) |
|---|---|---|---|---|---|
| `POST` | `/auth/register` | Creates a user account | Public | Antonia | Register page |
| `POST` | `/auth/login` | Logs in, returns a JWT valid for 24 hours | Public | Alexandru | Login page (and right after registering) |
| `GET` | `/auth/me` | Returns the logged-in user | Token | Alexandru | — |
| `POST` | `/announcements` | Creates a new announcement | Token | Antonia | Announcement form |
| `GET` | `/announcements` | Returns announcements matching optional filters (summary projection, active and newest event first by default) | Public | Antonia | List page, map page |
| `GET` | `/announcements/{id}` | Returns one announcement with full details and photo metadata | Public | Antonia | Details page |
| `PUT` | `/announcements/{id}` | Edits an announcement (replaces every form field) | Token; owner only | Antonia | Edit page |
| `PATCH` | `/announcements/{id}/status` | Marks an announcement as resolved or active again | Token; owner only | Patrick | `Mark as resolved` / `Mark as active` on the details page |
| `POST` | `/announcements/{id}/photos` | Uploads photos to an announcement (multipart) | Token; owner only | Patrick | Announcement form |
| `GET` | `/photos/{id}` | Returns raw image stream (JPG/PNG) | Public | Patrick | Announcement cards, map popups, details page |
| `GET` | `/announcements/{id}/poster` | Downloads announcement poster as PDF | Public | Alexandru | `Download poster` on each card and on the details page |
| `POST` | `/announcements/{id}/messages` | Sends a message about an announcement | Token | Antonia | Conversation page, `Send a message` on the details page |
| `GET` | `/announcements/{id}/messages` | Returns one conversation, oldest first, and marks it as read | Token; only the two people in it | Antonia | Conversation page |
| `GET` | `/conversations` | Returns all my conversations, newest first | Token | Antonia | Messages page |
| `GET` | `/messages/unread-count` | Returns how many messages I have not read | Token | Antonia | Header badge (every 30 seconds) |
| `GET` | `/notifications/area` | Returns my notification area | Token | Crișan | Notification settings |
| `PUT` | `/notifications/area` | Saves or updates my notification area | Token | Crișan | Notification settings |
| `DELETE` | `/notifications/area` | Removes my notification area | Token | Crișan | Notification settings |
| `GET` | `/notifications` | Returns my notifications (newest first) | Token | Crișan | Notifications page |
| `GET` | `/notifications/unread-count` | Returns how many notifications I have not read | Token | Crișan | Header badge (every 30 seconds) |
| `PATCH` | `/notifications/{id}/read` | Marks one notification as read | Token | Crișan | Notifications page |
| `PATCH` | `/notifications/read-all` | Marks all my notifications as read | Token | Crișan | Notifications page |

### Authentication & Permissions

User accounts follow ADR 014 (`decisions.md`); the agreed DEMO 2 contract is in `decisions/vanessza-decisions.md`.

- **Token:** log in with `POST /auth/login` and send the returned token in the header `Authorization: Bearer <token>`. Tokens are JWTs signed with HS256 using `JWT_SECRET` (`.env`), hold the user id, and are valid for 24 hours. There is no refresh token and no logout endpoint: the token cannot be revoked before it expires, and changing `JWT_SECRET` invalidates every token.
- **Public endpoints** work without a token. An invalid or expired token sent to a public endpoint is ignored: the request works as if no token was sent.
- **Token endpoints** answer `401 Unauthorized` (`Authentication is required`) when the token is missing, invalid or expired. This check runs before anything else, so a request without a token gets `401` even when its body is invalid or the announcement does not exist.

| Endpoint | Without an account | Another user | Owner |
|---|---|---|---|
| `GET /announcements`, `GET /announcements/{id}`, photos, posters | ✅ | ✅ | ✅ |
| `POST /auth/register`, `POST /auth/login` | ✅ | ✅ | ✅ |
| `GET /auth/me` | 401 | ✅ | ✅ |
| `POST /announcements` | 401 | ✅ (becomes the owner) | – |
| `PUT /announcements/{id}` | 401 | 403 | ✅ |
| `PATCH /announcements/{id}/status` | 401 | 403 | ✅ |
| `POST /announcements/{id}/photos` | 401 | 403 | ✅ |
| `POST /announcements/{id}/messages` | 401 | ✅ (always to the owner) | ✅ (only to someone who wrote) |
| `GET /announcements/{id}/messages` | 401 | ✅ own conversation; 403 for anyone else's | ✅ |
| `GET /conversations`, `GET /messages/unread-count` | 401 | ✅ | ✅ |
| `GET /notifications/area`, `PUT /notifications/area`, `DELETE /notifications/area` | 401 | ✅ | ✅ |
| `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` | 401 | ✅ | ✅ |

> **Owner check:**
> - Only the owner may edit an announcement, change its status or add photos. Another logged-in user gets `403 Forbidden` with `You can only change your own announcements`. The checks run in this order: `401`, then `400` (validation), then `404`, then `403`.
> - `POST /announcements` saves the logged-in user as the owner. Every announcement has an owner: `V8` makes `owner_id` required.
> - `GET /announcements/{id}` and the responses of `POST`, `PUT` and `PATCH` include `"ownerId": 1`. The list (`GET /announcements`) does not.
>
> Until then, any logged-in user can edit any announcement, change its status and add photos to it.

---

## 2. General Conventions
- **Base URL:** `http://localhost:8080` (all endpoints documented below are relative to this root). The frontend calls the backend on the same host it was opened from, so from a phone on the same Wi-Fi it is `http://<laptop IP>:8080`.
- **CORS:** only the frontend addresses in `CORS_ALLOWED_ORIGINS` (`.env`, comma-separated, default `http://localhost:4200`) may call the API from a browser.
- **Interactive Documentation:**
  - Swagger UI: `http://localhost:8080/swagger-ui.html` (public, no login needed)
  - OpenAPI 3 Specification (JSON): `http://localhost:8080/v3/api-docs` (public)
  - Ready-made requests with the demo accounts: `backend/auth.http` (IntelliJ HTTP Client).
- **Field Names:** All JSON identifiers are in English, using `camelCase` (e.g., `contactPhone`, `distinctiveMarks`, `eventDate`).
- **Date-Time Format:** ISO 8601 string format without zone offset: `YYYY-MM-DDTHH:mm:ss` (e.g., `2026-11-01T14:30:00`).
- **Fixed Values (Enums):** Fixed domain attributes are strictly uppercase strings:
  - Announcement Type: `LOST`, `FOUND`
  - Status: `ACTIVE`, `RESOLVED`
  - Species: `CAT`, `DOG`, `OTHER`
- **Authentication:** `Authorization: Bearer <token>` on the endpoints marked "Token" in the table above (see **Authentication & Permissions**).
- **Contact Details:** `contactName` and `contactPhone` are returned by `GET /announcements/{id}` and in the `POST /announcements`, `PUT /announcements/{id}` and `PATCH /announcements/{id}/status` responses, and printed on the poster. They are not part of `GET /announcements`. `contactPhone` is optional (V9): an announcement without a phone has `"contactPhone": null` in every response, and its poster asks people to scan the QR code and send a message instead.

---

## 3. Endpoints & Payload Details

### 3.1 Announcements

#### `POST /announcements`
Creates a new lost or found announcement.

**Auth:** token required (`Authorization: Bearer <token>`). The logged-in user becomes the owner, and the response includes `"ownerId"`.

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
| `contactPhone` | `string` | No | Valid phone string (max 20 chars, regex: `^(\+?[0-9]{7,15})?$`) when filled in. Left out, `null` or empty (`""`, spaces) means no phone and is saved as `null`. |

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

##### Error Responses
- `400 Bad Request` (`Some fields are invalid`): one `fieldErrors` entry per broken rule, sorted by field name, for example `type: Type is required`, `eventDate: Event date cannot be in the future`, `contactPhone: Contact phone must be a valid phone number`, `coordinates: Latitude and longitude must be provided together`. An unknown enum value gives `species: Invalid value 'Cat', allowed values: CAT, DOG, OTHER`; invalid JSON gives `Malformed request body`.
- `401 Unauthorized` (`Authentication is required`): no token, or an invalid or expired one.

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
Returns a single announcement by its unique identifier, including full details, coordinates, reporter contact info, and all associated photo metadata. Public.

The response also has `"ownerId"` (the id of the user who posted it), so the frontend can show the owner's actions only to the owner.

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

**Auth:** token required (`Authorization: Bearer <token>`).

> Only the owner may edit; another logged-in user gets `403 Forbidden` (`You can only change your own announcements`). The frontend edit page is planned, not on main yet; today editing is only possible through the API (see `backend/auth.http`).

##### Request Body (`application/json`)
The same body and the same field constraints as `POST /announcements` (see `AnnouncementRequest` above).

##### Response (`200 OK`)
The whole updated announcement, in the same format as `GET /announcements/{id}`.

##### Error Responses
- `400 Bad Request`: the same validation errors as `POST /announcements` (missing required fields, too long text, future `eventDate`, invalid phone, only one coordinate, invalid enum value). The body is checked before the ID, so an invalid body returns `400` even when the ID does not exist.
- `401 Unauthorized` (`Authentication is required`): no token, or an invalid or expired one. Checked first.
- `404 Not Found` (`Announcement not found with id: {id}`).
- `403 Forbidden` (`You can only change your own announcements`): the user is not the owner. Checked last.

---

#### `PATCH /announcements/{id}/status`
Changes the status of an announcement: `RESOLVED` when the pet is back home, `ACTIVE` to show it again in the default list. Sending the status it already has is not an error.

**Auth:** token required (`Authorization: Bearer <token>`). Only the owner may change the status.

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
- `401 Unauthorized` (`Authentication is required`): no token, or an invalid or expired one. Checked first.
- `404 Not Found` (`Announcement not found with id: {id}`).
- `403 Forbidden` (`You can only change your own announcements`): the user is not the owner. Checked last.

---

### 3.2 Photos & Media

#### `POST /announcements/{id}/photos`
Uploads one or more binary photos and attaches them to the specified announcement.

**Auth:** token required (`Authorization: Bearer <token>`). Only the owner may add photos.

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

##### Error Responses
- `400 Bad Request`: `No files provided`, `File is empty`, `Only JPG and PNG images are allowed`, `File name is too long`, `Invalid image file` (see the table in section 4).
- `401 Unauthorized` (`Authentication is required`): no token, or an invalid or expired one.
- `404 Not Found` (`Announcement not found with id: {id}`).
- `413 Content Too Large` (`Maximum file size is 5 MB`).
- `403 Forbidden` (`You can only change your own announcements`): the user is not the owner. Checked last.

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

User accounts follow ADR 014 (`decisions.md`); who may call what is in **Authentication & Permissions** (section 1). For local testing, Flyway seeds three demo accounts (`ana@safepaws.dev`, `mihai@safepaws.dev`, `elena@safepaws.dev`, password `SafePaws1!`, see the root `README.md`).

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

#### `POST /auth/login`
Logs in a user with email and password and returns a JWT valid for 24 hours. Public.

##### Request Body (`application/json`)
```json
{
  "email": "ana@safepaws.dev",
  "password": "SafePaws1!"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | `string` | Yes | Spaces at the ends are removed and capital letters do not matter (`" Ana@SafePaws.dev "` logs in as `ana@safepaws.dev`). |
| `password` | `string` | Yes | Compared with the stored BCrypt hash. |

##### Response (`200 OK`)
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

- `token`: send it as `Authorization: Bearer <token>`.
- `expiresAt`: when the token expires, in **UTC** (no zone offset in the string), 24 hours after the login.
- `user`: the same shape as `GET /auth/me`. The password and its hash are never returned.

##### Error Responses
- `401 Unauthorized` (`Invalid email or password`), with `"fieldErrors": null`. The same answer for an unknown email, a wrong password, a missing or blank field, a missing body, and a password longer than 72 bytes, so the API does not reveal which emails have an account. There is no `400` for this endpoint.

---

#### `GET /auth/me`
Returns the currently logged-in user. Token required (`Authorization: Bearer <token>`).

##### Response (`200 OK`)
```json
{
  "id": 1,
  "email": "ana@safepaws.dev",
  "displayName": "Ana Pop"
}
```

##### Error Responses
- `401 Unauthorized` (`Authentication is required`), with `"fieldErrors": null`: the token is missing, invalid or expired, or its user no longer exists.


---

### 3.5 Messages

Messages follow ADR 015 (`decisions.md`): they exist only in the app, and the frontend checks for new ones every 30 seconds. A **conversation** is every message about one announcement between the same two people: the owner and someone who wrote to them. Every endpoint needs a token; the checks run in the order `401`, `400`, `404`, `403`.

Times are `YYYY-MM-DDTHH:mm:ss`, without fractions of a second.

#### `POST /announcements/{id}/messages`
Sends a message about the announcement.

- Someone who is **not the owner** leaves out `recipientId`: the message always goes to the owner. Naming anyone else returns `403`.
- The **owner** must give `recipientId`: the person they answer, who must have written about this announcement before. So nobody can start a conversation with their own announcement.

##### Request Body (`application/json`)
```json
{
  "body": "Hi! I think I saw your cat near the park.",
  "recipientId": null
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `body` | `string` | Yes | Not blank, max 1000 characters. Spaces at the start and end are removed. |
| `recipientId` | `number` | Owner only | The id of someone who has written about this announcement. Left out (or `null`) by everyone else. |

##### Response (`201 Created`)
```json
{
  "id": 12,
  "senderId": 2,
  "recipientId": 1,
  "body": "Hi! I think I saw your cat near the park.",
  "createdAt": "2026-11-25T18:40:00",
  "readAt": null
}
```

##### Error Responses
- `400 Bad Request` (`Some fields are invalid`):
  - `body`: `Message is required`, `Message must be at most 1000 characters`
  - `recipientId`: `Recipient is required` (the owner did not give one), `Recipient has not written about this announcement` (also when the owner names themselves)
- `403 Forbidden`: `You can only write in your own conversations` (someone who is not the owner names a `recipientId` other than the owner)
- `404 Not Found`: `Announcement not found with id: {id}`

---

#### `GET /announcements/{id}/messages?with={userId}`
Returns the messages between me and `userId` about this announcement, **oldest first**. Opening the conversation marks the messages I received in it as read (their `readAt` is set); messages I sent stay unread until the other person opens it.

- Someone who is not the owner can leave out `with`: it means the owner.
- The owner must give `with`.
- A conversation nobody has written in yet returns `[]`.

##### Response (`200 OK`)
A list of messages, each in the same shape as the `POST` response above.

##### Error Responses
- `400 Bad Request`: `with: Conversation partner is required` (the owner left out `with`); `with: Invalid value 'abc'` (not a number)
- `403 Forbidden`: `You can only read your own conversations` (neither I nor `with` is the owner, so it is someone else's conversation)
- `404 Not Found`: `Announcement not found with id: {id}`

---

#### `GET /conversations`
Returns all my conversations, as owner and as writer, **the one with the newest message first**.

##### Response (`200 OK`)
```json
[
  {
    "announcementId": 1,
    "petName": "Mitzi",
    "species": "CAT",
    "type": "LOST",
    "otherUser": { "id": 2, "displayName": "Mihai Rus" },
    "lastMessage": {
      "body": "I saw her again this evening, close to the first spot. She looked fine.",
      "createdAt": "2026-11-06T19:15:00",
      "fromMe": false
    },
    "unreadCount": 1
  }
]
```

| Field | Notes |
|---|---|
| `petName` | `null` when the announcement has none (often for `FOUND`) |
| `otherUser` | The other person in the conversation |
| `lastMessage.fromMe` | `true` when I sent the last message |
| `unreadCount` | Messages I received in this conversation and have not opened |

---

#### `GET /messages/unread-count`
How many messages I have received and not opened, in all conversations. The header calls it every 30 seconds.

##### Response (`200 OK`)
```json
{ "count": 3 }
```

---

### 3.6 Notifications

Notifications follow ADR 015 (`decisions.md`): a user sets an area (a point and a radius), and whenever a new announcement is posted inside that area, the user gets an in-app notification. The author never gets one for their own announcement, and an announcement without coordinates creates no notifications. The frontend checks for new notifications every 30 seconds. Every endpoint needs a token.

Times are `YYYY-MM-DDTHH:mm:ss`, without fractions of a second.

#### `GET /notifications/area`
Returns the current user's notification area. Token required.

##### Response (`200 OK`)
```json
{
  "latitude": 46.7571,
  "longitude": 23.5547,
  "radiusKm": 3
}
```

##### Error Responses
- `401 Unauthorized` (`Authentication is required`): no token or invalid/expired token.
- `404 Not Found` (`Notification area is not set`): user has not set an area.

---

#### `PUT /notifications/area`
Saves or replaces the user's notification area. Token required.

##### Request Body (`application/json`)
```json
{
  "latitude": 46.7571,
  "longitude": 23.5547,
  "radiusKm": 3
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `latitude` | `number` | Yes | `[-90.0, 90.0]` |
| `longitude` | `number` | Yes | `[-180.0, 180.0]` |
| `radiusKm` | `integer` | Yes | Whole number of km between `1` and `20` |

##### Response (`200 OK`)
The saved notification area (same shape as `GET /notifications/area`).

##### Error Responses
- `400 Bad Request` (`Some fields are invalid`): `latitude`: "Latitude is required", "Latitude must be between -90 and 90"; `longitude`: "Longitude is required", "Longitude must be between -180 and 180"; `radiusKm`: "Radius is required", "Radius must be between 1 and 20 km".
- `401 Unauthorized` (`Authentication is required`).

---

#### `DELETE /notifications/area`
Removes the current user's notification area. Existing notifications are retained. Token required.

##### Response (`204 No Content`)

##### Error Responses
- `401 Unauthorized` (`Authentication is required`).

---

#### `GET /notifications`
Returns the user's notifications (up to 50, newest first). Token required.

##### Response (`200 OK`)
```json
[
  {
    "id": 7,
    "announcement": {
      "id": 31,
      "type": "LOST",
      "species": "DOG",
      "petName": "Bruno",
      "area": "Grigorescu, Cluj-Napoca"
    },
    "createdAt": "2026-11-26T10:05:00",
    "read": false
  }
]
```

##### Error Responses
- `401 Unauthorized` (`Authentication is required`).

---

#### `GET /notifications/unread-count`
Returns how many notifications the user has not read. Checked by the header every 30 seconds. Token required.

##### Response (`200 OK`)
```json
{
  "count": 2
}
```

##### Error Responses
- `401 Unauthorized` (`Authentication is required`).

---

#### `PATCH /notifications/{id}/read`
Marks a single notification as read. Token required.

##### Response (`204 No Content`)

##### Error Responses
- `401 Unauthorized` (`Authentication is required`).
- `404 Not Found` (`Notification not found with id: {id}`): when the notification does not exist or belongs to another user.

---

#### `PATCH /notifications/read-all`
Marks all of the user's notifications as read. Token required.

##### Response (`204 No Content`)

##### Error Responses
- `401 Unauthorized` (`Authentication is required`).

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
| `fieldErrors` | `array` or `null` | Validation errors specifying the invalid `field` and descriptive `message`. Empty array for other errors and for 401 from SecurityConfig, `null` for 401 from wrong credentials (`/auth/login`). |
| `timestamp` | `string` | ISO 8601 timestamp of when the error occurred, truncated to seconds. |

### Real Error Messages Reference Table

The table below catalogs the exact HTTP status codes, reason phrases, triggers, and error messages returned by `GlobalExceptionHandler` and domain services:

| Status Code | Reason Phrase (`error`) | Exception / Trigger | Message (`message`) | `fieldErrors` Payload |
|---|---|---|---|---|
| `400 Bad Request` | `Bad Request` | Validation failure (`MethodArgumentNotValidException`) | `Some fields are invalid` | Array of `{field, message}` (e.g., `type: "Type is required"`, `species: "Species is required"`, `petName: "Pet name must be at most 50 characters"`, `eventDate: "Event date cannot be in the future"`, `area: "Area is required"`, `contactName: "Contact name is required"`, `contactPhone: "Contact phone must be a valid phone number"`, `coordinates: "Latitude and longitude must be provided together"`, `status: "Status is required"`, `email: "Email must be a valid email address"`, `password: "Password must have at least 8 characters, one uppercase letter, one digit and one special character"`), sorted alphabetically by field name |
| `400 Bad Request` | `Bad Request` | Invalid enum value (`HttpMessageNotReadableException`) | `Some fields are invalid` | `[{"field": "<fieldName>", "message": "Invalid value '<val>', allowed values: <ALLOWED_VALUES>"}]` |
| `400 Bad Request` | `Bad Request` | Malformed JSON request body (`HttpMessageNotReadableException`) | `Malformed request body` | `[]` (empty) |
| `400 Bad Request` | `Bad Request` | Path or query parameter type mismatch (`MethodArgumentTypeMismatchException`, e.g. `/announcements/abc` or `?species=cat`) | `Some fields are invalid` | `[{"field": "id", "message": "Invalid value 'abc'"}]` (for enums followed by `, allowed values: ...`) |
| `400 Bad Request` | `Bad Request` | Invalid `GET /announcements` filter combination (`InvalidFilterException`) | `Some fields are invalid` | Array of `{field, message}`, see `GET /announcements` above |
| `400 Bad Request` | `Bad Request` | A rule only the service can check (`InvalidRequestException`), e.g. a message recipient who never wrote | `Some fields are invalid` | `[{"field": "recipientId", "message": "Recipient has not written about this announcement"}]`, see `3.5 Messages` |
| `400 Bad Request` | `Bad Request` | No files provided in upload (`PhotoService`) | `No files provided` | `[]` (empty) |
| `400 Bad Request` | `Bad Request` | Uploaded file is empty (`PhotoService`) | `File is empty` | `[]` (empty) |
| `400 Bad Request` | `Bad Request` | Non-JPG/PNG mime type (`PhotoService`) | `Only JPG and PNG images are allowed` | `[]` (empty) |
| `400 Bad Request` | `Bad Request` | File name exceeds 255 characters (`PhotoService`) | `File name is too long` | `[]` (empty) |
| `400 Bad Request` | `Bad Request` | Corrupted image data / invalid magic bytes (`PhotoService`) | `Invalid image file` | `[]` (empty) |
| `401 Unauthorized` | `Unauthorized` | Missing, invalid or expired token on a token endpoint (Spring Security), or the token's user no longer exists (`/auth/me`) | `Authentication is required` | `[]` (empty) |
| `401 Unauthorized` | `Unauthorized` | Wrong login (`InvalidCredentialsException`) | `Invalid email or password` | `null` |
| `403 Forbidden` | `Forbidden` | Changing an announcement you do not own (`AnnouncementOwnershipService`) | `You can only change your own announcements` | `[]` (empty) |
| `403 Forbidden` | `Forbidden` | Reading a conversation you are not part of (`MessageService`) | `You can only read your own conversations` | `[]` (empty) |
| `403 Forbidden` | `Forbidden` | Writing to someone other than the owner when you are not the owner (`MessageService`) | `You can only write in your own conversations` | `[]` (empty) |
| `404 Not Found` | `Not Found` | Announcement ID not found (`AnnouncementNotFoundException`) | `Announcement not found with id: {id}` | `[]` (empty) |
| `404 Not Found` | `Not Found` | Photo ID not found (`PhotoService`) | `Photo not found with id: {id}` | `[]` (empty) |
| `409 Conflict` | `Conflict` | Email already has an account (`EmailAlreadyRegisteredException`) | `Email is already registered` | `[]` (empty) |
| `413 Content Too Large` | `Content Too Large` | Uploaded file exceeds 5 MB (`PhotoService`) | `Maximum file size is 5 MB` | `[]` (empty) |
| `415 Unsupported Media Type` | `Unsupported Media Type` | Incorrect media type (e.g. `text/plain` on `POST`) | Spring Web default detail message | `[]` (empty) |
| `500 Internal Server Error` | `Internal Server Error` | Unexpected server runtime exception | `An unexpected error occurred` | `[]` (empty) |