# Frontend

This folder contains the frontend of SafePaws, built with Angular.

It handles the user interface: posting announcements, viewing them in a list and on the map, and generating printable posters.

## How to run

### Requirements

- Node.js 22.22.3 or newer (24 LTS recommended) and npm

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

The list needs the backend running on `http://localhost:8080` (see `backend/README.md`: copy `.env.example` to `.env`, run `docker compose up -d`, then `./mvnw spring-boot:run` in `backend/`). It comes with test announcements, so the list is not empty. Without the backend, the list page shows "Could not load the announcements."

The backend URL is set in one place, in `src/environments/environment.ts` (and `environment.development.ts` for `npm start`).

### Pages

| Route                | Page                   |
| -------------------- | ---------------------- |
| `/announcements`     | List of announcements  |
| `/announcements/new` | Form for posting a pet |

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
│   ├── pages/        pages (list, form)
│   ├── components/   reusable pieces (header, announcement card, map)
│   ├── services/     calls to the backend
│   ├── models/       data types (Announcement, Photo, MapPin)
│   └── shared/       things used everywhere
└── environments/     backend URL
```

## Map component

The map uses [Leaflet](https://leafletjs.com/) with [OpenStreetMap](https://www.openstreetmap.org/) tiles (free, no API key). Use it in a page like this:

```ts
imports: [MapView];
```

```html
<app-map [pins]="pins" [center]="center" [zoom]="15" (locationSelected)="onMapClick($event)" />
```

| Input / output       | What it does                                                                                     |
| -------------------- | ------------------------------------------------------------------------------------------------ |
| `pins`               | List of `{ latitude, longitude, type: 'LOST' \| 'FOUND', label? }`. LOST is red, FOUND is green. |
| `center`             | `{ latitude, longitude }`. Without it, the map shows all of Romania.                             |
| `zoom`               | Zoom level used together with `center` (default 15).                                             |
| `(locationSelected)` | Emits `{ latitude, longitude }` (6 decimals) where the user clicked.                             |

The map does not add a pin by itself when it is clicked. The page decides: for the form, put the clicked location in `pins` and in the latitude and longitude fields. The pin colors are the `--color-lost` and `--color-found` variables in `src/styles.css`. The map is 400px high by default, set another `height` on `app-map` to change it.
