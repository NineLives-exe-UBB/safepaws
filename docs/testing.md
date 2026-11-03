# Testing

How SafePaws is tested: automated tests, manual test scenarios and how to report bugs.

## 1. Automated tests

Both parts fail the build when coverage is under 70%.

| Part | Command | What it needs | Coverage on 3 Nov |
|---|---|---|---|
| backend | `cd backend && ./mvnw verify` | Docker running (the repository test starts a PostgreSQL with Testcontainers) | 95.7% instructions, 34 tests |
| frontend | `cd frontend && npm run test:coverage` | `npm install` done once | 97.7% statements, 24 tests |

The backend threshold is in `pom.xml` (jacoco, `verify` phase). The frontend threshold is in `angular.json` (`coverageThresholds`). Both run on every pull request through GitHub Actions (`.github/workflows/ci.yml`), together with a secret scan and a Flyway migration guard.

Where the tests live:

- backend: `backend/src/test/java/.../safepaws/`, same tree as `main`. `controller/` has `@WebMvcTest` slice tests (status codes, JSON, validation) with the service mocked, `service/` has unit tests with mocked repositories, `repository/` has `@DataJpaTest` tests against a real PostgreSQL started by Testcontainers. Frameworks: JUnit 5, Mockito, Spring Boot Test.
- frontend: a `*.spec.ts` next to each component, page and service, run with Vitest (jsdom, no browser needed).

## 2. Manual testing setup

1. `cp .env.example .env` and set a password, then `docker compose up -d`
2. `cd backend && ./mvnw spring-boot:run`, wait for `Started SafepawsApplication`
3. API is at `http://localhost:8080`, Swagger UI at `http://localhost:8080/swagger-ui.html`
4. frontend: `cd frontend && npm start -- --configuration production`, then open `http://localhost:4200`

Plain `npm start` uses **mock data** (6 fake announcements, no backend call). Use the production configuration above to test against the real API.

Test data comes from `V2__test_data.sql`: 14 announcements (8 LOST, 6 FOUND, 3 RESOLVED, 3 without coordinates, 6 without pet name) and 2 photos (Mitzi, id 1 and Rex, id 2). To reset it: `docker compose down -v`, then start again.

## 3. Test scenarios

Status: **ready** = can be run now, **blocked** = endpoint or page not implemented yet, scenario is written so it can run as soon as it lands.

Field rules for an announcement (from `AnnouncementRequest`):
required `type`, `species`, `eventDate`, `area`, `contactName`, `contactPhone`; `species` is `CAT`, `DOG` or `OTHER`; `eventDate` not in the future; `contactPhone` 7 to 15 digits with optional `+`; `latitude` and `longitude` both or none; max lengths: petName 50, breed 100, color 100, distinctiveMarks 500, area 150, contactName 100, contactPhone 20.

### A. Posting an announcement

Endpoint `POST /announcements`. Page `/announcements/new` is still a placeholder, so until the form exists these run through Swagger or curl. All error responses use the standard format (`status`, `error`, `message`, `fieldErrors`, `timestamp`).

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| A1 | all fields valid | post the full example from the API contract (LOST, CAT, Mitzi, coordinates, contact) | `201 Created`, `Location: /announcements/{id}`, body has the new `id`, `status: ACTIVE`, `createdAt` set, `photos: []`; the announcement appears first in `GET /announcements` | ready |
| A2 | only required fields | post `type`, `species`, `eventDate`, `area`, `contactName`, `contactPhone` | `201`, optional fields are `null` | ready |
| A3 | empty body | post `{}` | `400`, `message: Some fields are invalid`, `fieldErrors` lists all six required fields, sorted by field name | ready |
| A4 | blank required text | `area`, `contactName`, `contactPhone` set to `""` or spaces | `400`, one field error for each | ready |
| A5 | event date in the future | `eventDate` tomorrow | `400`, field error `eventDate: Event date cannot be in the future` | ready |
| A6 | invalid phone | `contactPhone` with letters, or 5 digits | `400`, field error on `contactPhone` | ready |
| A7 | value too long | `petName` with 51 characters | `400`, field error `Pet name must be at most 50 characters` | ready |
| A8 | half coordinates | `latitude` set, `longitude` missing | `400`, error `Latitude and longitude must be provided together` | ready |
| A9 | coordinates out of range | `latitude: 95` | `400`, field error on `latitude` | ready |
| A10 | unknown enum value | `species: "Cat"` (display name instead of enum) | `400`, message lists allowed values `CAT, DOG, OTHER` | ready |
| A11 | malformed JSON | body `{"type": ` | `400`, `message: Malformed request body`, empty `fieldErrors` | ready |
| A12 | wrong content type | send as `text/plain` | `415`, same error format | ready |
| A13 | form: happy path | fill every field in the UI, submit | redirected to the new announcement or the list, the card is there | blocked (form) |
| A14 | form: missing required | submit with `area` empty | field error shown next to the field, nothing sent to the backend or `400` shown clearly | blocked (form) |

### B. Photos

Endpoints `POST /announcements/{id}/photos` (multipart, field `files`, merged in PR #8) and `GET /photos/{id}` (not yet). Limits: only `image/jpeg` and `image/png`, max 5 MB per file (`413`), 25 MB per request, empty file and empty request rejected with `400`, one main photo per announcement.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| B1 | one photo | upload one jpeg to announcement 3 | `201`, list with one photo, `isMain: true`, `url: /photos/{id}` | ready |
| B2 | multiple photos | upload 3 files in one request | `201`, 3 photos, first `isMain: true`, others `false` | ready |
| B3 | second upload | upload again to an announcement that already has a main photo | new photos have `isMain: false`, main stays the same | ready |
| B4 | not an image | upload a `.pdf` or `.txt` | `400` with a clear message, nothing stored in `photo` | ready |
| B5 | too large | upload a 6 MB jpeg | rejected (`400` or `413`), standard error format, nothing stored | ready |
| B6 | unknown announcement | upload to `/announcements/999/photos` | `404` | ready |
| B7 | fetch photo | `GET /photos/1` (Mitzi) | `200`, `Content-Type: image/jpeg`, the image renders in `<img>` | blocked |
| B8 | unknown photo | `GET /photos/999` | `404` | blocked |
| B9 | photos in announcement | after B1, `GET /announcements/3` and `GET /announcements` | both include the photo list; the card shows the main photo instead of `No photo` | blocked |
| B10 | form upload | pick one, then several photos in the form | preview shown, after submit the card has the photo | blocked (form) |

### C. Announcements list

Endpoints `GET /announcements`, `GET /announcements/{id}`. Page `/announcements`.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| C1 | list with data | open `/announcements` with the test data | 14 cards, newest first (by `createdAt`); each card shows LOST or FOUND label, species, area, date, `Download poster`; cards without photo show `No photo` | ready |
| C2 | empty list | `DELETE FROM announcement` in psql, reload | API returns `[]`, page shows `There are no announcements yet.` (reset data afterwards) | ready |
| C3 | backend down | stop the backend, reload | page shows `Could not load the announcements. Please try again later.` | ready |
| C4 | one announcement | `GET /announcements/1` | `200`, all fields of Mitzi, `status: ACTIVE` | ready |
| C5 | unknown id | `GET /announcements/999` | `404`, `message: Announcement not found with id: 999` | ready |
| C6 | non numeric id | `GET /announcements/abc` | `400`, field error on `id` | ready |
| C7 | nav | click `Post announcement` in the header, then `SafePaws` | goes to the form page, then back to the list | ready |

### D. Poster

Endpoint `GET /announcements/{id}/poster`, button `Download poster` on each card.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| D1 | download | click `Download poster` on Mitzi | a PDF downloads, `Content-Type: application/pdf`, opens without errors | blocked |
| D2 | content | open the PDF | shows main photo, LOST or FOUND, species, pet name, color and distinctive marks, area, event date, contact name and phone, a QR code | blocked |
| D3 | QR code | scan the QR with a phone | opens the announcement page for that id | blocked |
| D4 | no photo | poster for an announcement without photos (id 3) | still generates, placeholder instead of photo | blocked |
| D5 | no pet name | poster for a FOUND announcement without name | no empty `Name:` line and no `null` in the PDF | blocked |
| D6 | unknown id | `GET /announcements/999/poster` | `404` in the standard error format | blocked |

## 4. Known issues (3 Nov)

- `docs/api.md` describes a different API (`/api/v1`, JWT, `/pets`). The API contract from the team rules is the correct one.
- `docs/usage-guide.md` mentions `application.yml` and creating the database by hand; the real setup is `application.properties` and `docker compose`.
- `contactPhone` is returned in every response. Accepted for now, to be hidden once messaging exists.

## 5. Reporting bugs

One Trello card per bug, label `bug`, with: what you did (steps), what you expected, what happened, branch or commit. Attach the response body or a screenshot. Link the scenario ID from this file when there is one.
