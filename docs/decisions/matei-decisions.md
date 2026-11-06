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

### Demo data (planned)

- **Migrations:** `V4__Seed_demo_photos.java` loads the photos, and `V5` completes the original 14 announcements.
- **Locations:** the Java migration goes in `backend/src/main/java/db/migration/`, and the images go in `backend/src/main/resources/db/demo-photos/`.
- **Photos:** free Wikimedia Commons photos (CC0 or public domain), with sources listed for the documentation.

## Messages

### Antonia

> V3 is on main, so you can use `unaccent` in search. It's a database function that's STABLE, not IMMUTABLE, so it can't go directly in an index, which is fine at this size. `grey` won't find `Gray` because they're different words, not an accent difference.

### Jessica

> Test data now has 22 announcements, with Brașov, Timișoara, Iași, Sibiu and Oradea added. `Iosefin, Timisoara` and `Tatarasi, Iasi` have no diacritics on purpose. `grey` not finding `Gray` is expected. The new rows' dates depend on when the migration ran, so don't use exact dates in scenarios. No row is missing coordinates now, so create one through the API to test that case.

### Luca

> All 22 test announcements now have coordinates, across 8 cities/towns, so the map shouldn't open zoomed in on Cluj only. Coordinates are still optional in the API, so the map must handle a missing pin. To test that, create an announcement through the API without coordinates.

### Tudor

> For `database.md`: V3 enables `unaccent`, adds indexes on type, species and status, fills in the missing coordinates and adds 8 announcements (22 total). Area format is "Neighborhood, City".
>
> `testing.md:30` needs updating: now 22 announcements (12 LOST, 10 FOUND, 4 RESOLVED, none without coordinates, 10 without a pet name).
>
> `architecture.md:131` should list V3 too.

### Crișan

> I'm adding demo photos with a Java Flyway migration, `V4__Seed_demo_photos.java`, in `backend/src/main/java/db/migration/`. The images go in `backend/src/main/resources/db/demo-photos/`. OK with you? The CI migration guard doesn't check Java migrations, so we need to watch version numbers ourselves.
