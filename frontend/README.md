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

The list needs the backend running on `http://localhost:8080`, with the seeded test data. See `backend/README.md` (copy `.env.example` to `.env`, run `docker compose up -d`, then `./mvnw spring-boot:run` in `backend/`). Without it, the list page shows "Could not load the announcements."

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
