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

The app expects the backend to run on `http://localhost:8080`. The URL is set in one place, in `src/environments/environment.ts` (and `environment.development.ts` for `npm start`).

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
│   ├── components/   reusable pieces (header, ...)
│   ├── services/     calls to the backend
│   ├── models/       data types (Announcement, Photo)
│   └── shared/       things used everywhere
└── environments/     backend URL
```
