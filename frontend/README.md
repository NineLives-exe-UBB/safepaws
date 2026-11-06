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

`npm start` shows **mock data** (`useMockData: true` in `src/environments/environment.development.ts`), with the filters and sorting done in the app, so no backend is needed. To use the real backend, set it to `false`. The backend must then run on `http://localhost:8080` with the seeded test data (see `backend/README.md`: copy `.env.example` to `.env`, run `docker compose up -d`, then `./mvnw spring-boot:run` in `backend/`). Without it, the list page shows "Could not load the announcements."

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
- **In the URL:** only the values that are not the default, for example `/announcements?species=CAT&color=grey`, so they survive a refresh and can be shared. `fromQueryParams` and `toQueryParams` in `models/announcement-filters.model.ts` read and write them.
- **Location:** the page asks the browser for the user's location (`LocationService`). If it is refused, the radius and the "Nearest first" sorting are disabled, with a short message, and no distance is shown on the cards.
- **Backend:** `AnnouncementService.getAll(filters, location)` sends the filters as query parameters (`type`, `species`, `status`, `color`, `breed`, `area`, `radiusKm`, `sort`, `userLatitude`, `userLongitude`) and expects an optional `distanceKm` in each announcement. These names are the ones assumed from the API contract, so if the backend uses different ones, change them in one place: `getAll` and `toQueryParams`.

## Project structure

```
src/
├── app/
│   ├── pages/        pages (list, form)
│   ├── components/   reusable pieces (header, ...)
│   ├── services/     calls to the backend
│   ├── models/       data types (Announcement, Photo)
│   └── shared/       things used everywhere
└── environments/     backend URL
```
