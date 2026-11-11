# Luca's decisions and messages

Decisions and team messages for the frontend cards "Announcements list", "Leaflet map component", "Filter bar" and "Map page".

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

- **Order on the page:** the backend sorts the list (newest event date first by default), and the page shows the announcements in the order it receives them. The sorting that the page did itself while the backend sorted by creation date was removed.
- **List type:** the list is typed from the code (`AnnouncementSummaryResponse`): `id`, `type`, `petName`, `species`, `area`, `latitude`, `longitude`, `distanceKm` (only when a location was sent), `eventDate`, `status` and `photos`. The full `Announcement` type is only for `POST /announcements` and `GET /announcements/{id}`.
- **No mock data:** the frontend always calls the backend. The test data comes from the migrations.

### Filter bar (`frontend/src/app/components/filter-bar`)

- **One reusable component:** `<app-filter-bar>` is used by the list page and the map page, so both show the same results for the same filters.
- **Same defaults as the backend:** with no filter the backend returns only active announcements, newest event first. So the status dropdown starts on "Active" and has no "Any" option, because the backend has no value for all statuses.
- **Filters in the URL:** only the values that are not the default, for example `/announcements?species=CAT&color=grey`. They survive a refresh and can be shared. The text fields wait 300 ms after the user stops typing before asking the backend.
- **Location is only sent when needed:** `lat` and `lng` make the backend filter by radius (10 km by default) and leave out announcements without coordinates. So the frontend only sends them when the user picks a radius or "Closest first". Because of that, "1.2 km away" is only shown on the cards in those cases.
- **"Closest first" without a radius:** the frontend sends `radiusKm=100`, the maximum, so the list is not cut at 10 km.
- **Location refused:** the radius and "Closest first" are disabled, with the message "Allow location access to see announcements near you". The filters in the URL that need the location are ignored.

### Map page (`/map`)

- **Same results as the list:** the map page and the list page share one class, `AnnouncementSearch`, which reads the filters from the URL, asks for the location and loads the announcements. They also use the same filter bar. So `/map?species=CAT` and `/announcements?species=CAT` show the same announcements.
- **Where the map starts:** centered on the user when the browser location is allowed. If it is refused (or still being asked), the map zooms so that all the pins fit on the screen. Changing the filters moves the map only in the second case, so it does not jump while the user explores around their own location.
- **Pins:** one for each announcement that has coordinates, red for LOST and green for FOUND. Announcements without coordinates are counted in a note above the map, "1 announcement has no location and is not on the map."
- **Popup:** main photo, pet name (or the species when there is no name), area, date and a "View details" button to `/announcements/:id`. That route is an assumption until the details page exists, and until then the link goes back to the list.
- **Loaded on demand:** the map page is a lazy route, so Leaflet is not part of the first download of the app (the initial bundle was over the 500 kB budget with it).

## Messages

### Antonia

> Thanks for the coordinates, the filters and the sorting, the filter bar uses them now. Two things I noticed:
>
> 1. There is no way to ask for all statuses at once: without `status` the list is only ACTIVE. That is fine for the list, so the status dropdown only has Active and Resolved. If we ever want "all", it needs a new value.
> 2. `lat` and `lng` also apply `radiusKm`, 10 km by default. With `sort=closest` and no radius, the list is cut at 10 km, so the frontend sends `radiusKm=100`. If you would rather have no radius limit for `sort=closest`, tell me and I'll stop sending it.
>
> `docs/api.md` doesn't describe the new parameters yet. Can you give Tudor a short summary?

### Victor and Vanessza

> The map component is on main. Use it with `imports: [MapView]` and `<app-map [pins]="pins" [center]="center" (locationSelected)="onMapClick($event)" />`. A pin is `{ latitude, longitude, type: 'LOST' | 'FOUND', label? }`. `(locationSelected)` gives you the clicked `{ latitude, longitude }`, and the map doesn't place a pin by itself, so put the location in `pins` and in the form fields yourself. Without `center` it shows all of Romania. Not every announcement has coordinates, so skip those when you build the pins.
>
> Run `npm install` after pulling, because Leaflet is a new dependency. Details are in `frontend/README.md`.
>
> The map has two more inputs now: `[fitToPins]="true"` zooms to fit all the pins when there is no `center`, and `[popup]` takes an `<ng-template let-pin>` that is shown when a pin is clicked (give the pins an `id` to find what they stand for). The map page uses both.

### Vanessza

> The map popups have a "View details" button that goes to `/announcements/:id`. Is that the route of your details page? If it is different, tell me and I'll change the link (it is one line in `announcement-popup.html`). Until the details page exists, the button goes back to the list.

### Jessica

> `testing.md` frontend line: with the filter bar and the map page there are 168 tests and about 86% statement coverage (it says 26 tests and 97.7%). The drop comes from the form, which has about 68% statement coverage. The total is still above the 70% minimum, but with a smaller margin.
>
> Scenario C1: the page shows the cards in the order the backend returns them, which is newest event date first, so the order matches the date on each card. It is not by `createdAt` anymore.
>
> New manual scenarios for the filters: with the list open, choose "Resolved" in the status dropdown, and only resolved announcements show and the URL has `?status=RESOLVED`. Type `grey` in Color, and after a short pause only matching cards show. Choose "Closest first" and allow the location, and the cards show "x.x km away". Refuse the location, and the radius and "Closest first" are disabled with a message. Filter until nothing matches, and the page says "No announcements match your filters", with a "Clear filters" button. Reload a filtered URL, and the same filters are applied.
>
> New manual scenarios for the map page (`/map`): the "Map" link in the header is highlighted, and the tab title is "Map | SafePaws". Allow the location, and the map starts around you. Refuse it, and the map shows all the pins, with the message about the location in the filter bar. Click a pin, and a popup shows the photo, the name (or species), the area, the date and "View details". Choose "Found" in the filter bar, and only green pins stay. Create an announcement through the API without coordinates, and the note says that one announcement is not on the map.

> I ran the UI scenarios of the list, the filter bar and the map page in a browser (Brave, driven by a script) against the real backend on 11 Nov, with the 23 announcements of a database that has all 5 migrations (19 active, 18 of them with coordinates, one without). Results: C1, C7, E4, E7 to E9, F12, G12 to G14, H1 to H9 all pass. C8 passes too: a new announcement posted through the form shows up in the list without a reload, at the right place for its event date.
>
> Not run: C2 (deletes all announcements), C3 and H10 (stop the backend). H11 stays blocked, because `View details` goes to `/announcements/:id`, which has no page yet, so it goes back to the list. The script created one announcement for C8 and I deleted it afterwards, so the database is as I found it.

### Tudor

> For `architecture.md`: the map uses Leaflet with OpenStreetMap tiles (free, no API key). It is a reusable component, `<app-map>`, in `frontend/src/app/components/map`. It shows Romania by default or centers on `[center]`, shows pins from `[pins]` (red for LOST, green for FOUND), and emits `(locationSelected)` with the coordinates the user clicked. Usage is in `frontend/README.md`.
>
> The frontend tree in `architecture.md` should also list `shared/` (Leaflet icon helper), `components/filter-bar` and the `MapPin` and `Coordinates` types in `models/`.
>
> For `decisions.md`: Leaflet with OpenStreetMap was chosen over Google Maps because it is free and needs no API key. The other decisions are above.
>
> For `usage-guide.md`: on the announcements page, use the filter bar above the cards to narrow the list by type, species, status, color, breed and area, or by distance from you (the browser asks for your location). The filters are kept in the address, so you can share a filtered list or reload the page. "Clear filters" appears when a filter is active and removes them.
>
> For `usage-guide.md`: the "Map" link in the header opens a map with a pin for each announcement, red for lost and green for found. If you allow the location, the map starts around you, otherwise it shows all the pins. Click a pin to see the photo, name, area and date, and "View details" to open the announcement. The filter bar above the map works like the one on the list, and the same filters give the same announcements. Screenshots are in the `screenshots` folder of my project (`map-popup.png`, `map-fit-pins.png`, `map-near-me.png`).
>
> Numbers for `testing.md` are in the message for Jessica.

### Matei

> Thanks for the coordinates on all 22 announcements. The map shows all of Romania by default and only draws the pins it is given, so a missing coordinate is skipped by the page that builds the pins, not by the map. I'll test that with an announcement created through the API without coordinates when the map page is built.
