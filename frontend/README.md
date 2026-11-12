# Frontend

This folder contains the frontend of SafePaws, built with Angular.

It handles the user interface: posting announcements, viewing them in a list, on the map and on their own page, marking them as resolved, and downloading the printable posters that the backend generates.

## How to run

### Requirements

- Node.js 22.22.3+ or 24.15.0+ (the versions Angular 22 supports) and npm

### Steps

1. Go to the frontend folder and install the dependencies:

   ```bash
   cd frontend
   npm install
   ```

2. Start the development server:

   ```bash
   npm start
   ```

3. Open http://localhost:4200 in the browser.

The pages need the backend running on port 8080 (see `backend/README.md`: copy `.env.example` to `.env`, run `docker compose up -d`, then `./mvnw spring-boot:run` in `backend/`). It comes with test announcements, so the list is not empty. Without the backend, the list page shows "Could not load the announcements. Please try again later."

The backend URL is set in `src/environments/environment.ts` (and `environment.development.ts` for `npm start`): port 8080 on the same host the page was opened from, so `http://localhost:8080` on the laptop.

### Opening the app from a phone

Start the development server so other devices on the same Wi-Fi can reach it:

```bash
npm start -- --host 0.0.0.0
```

Then open `http://<laptop IP>:4200` on the phone. The backend also needs the laptop IP in `.env` (`CORS_ALLOWED_ORIGINS` and `FRONTEND_URL`); the steps are in `docs/usage-guide.md`. Browsers only give the location on HTTPS or `localhost`, so "Closest first" and the radius filter only work on the laptop.

### Pages

| Route                | Page                                                      |
| -------------------- | --------------------------------------------------------- |
| `/announcements`     | List of announcements (`/` and unknown addresses go here) |
| `/announcements/new` | Form for posting a pet                                    |
| `/announcements/:id` | Details of one announcement                               |
| `/map`               | Map with a pin for each announcement                      |

### Tests and coverage

```bash
npm test                # run the tests in watch mode
npm run test:coverage   # run the tests once, with the coverage report
```

`npm run test:coverage` fails if the coverage is under 70% (statements, branches, functions or lines). The report is printed in the terminal, and the detailed version is in `coverage/index.html`.

### Build

```bash
npm run build
```

The result is in `dist/frontend`.

## Details page

`/announcements/:id` shows one announcement. It opens from a card in the list (the whole card is clickable), from "View details" on a map pin, from the QR code on a poster, and after posting the form.

- **Gallery:** the main photo first, shown whole (portrait and landscape photos are not cropped), and the other photos as thumbnails.
- **Details:** only the fields that are filled in, the contact with a `tel:` link, a small map with the pin, and "Download poster".
- **Status:** "Mark as resolved" and "Mark as active" call `PATCH /announcements/{id}/status` and show the announcement that the backend returns.
- **Not found:** an id that is not a positive whole number, or a 404 from the backend, shows "Announcement not found" with a link back to the list.

## Map page

`/map` shows a pin for each announcement that matches the filters, red for LOST and green for FOUND. It uses the same filter bar as the list, so both pages show the same results for the same filters (`/map?species=CAT` works like `/announcements?species=CAT`).

- **Start:** if the browser location is allowed, the map is centered on the user. If it is refused, it zooms so that all the pins fit on the screen.
- **Popup:** clicking a pin shows the main photo, the pet name (or the species), the area, the date and a "View details" link to `/announcements/:id`.
- **No position:** announcements without coordinates cannot get a pin, and the page says how many there are.
- **Shared code:** `AnnouncementSearch` (`services/announcement-search.ts`) reads the filters from the URL, asks for the location and loads the announcements. Each page that shows announcements provides its own instance.

## Filters

The list page has a filter bar (`<app-filter-bar>`, in `components/filter-bar`) that other pages, like the map page, can reuse:

```html
<app-filter-bar
  [filters]="filters"
  [locationStatus]="locationStatus"
  (filtersChange)="onFiltersChange($event)"
/>
```

- **Filters:** type, species and status (dropdowns), color, breed and area (text), radius and sort. The text fields wait 300 ms after the user stops typing.
- **Defaults:** the same as the backend. Without any filter it returns only active announcements, newest event first, so the status dropdown starts on "Active" and has no "Any" option.
- **In the URL:** only the values that are not the default, for example `/announcements?species=CAT&color=grey`, so they survive a refresh and can be shared. `fromQueryParams` and `toQueryParams` in `models/announcement-filters.model.ts` read and write them.
- **Location:** the page asks the browser for the user's location (`LocationService`). If it is refused, the radius and "Closest first" are disabled, with a short message. The location is only sent to the backend when a radius or "Closest first" is chosen, because the backend then also filters by radius (10 km by default) and leaves out announcements without coordinates. "Closest first" without a radius asks for 100 km, the maximum. The distance on a card ("1.2 km away") is only known in those cases.
- **Backend:** `AnnouncementService.getAll(filters, location)` sends `type`, `species`, `status`, `color`, `breed`, `area`, `lat`, `lng`, `radiusKm` and `sort` (`newest`, `oldest` or `closest`) as query parameters. The backend sorts, the page shows the announcements in the order it returns them.

## Project structure

```
src/
├── app/
│   ├── pages/        pages (list, form, details, map)
│   ├── components/   reusable pieces (header, announcement card, map popup, filter bar, map)
│   ├── services/     calls to the backend, browser location, AnnouncementSearch
│   ├── models/       data types and labels (Announcement, Photo, filters, MapPin, Coordinates)
│   └── shared/       Leaflet icon helper, DATE_FORMAT
├── environments/     backend URL
└── styles.css        global styles and the CSS variables
```

## Conventions

- **Colors, shadows and corners:** only the CSS variables from `src/styles.css` (`var(--color-primary)`, `var(--color-text-muted)`, `var(--radius-control)`...), never hex codes. A new color is first added there as a variable, named by its role.
- **Dates:** the `date` pipe with `DATE_FORMAT` from `shared/date-format.ts` ("30 Oct 2026, 18:00").
- **Types and species:** `TYPE_LABELS` and `SPECIES_LABELS` ("Lost", "Cat"), never the raw `LOST` or `CAT`.
- **Wording:** "lost pet" for LOST, "found animal" for FOUND, "animal" when a text is about both, and a full stop at the end of every message.
- **Formatting:** Prettier (`.prettierrc`). Before a pull request: `npm run test:coverage`, `npm run build` (no warnings) and `npx prettier --check src`.
- **Phone screens:** below 600px every page has a one-column layout; check new pages in DevTools with a phone size.

## Map component

The map uses [Leaflet](https://leafletjs.com/) with [OpenStreetMap](https://www.openstreetmap.org/) tiles (free, no API key). Use it in a page like this:

```ts
imports: [MapView];
```

```html
<app-map [pins]="pins" [center]="center" [zoom]="15" (locationSelected)="onMapClick($event)" />
```

| Input / output       | What it does                                                                                          |
| -------------------- | ----------------------------------------------------------------------------------------------------- |
| `pins`               | List of `{ latitude, longitude, type: 'LOST' \| 'FOUND', label?, id? }`. LOST is red, FOUND is green. |
| `center`             | `{ latitude, longitude }`. Without it, the map shows all of Romania.                                  |
| `zoom`               | Zoom level used together with `center` (default 15).                                                  |
| `fitToPins`          | Without a `center`, zooms so that all the pins fit on the screen.                                     |
| `popup`              | A `<ng-template let-pin>` shown in a popup when a pin is clicked. `pin.id` finds what it stands for.  |
| `(locationSelected)` | Emits `{ latitude, longitude }` (6 decimals) where the user clicked.                                  |

The map does not add a pin by itself when it is clicked. The page decides: for the form, put the clicked location in `pins` and in the latitude and longitude fields. The pin colors are the `--color-lost` and `--color-found` variables in `src/styles.css`. The map is 450px high by default (380px and 320px on smaller screens), set another `height` on `app-map` to change it.