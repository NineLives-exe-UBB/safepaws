# Matei's decisions and messages

Decisions and team messages for the database cards "Search support & demo data" and "Complete & presentable demo data".

## Decisions

### Search support (migration `V3__search_support.sql`)

- **Relative dates for new test data:** the 8 new announcements use dates relative to when the migration runs, so they stay recent and are never in the future. The dates differ between machines.
- **Placeholder phone numbers:** test data keeps the fictional `0700000xxx` series, so the numbers should not belong to real people.
- **Area format:** every area follows "Neighborhood, City". `Florești, Cluj` and `Baciu, Cluj` became `Centru, Florești` and `Centru, Baciu`, with their pins moved to those centers.
- **Missing diacritics on purpose:** `Iosefin, Timisoara` and `Tatarasi, Iasi` are written without diacritics, for search testing.
- **Colors stay as written:** `grey`, `Gray` and `dark grey` are different spellings. Search ignores diacritics but does not treat `grey` and `gray` as the same word.
- **Matching old rows:** `V3` finds announcements from `V2` by placeholder phone number, not by ID.

### Demo data (migrations `V4__Seed_demo_photos.java` and `V5__complete_demo_data.sql`)

- **Locations:** the Java migration goes in `backend/src/main/java/db/migration/`, and the images go in `backend/src/main/resources/db/demo-photos/`.
- **Photos:** free Wikimedia Commons photos under CC0. Sources are in `SOURCES.md` next to the images.
- **Descriptions follow the photos:** where a photo didn't match the text, `V5` changes the text. For example, the found kitten in Andrei Mureșanu is now grey tabby and white, not black, and Pufi is European, not Persian.
- **Reset before the demo:** all demo dates are set when the migrations run and don't move afterwards. Run `docker compose down -v` on the demo machine shortly before the demo so they are recent. This also deletes anything added by hand.
- **Next migration is `V6`:** SQL and Java migrations share version numbers, and the CI migration guard only checks SQL files. A duplicate with a Java migration stops Flyway from starting. Merged Java migrations also have no checksum, so editing one raises no error and doesn't re-run. Fixes go in a new migration.
- **New test data must be complete:** `DemoDataMigrationTest` and `DemoPhotosMigrationTest` fail if a migration adds an announcement with missing details or without exactly one main photo.

## Messages

### Antonia

> V3 is on main, so you can use `unaccent` in search. It's a database function that's STABLE, not IMMUTABLE, so it can't go directly in an index, which is fine at this size. `grey` won't find `Gray` because they're different words, not an accent difference.

### Jessica

> Test data now has 22 announcements, with Brașov, Timișoara, Iași, Sibiu and Oradea added. `Iosefin, Timisoara` and `Tatarasi, Iasi` have no diacritics on purpose. `grey` not finding `Gray` is expected. The new rows' dates depend on when the migration ran, so don't use exact dates in scenarios. No row is missing coordinates now, so create one through the API to test that case.
>
> With the demo data, every test announcement has a photo, which affects `testing.md`. B1, B9 and D4 use announcement 3 as the one without photos, so create a new announcement through the API for those instead. C1 now has 22 cards, and none shows `No photo` unless you create one without photos. On a fresh database, announcements 9, 13 and 21 have 2 photos each for testing the gallery.

### Luca

> All 22 test announcements now have coordinates, across 8 cities/towns, so the map shouldn't open zoomed in on Cluj only. Coordinates are still optional in the API, so the map must handle a missing pin. To test that, create an announcement through the API without coordinates.

### Tudor

> For `database.md`: V3 enables `unaccent`, adds indexes on type, species and status, fills in the missing coordinates and adds 8 announcements (22 total). Area format is "Neighborhood, City".
>
> `testing.md:30` needs updating: now 22 announcements (12 LOST, 10 FOUND, 4 RESOLVED, none without coordinates, 10 without a pet name).
>
> `architecture.md:131` should list V3 too.
>
> With the demo data, for `database.md`: V4 is a Java migration that loads the photos from files, and V5 completes the data. Their locations and rules are in the decisions above, and the photo sources are in `backend/src/main/resources/db/demo-photos/SOURCES.md`.
>
> `testing.md:30` should also say 25 photos, with every announcement having a main photo. Mitzi and Rex keep photo ids 1 and 2.
>
> `architecture.md` should list V4, V5 and the Java migration folder, which the project structure in the rules doesn't include either.

### Vanessza

> For the details page: on a fresh database, announcements 9, 13 and 21 have 2 photos each for the gallery. The demo photos mix portrait and landscape, and are 500 or 960 px wide, so they shouldn't be stretched to a fixed shape.

### Crișan

> I'm adding demo photos with a Java Flyway migration, `V4__Seed_demo_photos.java`, in `backend/src/main/java/db/migration/`. The images go in `backend/src/main/resources/db/demo-photos/`. OK with you? The CI migration guard doesn't check Java migrations, so we need to watch version numbers ourselves.
>
> For the poster: every test announcement now has a main photo, so test the no-photo case with a new announcement created through the API. The photos mix portrait and landscape, and are 500 or 960 px wide, so check that both fit the page without being stretched and still print clearly.
