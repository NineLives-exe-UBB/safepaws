# Testing

How SafePaws is tested: automated tests, manual test scenarios and how to report bugs.

## 1. Automated tests

Both parts fail the build when coverage is under 70%.

| Part | Command | What it needs | Last run (11 Nov) |
|---|---|---|---|
| backend | `cd backend && ./mvnw clean verify` | Docker running (the repository and migration tests start a PostgreSQL with Testcontainers) | 115 tests, 97.1% instructions |
| frontend | `cd frontend && npm run test:coverage` | `npm ci` after every pull that changes `package-lock.json` | 170 tests, 88.2% statements |

The test counts are the numbers the runners report, including each case of a parameterized test. Coverage reports: `backend/target/site/jacoco/index.html` and `frontend/coverage/index.html`.

The backend threshold is in `pom.xml` (JaCoCo instruction coverage, `verify` phase). The frontend threshold is in `angular.json` (`coverageThresholds`: statements, branches, functions and lines) and applies to the total, not per file. The form page is the weakest file (73.6% statements, 69.7% branches, 65.7% functions), so new form code without tests can pull the total down quickly.

All CI jobs (backend tests, frontend tests and production build, secret scan, Flyway migration guard) run on every pull request through GitHub Actions (`.github/workflows/ci.yml`) and are required before merging into `main`.

Where the tests live:

- backend: `backend/src/test/java/.../safepaws/`, same tree as `main`. Frameworks: JUnit 5, Mockito, Spring Boot Test, Testcontainers.
  - `controller/`: `@WebMvcTest` slice tests (status codes, JSON, validation, query parameters) with the services mocked.
  - `service/`: unit tests with mocked repositories (announcements, filters and distances, photos, poster).
  - `dto/`: plain unit tests for the `GET /announcements` filter rules (`AnnouncementFilterTest`).
  - `repository/`: `@DataJpaTest` tests against a real PostgreSQL started by Testcontainers: the repository, the filter specifications (`AnnouncementSpecificationsTest`) and the migrations (`SearchSupportMigrationTest`, `DemoDataMigrationTest`, `DemoPhotosMigrationTest`). The demo data tests fail if a migration adds an incomplete announcement or one without exactly one main photo.
- frontend: a `*.spec.ts` next to each component, page, service and the filters model, run with Vitest (jsdom, no browser needed).

## 2. Manual testing setup

1. `cp .env.example .env` and set a password, then `docker compose up -d`
2. `cd backend && ./mvnw spring-boot:run`, wait for `Started SafepawsApplication`
3. API is at `http://localhost:8080`, Swagger UI at `http://localhost:8080/swagger-ui.html`
4. frontend: `cd frontend && npm start`, then open `http://localhost:4200`

The frontend connects directly to the backend at `http://localhost:8080`. If port 8080 is taken, start the backend with `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8099` for API testing only; UI testing needs the backend on 8080.

Test data comes from the Flyway migrations (`V2` to `V5`, see `database.md`): 22 announcements (12 LOST, 10 FOUND; 18 ACTIVE, 4 RESOLVED; 10 without a pet name; all with coordinates) in 8 cities (Cluj-Napoca and nearby, Brașov, Timișoara, Iași, Sibiu, Oradea), and 25 photos. Every announcement has exactly one main photo; Mitzi and Rex keep photo ids 1 and 2, and announcements 9, 13 and 21 have 2 photos each (gallery). `Iosefin, Timisoara` and `Tatarasi, Iasi` are written without diacritics on purpose, for the search. The dates of the announcements added in `V3` depend on when the migration ran, so scenarios don't use exact dates. To test an announcement without photos or without coordinates, create one through the API. To reset the data: `docker compose down -v`, then start again.

## 3. Test scenarios

Status: **ready** = can be run now, **blocked** = feature not on `main` yet; the scenario is written so it can run as soon as it lands. The owner is who to review the scenario with.

Field rules for an announcement (from `AnnouncementRequest`):
required `type`, `species`, `eventDate`, `area`, `contactName`, `contactPhone`; `species` is `CAT`, `DOG` or `OTHER`; `eventDate` not in the future; `contactPhone` 7 to 15 digits with optional `+`; `latitude` and `longitude` both or none; max lengths: petName 50, breed 100, color 100, distinctiveMarks 500, area 150, contactName 100, contactPhone 20. The form additionally requires the location.

### A. Posting an announcement (API)

Endpoint `POST /announcements`, through Swagger or curl. Owner: Antonia. The form is in section I. All error responses use the standard format (`status`, `error`, `message`, `fieldErrors`, `timestamp`).

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| A1 | all fields valid | post the full example from the API contract (LOST, CAT, Mitzi, coordinates, contact) | `201 Created`, `Location: /announcements/{id}`, body has the new `id`, `status: ACTIVE`, `createdAt` set, `photos: []`; the announcement appears in `GET /announcements` (position by `eventDate`, newest first) | ready |
| A2 | only required fields | post `type`, `species`, `eventDate`, `area`, `contactName`, `contactPhone` | `201`, optional fields are `null`, no coordinates | ready |
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
| A13 | special characters | `color: "Black & white"`, `breed: "2 white paws"`, `petName: "Max 2"` | `201`, values stored as sent | ready |

### B. Photos (API)

Endpoints `POST /announcements/{id}/photos` (multipart, field `files`) and `GET /photos/{id}`. Owner: Patrick. Limits: only `image/jpeg` and `image/png`, max 5 MB per file (`413`), 25 MB per request, empty file and empty request rejected with `400`, one main photo per announcement. Upload from the form is in section I.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| B1 | one photo | create an announcement through the API (it has no photos), upload one jpeg to it | `201`, list with one photo, `isMain: true`, `url: /photos/{id}` | ready |
| B2 | multiple photos | upload 3 files in one request to a new announcement | `201`, 3 photos, first `isMain: true`, others `false` | ready |
| B3 | second upload | upload again to an announcement that already has a main photo | new photos have `isMain: false`, main stays the same | ready |
| B4 | not an image | upload a `.pdf`, and a `.txt` sent as `image/jpeg` | `400` with a clear message, nothing stored | ready |
| B5 | too large | upload a 6 MB jpeg | `413`, standard error format, nothing stored | ready |
| B6 | empty file | upload a 0 byte file | `400` | ready |
| B7 | unknown announcement | upload to `/announcements/99999/photos` | `404` | ready |
| B8 | fetch photo | `GET /photos/1` (Mitzi) | `200`, `Content-Type: image/jpeg`, the image renders in `<img>` | ready |
| B9 | unknown photo | `GET /photos/99999` | `404` | ready |
| B10 | photos in announcement | after B1, `GET /announcements/{id}` and `GET /announcements` | both include the photo list; the card shows the main photo instead of `No photo` | ready |

### C. Announcements list

Page `/announcements` (`/` redirects there), endpoints `GET /announcements`, `GET /announcements/{id}`. Owner: Luca.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| C1 | list with data | open `/announcements` on a fresh database | 18 cards (only ACTIVE by default), newest event date first, so the order matches the date on each card; each card shows the main photo, LOST or FOUND label, species, pet name when there is one, area, date, `Download poster`; a card without photos (create one through the API) shows `No photo` | ready |
| C2 | empty list | `DELETE FROM announcement` in psql, reload | API returns `[]`, page shows `There are no announcements yet.` (reset data afterwards) | ready |
| C3 | backend down | stop the backend, reload | page shows `Could not load the announcements. Please try again later.` | ready |
| C4 | one announcement | `GET /announcements/1` | `200`, all fields of Mitzi, `status: ACTIVE` | ready |
| C5 | unknown id | `GET /announcements/99999` | `404`, `message: Announcement not found with id: 99999` | ready |
| C6 | non numeric id | `GET /announcements/abc` | `400`, field error on `id` | ready |
| C7 | nav | click `Post announcement`, `Map`, `Announcements` and `SafePaws` in the header | goes to the form, the map page, the list and the list; the current link is highlighted | ready |
| C8 | new post shows up | post an announcement through the form, go back to the list | the new card is there without a manual reload, at the right place for its event date | ready |

### D. Poster

Endpoint `GET /announcements/{id}/poster` (file `announcement-{id}-poster.pdf`), button `Download poster` on each card. Owner: Alexandru.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| D1 | download | click `Download poster` on Mitzi | a PDF downloads, `Content-Type: application/pdf`, opens without errors, one A4 page | ready |
| D2 | content | open the PDF | LOST or FOUND banner, main photo, name, species as text (`Cat`, not `CAT`), breed, color, marks, date, area, contact name and phone, QR box with `Scan for updates` | ready |
| D3 | no photo | create an announcement through the API (it has no photos), download its poster | still generates, grey `No photo available` box instead of a gap | ready |
| D4 | missing fields | poster for an announcement with only the required fields, and for a FOUND one without a name | no `Name`, `Breed`, `Color` rows, no `null` or `N/A` anywhere | ready |
| D5 | long text | `distinctiveMarks` with 450 characters | everything stays on one A4 page, the text does not overflow its row | ready |
| D6 | resolved | poster for a RESOLVED announcement (e.g. Luna) | clear `RESOLVED` mark under the banner | ready |
| D7 | diacritics | posters for Mănăștur, Florești and Brașov announcements | `ă`, `ș`, `ț` print correctly, no boxes or `?` | ready |
| D8 | photo shapes | posters for announcements with portrait and landscape photos (demo photos are 500 or 960 px wide) | the photo fits the page without being stretched and prints clearly | ready |
| D9 | unknown id | `GET /announcements/99999/poster` | `404` in the standard error format | ready |
| D10 | QR on poster | open a poster | a real QR code instead of the `QR CODE` placeholder | blocked (Patrick, QR) |
| D11 | QR scan | print the poster, scan from 2 to 3 meters with a phone on the same Wi-Fi | opens the announcement page for that id | blocked (Patrick, QR; Vanessza, details page) |

### E. Filters

`GET /announcements` query parameters `type`, `species`, `status`, combined with AND (see `api.md`). Filter bar on `/announcements` and `/map`. Owners: Antonia (API), Luca (filter bar).

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| E1 | default | no parameters | only ACTIVE, newest event date first | ready |
| E2 | type | `type=LOST`, then `type=FOUND` | only that type; the two together give the full active list | ready |
| E3 | species | `species=CAT`, `DOG`, `OTHER` | only that species, each one not empty | ready |
| E4 | status | choose `Resolved` in the status dropdown | only the 4 resolved announcements, the URL has `?status=RESOLVED` | ready |
| E5 | combination | `type=LOST&species=CAT&area=cluj` | every result matches all three | ready |
| E6 | invalid value | `type=lost`, `species=HAMSTER`, `status=DELETED` | `400` with the allowed values (enum values are case sensitive) | ready |
| E7 | filter bar | pick Type, Species, Status in the bar | list updates, URL gets the parameters | ready |
| E8 | reload and share | reload or open a filtered URL, e.g. `/announcements?species=CAT&color=grey` | the same filters are selected and applied | ready |
| E9 | no match | filter until nothing matches | `No announcements match your filters`, with a `Clear filters` button that removes the filters and the URL parameters | ready |

### F. Text search

Parameters `color`, `breed`, `area`: part of the text, ignoring case, diacritics and spaces at the ends. Owners: Antonia (API), Luca (filter bar).

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| F1 | case | `color=grey`, `Grey`, `GREY`, `%20grey%20` | the same results, not empty | ready |
| F2 | partial | `color=gre` | at least the grey ones | ready |
| F3 | diacritics | `area=manastur` | finds `Mănăștur, Cluj-Napoca` | ready |
| F4 | diacritics both ways | `area=MĂNĂȘTUR`; `area=timișoara`; `area=iași` | same as F3; also finds `Iosefin, Timisoara` and `Tatarasi, Iasi` (no diacritics in the data) | ready |
| F5 | other city | `area=brasov` | only Brașov announcements | ready |
| F6 | breed | `breed=TERRIER` | case ignored | ready |
| F7 | spelling | `color=gray` | does not find `grey` (different word, expected) | ready |
| F8 | no results | `area=zzzznotacity` | `200`, `[]` | ready |
| F9 | blank | `color=%20%20&area=` | treated as not sent, full active list | ready |
| F10 | too long | `color` 101 chars, `area` 151 chars | `400`, field error on that parameter | ready |
| F11 | wildcards | `area=%25` and `area=_` | literal, no results (not "match everything") | ready |
| F12 | search in the bar | type `grey` in Color, then `manastur` in Area | after a short pause (300 ms) only matching cards show; list and URL update | ready |

### G. Sort and distance

Parameters `sort` (`newest`, `oldest`, `closest`), `lat`, `lng`, `radiusKm` (1 to 100, default 10). With a location, results get `distanceKm` and announcements without coordinates are left out. Owners: Antonia (API), Luca (filter bar).

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| G1 | oldest | `sort=oldest` | ascending event date | ready |
| G2 | invalid sort | `sort=random`, `sort=NEWEST` | `400`, `Invalid value 'random', allowed values: newest, oldest, closest` (values are lowercase only) | ready |
| G3 | closest without location | `sort=closest` | `400`, `sort=closest requires lat and lng` | ready |
| G4 | distance | `lat=46.7712&lng=23.6236&sort=closest` | every result has `distanceKm <= 10`, ascending, only Cluj area | ready |
| G5 | radius | same location with `radiusKm=1`, default, `radiusKm=100` | result count grows with the radius | ready |
| G6 | invalid radius | `radiusKm=0`, `101`, `-5`; `radiusKm=5` without lat/lng | `400`, `radiusKm must be between 1 and 100` or `radiusKm requires lat and lng` | ready |
| G7 | invalid location | `lat=46.7` without `lng`; `lat=95`; `lat=abc` | `400` in the standard format, e.g. `Latitude and longitude must be provided together` | ready |
| G8 | other city | `lat=45.6427&lng=25.5887&radiusKm=20` (Brașov) | only Brașov announcements | ready |
| G9 | nowhere | `lat=44&lng=31&radiusKm=100` (Black Sea) | `[]` | ready |
| G10 | no coordinates | create one without coordinates (A2); list without and with `lat`/`lng` | shown without location, left out with location | ready |
| G11 | combination | location + `radiusKm=50` + `type=LOST` + `species=DOG` + `sort=closest` | all filters apply, ascending distance | ready |
| G12 | closest first (UI) | choose `Closest first` and allow the location | cards ordered by distance, each shows `x.x km away` | ready |
| G13 | radius (UI) | choose `Within 5 km` and allow the location | only announcements within 5 km show, with their distance | ready |
| G14 | location refused (UI) | refuse the location in the browser | radius and `Closest first` disabled, `Allow location access to see announcements near you` shown, the rest of the list works | ready |

### H. Map page

Page `/map`. Owner: Luca.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| H1 | open | click `Map` in the header | the `Map` link is highlighted, the tab title is `Map \| SafePaws` | ready |
| H2 | pins | open `/map` on a fresh database | one pin per active announcement, red for LOST, green for FOUND | ready |
| H3 | location allowed | allow the location | the map starts centered around you | ready |
| H4 | location refused | refuse the location | the map zooms so all the pins fit (all of Romania, not only Cluj), the filter bar shows the location message | ready |
| H5 | popup | click a pin | popup with the main photo, the pet name (or the species), the area, the date and `View details` | ready |
| H6 | filter | choose `Found` in the filter bar, then a species and a search | only matching pins stay, the map fits them | ready |
| H7 | same results | open `/map?species=CAT` and `/announcements?species=CAT` | the same announcements on both pages | ready |
| H8 | no results | filters with no results | no pins, a clear message, no error | ready |
| H9 | no coordinates | create an announcement through the API without coordinates, open `/map` | no pin for it, the note `1 announcement has no location and is not on the map.` | ready |
| H10 | backend down | stop the backend, open `/map` | error message, no crash | ready |
| H11 | details link | click `View details` in a popup | opens the announcement details page | blocked (Vanessza, details page; today it goes back to the list) |

### I. Form, photo upload and location picker

Page `/announcements/new`. Owner: Victor.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| I1 | happy path | fill every field, pick the location on the map, add 2 photos, submit | `Your announcement and photos were uploaded successfully!`, the card is in the list with its main photo, the pin is on the map | ready |
| I2 | without photos | same as I1 without photos | `Your announcement was created successfully!` with a `View announcements` link | ready |
| I3 | missing required | submit with area empty and no location picked | `Location is required.` and `Please select a location on the map before submitting.`, an error next to each empty field, nothing sent | ready |
| I4 | invalid values | date and time in the future, phone `12ab` | `Date and time cannot be in the future.`, `Enter a valid phone number (7–15 digits, optionally starting with +).`, nothing sent | ready |
| I5 | location picker | click on the map, then click somewhere else | `Selected location` updates, one pin only | ready |
| I6 | location allowed | allow location | map centered on the user | ready |
| I7 | location refused | block location | map centered on Romania, picking still works | ready |
| I8 | date picker | open the date picker | future dates cannot be picked | ready |
| I9 | photos | pick one, then several photos | previews shown, the first is labeled `Main photo`, `Remove` takes a photo out; after submit the first is the main one | ready |
| I10 | wrong file | pick a `.pdf` or a 6 MB photo | clear error; if the announcement was created, `Your announcement was created, but the photos could not be uploaded` | ready |
| I11 | special characters | color `Black & white`, breed `2 white paws`, name `Max 2` | accepted, like the backend (A13) | ready |
| I12 | switch type | fill some fields, switch Lost to Found | values are kept | ready |
| I13 | disabled while sending | submit with large photos, try to edit during `Submitting...` | whole form disabled | ready |
| I14 | backend field errors | submit something only the backend rejects | each error shown next to its field | ready |
| I15 | redirect | submit a valid announcement | goes to the new announcement's page | blocked (Vanessza, details page) |

### J. Announcement details page

Page `/announcements/:id`. Owner: Vanessza.

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| J1 | open from list | click a card (anywhere on it) | details page, title has the pet name or species | blocked |
| J2 | gallery | open announcement 9, 13 or 21 | main photo large, the other as a clickable thumbnail | blocked |
| J3 | details | open Mitzi | readable labels (`Cat`, `Lost`), empty fields hidden, no `null` | blocked |
| J4 | contact | tap the phone on a phone | `tel:` link starts a call | blocked |
| J5 | map | open any announcement | small map with its pin | blocked |
| J6 | poster | click `Download poster` | same PDF as D1 | blocked |
| J7 | not found | open `/announcements/99999` | friendly page with a link back to the list | blocked |
| J8 | resolved badge | open a RESOLVED announcement | `RESOLVED` badge | blocked |

### K. Resolved status

Endpoint `PATCH /announcements/{id}/status`, body `{ "status": "RESOLVED" }` or `ACTIVE`. Owners: Patrick (API), Vanessza (button).

| ID | Scenario | Steps | Expected | Status |
|---|---|---|---|---|
| K1 | resolve | `PATCH` with `RESOLVED` | `200`; `GET /announcements/{id}` shows `RESOLVED`; it leaves the default list and the map | blocked |
| K2 | back to active | `PATCH` with `ACTIVE` | back in the default list | blocked |
| K3 | unknown id | `PATCH /announcements/99999/status` | `404`, standard format | blocked |
| K4 | invalid status | body `{ "status": "DONE" }` or `{}` | `400`, standard format | blocked |
| K5 | button | `Mark as resolved` on the details page, then `Mark as active` | badge and list update | blocked |
| K6 | poster after resolve | resolve, download the poster | `RESOLVED` mark (D6) | blocked |

## 4. Last run

Run on 11 Nov 2026 on `main` (`6ab521e`), fresh database (all 5 migrations applied from empty), backend through the API (curl), both test suites. The UI scenarios (C1 to C3, C7, C8, E4, E7 to E9, F12, G12 to G14, H, I) are manual and were not run in this pass.

| Area | Result |
|---|---|
| backend suite | pass, 115 tests, 97.1% |
| frontend suite | pass, 170 tests, 88.2% |
| A, B, C4 to C6 | all pass |
| D1 to D7, D9 | all pass (checked the text and layout of the PDFs) |
| E1 to E3, E5, E6, F1 to F11, G1 to G11 | all pass |
| D10, D11, H11, I15, J, K | blocked, not on `main` |

Findings from this run:

- D5: long distinctive marks are cut after about 150 characters, in the middle of a word (`text th...`), although a quarter of the page is still empty. The poster stays on one page, but part of the description is lost.
- D10: the QR box prints the placeholder text `QR CODE` until Patrick's QR service is on `main`. Do not show a printed poster in the demo before that.
- G2, E6: `sort=NEWEST` and `type=lost` are rejected. This is intended (the values are case sensitive), while the search parameters ignore case, so the frontend must always send lowercase `sort` and uppercase enums.
- I11: the form only accepts letters in pet name, breed and color, so it rejects `Black & white`, `2 white paws` and `Max 2`, which the backend accepts (Victor's form fixes card).

## 5. Known issues

- `contactName` and `contactPhone` are returned by `GET /announcements/{id}` (and in the `POST /announcements` response) to anyone, because there is no authentication. They are not in the list response.
- `View details` in a map popup links to `/announcements/:id`, which has no page yet, so it goes back to the list.
- The poster has no QR code yet (`PosterService` passes no QR image), so D10 and D11 stay blocked.

## 6. Reporting bugs

One Trello card per bug, label `bug`, with: what you did (steps), what you expected, what happened, branch or commit. Attach the response body or a screenshot. Link the scenario ID from this file when there is one.
