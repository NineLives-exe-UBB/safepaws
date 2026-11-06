# Luca's decisions and messages

Decisions and team messages for the frontend cards "Announcements list" and "Leaflet map component".

## Decisions

### Map component (`frontend/src/app/components/map`)

- **Leaflet and OpenStreetMap:** the map uses Leaflet 1.9.4 with OpenStreetMap tiles. It is free and needs no API key, so there is no key to hide or to pay for.
- **One reusable component:** `<app-map>` (class `MapView`) is used by every page that needs a map. It shows all of Romania by default, or centers on `[center]`. Pins come from `[pins]`, and `(locationSelected)` reports where the user clicked.
- **A click does not place a pin:** the map only reports the click. The page decides what to do with it. The form puts the clicked location in `pins` and in the latitude and longitude fields.
- **Pin colors:** red for LOST and green for FOUND, from the CSS variables `--color-lost` and `--color-found` in `styles.css`. The announcement card labels use the same variables.
- **Clicked coordinates:** rounded to 6 decimals, and the longitude is brought back to -180..180 when the user clicks on a repeated copy of the world, so the backend does not reject it.
- **Marker images:** Leaflet's default marker images are copied to `leaflet/` by `angular.json` and the icon paths are set explicitly. Without this the default pin does not show in the Angular build.
- **Only announcements with coordinates get a pin:** coordinates are optional in the API, so a map page has to skip the announcements without them. The map itself only draws the pins it is given.

### Announcements list

- **Order on the page:** the list is sorted by event date, newest first, in the browser. The API still returns the announcements by creation date. The cards only show the event date, so a new announcement about an older event appeared first and the list looked unsorted.
- **Revisit when the backend sorts:** if the backend sorts by event date, the sorting in `announcement-list.ts` should be removed.
- **List type:** the list is typed from the code (`AnnouncementSummaryResponse`): `id`, `type`, `petName`, `species`, `area`, `eventDate`, `status` and `photos`. The full `Announcement` type is only for `POST /announcements` and `GET /announcements/{id}`.

## Messages

### Antonia

> The map page needs coordinates for every pin, but `GET /announcements` doesn't return `latitude` and `longitude` (they are not in `AnnouncementSummaryResponse`). Can you add them? They can be `null`. Without them the map page would need one `GET /announcements/{id}` per announcement. `createdAt` would also help: the page shows the event date only and can't show when something was posted.
>
> The frontend now sorts the list by event date itself, while the API returns it by `createdAt`. If you prefer to sort in the backend, tell me and I'll remove the sorting from the frontend.

### Victor and Vanessza

> The map component is on main. Use it with `imports: [MapView]` and `<app-map [pins]="pins" [center]="center" (locationSelected)="onMapClick($event)" />`. A pin is `{ latitude, longitude, type: 'LOST' | 'FOUND', label? }`. `(locationSelected)` gives you the clicked `{ latitude, longitude }`, and the map doesn't place a pin by itself, so put the location in `pins` and in the form fields yourself. Without `center` it shows all of Romania. Not every announcement has coordinates, so skip those when you build the pins.
>
> Run `npm install` after pulling, because Leaflet is a new dependency. Details are in `frontend/README.md`.

### Jessica

> `testing.md` frontend line: on main there are now 62 tests with 77.4% statements (it says 26 tests and 97.7%). The drop comes from the new form, which has about 68% statement coverage. The total is still above the 70% minimum, but with a smaller margin.
>
> Scenario C1 says the cards are "newest first (by `createdAt`)". The page now shows them newest first by event date, so the order of the cards follows the date on each card, and the API order is different. The map component has no page yet, so there is no manual scenario for it until the map page exists.

### Tudor

> For `architecture.md`: the map uses Leaflet with OpenStreetMap tiles (free, no API key). It is a reusable component, `<app-map>`, in `frontend/src/app/components/map`. It shows Romania by default or centers on `[center]`, shows pins from `[pins]` (red for LOST, green for FOUND), and emits `(locationSelected)` with the coordinates the user clicked. Usage is in `frontend/README.md`.
>
> The frontend tree in `architecture.md` should also list `shared/` (Leaflet icon helper) and the `MapPin` and `Coordinates` types in `models/`.
>
> For `decisions.md`: Leaflet with OpenStreetMap was chosen over Google Maps because it is free and needs no API key. The other decisions are above.
>
> Numbers for `testing.md` are in the message for Jessica.

### Matei

> Thanks for the coordinates on all 22 announcements. The map shows all of Romania by default and only draws the pins it is given, so a missing coordinate is skipped by the page that builds the pins, not by the map. I'll test that with an announcement created through the API without coordinates when the map page is built.
