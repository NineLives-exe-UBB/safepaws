# Testing

How SafePaws is tested: automated tests, manual test scenarios and how to report bugs.

## 1. Automated tests

Both parts fail the build when coverage is under 70%.

| Part | Command | What it needs | Tests |
|---|---|---|---|
| backend | `cd backend && ./mvnw verify` | Docker running (the repository and migration tests start a PostgreSQL with Testcontainers) | 115 (110 test methods; one parameterized test runs 6 cases) |
| frontend | `cd frontend && npm run test:coverage` | `npm install` done once | 190 (184 tests; one `it.each` runs 7 cases) |

The test counts are the number of tests the runners report, including each case of a parameterized test. They were counted from the test files. The coverage percentages are printed by the commands above (backend report: `backend/target/site/jacoco/index.html`, frontend report: `frontend/coverage/index.html`).

The backend threshold is in `pom.xml` (JaCoCo instruction coverage, `verify` phase). The frontend threshold is in `angular.json` (`coverageThresholds`: statements, branches, functions and lines). Both run on every pull request through GitHub Actions (`.github/workflows/ci.yml`), together with a frontend production build, a secret scan and a Flyway migration guard.

Where the tests live:

- backend: `backend/src/test/java/.../safepaws/`, same tree as `main`. Frameworks: JUnit 5, Mockito, Spring Boot Test, Testcontainers.
  - `controller/`: `@WebMvcTest` slice tests (status codes, JSON, validation, query parameters) with the services mocked.
  - `service/`: unit tests with mocked repositories (announcements, filters and distances, photos, poster).
  - `dto/`: plain unit tests for the `GET /announcements` filter rules (`AnnouncementFilterTest`).
  - `repository/`: `@DataJpaTest` tests against a real PostgreSQL started by Testcontainers: the repository, the filter specifications (`AnnouncementSpecificationsTest`) and the migrations (`SearchSupportMigrationTest`, `DemoDataMigrationTest`, `DemoPhotosMigrationTest`).
- frontend: a `*.spec.ts` next to each component, page, service and the filters model, run with Vitest (jsdom, no browser needed).

## 2. Manual testing setup

1. `cp .env.example .env` and set a password, then `docker compose up -d`
2. `cd backend && ./mvnw spring-boot:run`, wait for `Started SafepawsApplication`
3. API is at `http://localhost:8080`, Swagger UI at `http://localhost:8080/swagger-ui.html`
4. frontend: `cd frontend && npm start`, then open `http://localhost:4200`

The frontend connects directly to the backend at `http://localhost:8080`.

Test data comes from the Flyway migrations (`V2` to `V5`, see `database.md`): 22 announcements (12 LOST, 10 FOUND; 18 ACTIVE, 4 RESOLVED; 10 without a pet name; all with coordinates) and 25 photos. Every announcement has a main photo; Mitzi and Rex keep photo ids 1 and 2, and announcements 9, 13 and 21 have 2 photos each. The dates of the announcements added in `V3` depend on when the migration ran, so scenarios don't use exact dates. To test an announcement without photos or without coordinates, create one through the API. To reset the data: `docker compose down -v`, then start again.

## 3. Test scenarios

Status: **ready** = can be run now, **blocked** = feature not implemented yet, scenario is written so it can run as soon as it lands.

Field rules for an announcement (from `AnnouncementRequest`):
required `type`, `species`, `eventDate`, `area`, `contactName`, `contactPhone`; `species` is `CAT`, `DOG` or `OTHER`; `eventDate` not in the future; `contactPhone` 7 to 15 digits with optional `+`; `latitude` and `longitude` both or none; max lengths: petName 50, breed 100, color 100, distinctiveMarks 500, area 150, contactName 100, contactPhone 20.

### A. Posting an announcement

Endpoint `POST /announcements` (A1 to A12, through Swagger or curl), page `/announcements/new` (A13 to A15). All error responses use the standard format (`status`, `error`, `message`, `fieldErrors`, `timestamp`).

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| A1 | all fields valid | post the full example from the API contract (LOST, CAT, Mitzi, coordinates, contact) | `201 Created`, `Location: /announcements/{id}`, body has the new `id`, `status: ACTIVE`, `createdAt` set, `photos: []`; the announcement appears in `GET /announcements` (position by `eventDate`, newest first) | ready |
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
| A13 | form: happy path | fill every field, pick the location on the map, submit | `Your announcement was created successfully!` with a `View announcements` link, then redirected to `/announcements`, the card is there | ready |
| A14 | form: missing required | submit with `area` empty and no location picked on the map | `Location is required.` and `Please select a location on the map before submitting.` shown, nothing sent to the backend | ready |
| A15 | form: invalid values | pet name with digits, date and time in the future, phone `12ab` | `Pet name can contain only letters, spaces, apostrophes and hyphens.`, `Date and time cannot be in the future.`, `Enter a valid phone number (7–15 digits, optionally starting with +).`, nothing sent | ready |

### B. Photos

Endpoints `POST /announcements/{id}/photos` (multipart, field `files`) and `GET /photos/{id}`. Limits: only `image/jpeg` and `image/png`, max 5 MB per file (`413`), 25 MB per request, empty file and empty request rejected with `400`, one main photo per announcement.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| B1 | one photo | create an announcement through the API (it has no photos), upload one jpeg to it | `201`, list with one photo, `isMain: true`, `url: /photos/{id}` | ready |
| B2 | multiple photos | upload 3 files in one request | `201`, 3 photos, first `isMain: true`, others `false` | ready |
| B3 | second upload | upload again to an announcement that already has a main photo | new photos have `isMain: false`, main stays the same | ready |
| B4 | not an image | upload a `.pdf` or `.txt` | `400` with a clear message, nothing stored in `photo` | ready |
| B5 | too large | upload a 6 MB jpeg | rejected (`413`), standard error format, nothing stored | ready |
| B6 | unknown announcement | upload to `/announcements/999/photos` | `404` | ready |
| B7 | fetch photo | `GET /photos/1` (Mitzi) | `200`, `Content-Type: image/jpeg`, the image renders in `<img>` | ready |
| B8 | unknown photo | `GET /photos/999` | `404` | ready |
| B9 | photos in announcement | after B1, `GET /announcements/{id}` and `GET /announcements` | both include the photo list; the card shows the main photo instead of `No photo` | ready |
| B10 | form upload | pick one, then several photos in the form | previews shown, the first is labeled `Main photo`, `Remove` takes a photo out; after submit `Your announcement and photos were uploaded successfully!` and the card shows the main photo | ready |

### C. Announcements list

Endpoints `GET /announcements`, `GET /announcements/{id}`. Page `/announcements` (`/` redirects there).

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| C1 | list with data | open `/announcements` with the test data | 18 cards (only ACTIVE by default), newest event date first, so the order matches the date on each card; each card shows the main photo, LOST or FOUND label, species, pet name when there is one, area, date, `Download poster`; a card without photos (create one through the API) shows `No photo` | ready |
| C2 | empty list | `DELETE FROM announcement` in psql, reload | API returns `[]`, page shows `There are no announcements yet.` (reset data afterwards) | ready |
| C3 | backend down | stop the backend, reload | page shows `Could not load the announcements. Please try again later.` | ready |
| C4 | one announcement | `GET /announcements/1` | `200`, all fields of Mitzi, `status: ACTIVE` | ready |
| C5 | unknown id | `GET /announcements/999` | `404`, `message: Announcement not found with id: 999` | ready |
| C6 | non numeric id | `GET /announcements/abc` | `400`, field error on `id` | ready |
| C7 | nav | click `Post announcement`, `Map`, `Announcements` and `SafePaws` in the header | goes to the form, the map page, the list and the list; the current link is highlighted | ready |

### D. Poster

Endpoint `GET /announcements/{id}/poster`, button `Download poster` on each card.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| D1 | download | click `Download poster` on Mitzi | a PDF downloads, `Content-Type: application/pdf`, opens without errors | ready |
| D2 | content | open the PDF | shows main photo, LOST or FOUND, species, pet name, color and distinctive marks, area, event date, contact name and phone, a QR code | blocked |
| D3 | QR code | scan the QR with a phone | opens the announcement page for that id | blocked |
| D4 | no photo | poster for an announcement without photos (create one through the API) | still generates, without a photo | ready |
| D7 | photo shapes | posters for announcements with portrait and landscape photos (demo photos are 500 or 960 px wide) | the photo fits the page without being stretched and prints clearly | ready |
| D5 | no pet name | poster for a FOUND announcement without name | no empty `Name:` line and no `null` in the PDF | ready |
| D6 | unknown id | `GET /announcements/999/poster` | `404` in the standard error format | ready |

### E. Filters and sorting

Endpoint `GET /announcements` with query parameters (see `api.md`), filter bar on `/announcements` and `/map`.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| E1 | status | choose `Resolved` in the status dropdown | only resolved announcements show (4 with the test data), the URL has `?status=RESOLVED` | ready |
| E2 | text filter | type `grey` in Color | after a short pause (300 ms) only cards whose color contains `grey` show, whatever the case; `Gray` is not found (different word) | ready |
| E3 | diacritics | type `manastur` in Area | announcements in `Mănăștur` show | ready |
| E4 | closest first | choose `Closest first` and allow the location | cards ordered by distance, each shows `x.x km away` | ready |
| E5 | radius | choose `Within 5 km` and allow the location | only announcements within 5 km show, with their distance | ready |
| E6 | location refused | refuse the location in the browser | the radius and `Closest first` are disabled, `Allow location access to see announcements near you` is shown | ready |
| E7 | no match | filter until nothing matches | `No announcements match your filters`, with a `Clear filters` button that removes the filters | ready |
| E8 | reload | reload or share a filtered URL, for example `/announcements?species=CAT&color=grey` | the same filters are selected and applied | ready |
| E9 | invalid filters | `GET /announcements?radiusKm=5`, `?sort=closest`, `?sort=random`, `?lat=46.7` | `400`, field errors `radiusKm requires lat and lng`, `sort=closest requires lat and lng`, `Invalid value 'random', allowed values: newest, oldest, closest`, `Latitude and longitude must be provided together` | ready |

### F. Map page

Page `/map`.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| F1 | open | click `Map` in the header | the `Map` link is highlighted, the tab title is `Map \| SafePaws` | ready |
| F2 | location allowed | allow the location | the map starts centered around you | ready |
| F3 | location refused | refuse the location | the map zooms so all the pins fit, the filter bar shows the location message | ready |
| F4 | popup | click a pin | popup with the main photo, the pet name (or the species), the area, the date and `View details` | ready |
| F5 | filter | choose `Found` in the filter bar | only green pins stay (red is LOST, green is FOUND) | ready |
| F6 | no coordinates | create an announcement through the API without coordinates, open `/map` | the note `1 announcement has no location and is not on the map.` is shown | ready |
| F7 | same results | open `/map?species=CAT` and `/announcements?species=CAT` | the same announcements on both pages | ready |
| F8 | details link | click `View details` in a popup | opens the announcement details page | blocked (no details page, the link goes back to the list) |

## 4. Known issues

- `contactName` and `contactPhone` are returned by `GET /announcements/{id}` (and in the `POST /announcements` response) to anyone, because there is no authentication. They are not in the list response.
- `View details` in a map popup links to `/announcements/:id`, which has no page yet, so it goes back to the list.
- The poster has no QR code yet (`PosterService` passes no QR image), so D2 and D3 stay blocked.

## 5. Reporting bugs

One Trello card per bug, label `bug`, with: what you did (steps), what you expected, what happened, branch or commit. Attach the response body or a screenshot. Link the scenario ID from this file when there is one.
