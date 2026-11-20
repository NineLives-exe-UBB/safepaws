# Matei's decisions and messages

Decisions and team messages for the database cards "Search support & demo data", "Complete & presentable demo data" and, in the DEMO 2 section at the end, "Users table & announcement owners".

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

---

## DEMO 2: users and announcement owners

Migrations `V6__users_and_owners.sql`, `V7__messages_and_sightings.sql` and `V8__require_announcement_owner.sql`. The tables, columns and demo accounts are in `database.md`.

### Decisions

- **The table is `users`, not `user`:** `user` is a reserved word in PostgreSQL. The other tables stay singular (`announcement`, `photo`, `message`, `sighting`).
- **The password column is `password_hash`** (`VARCHAR(60)`), because it only ever holds a BCrypt hash.
- **Emails are checked in the database too:** `uq_users_email` makes them unique, and `ck_users_email_normalized` only accepts an email that is trimmed and lowercase. The backend still has to trim and lowercase it before saving, otherwise the insert fails.
- **`owner_id` is required since `V8`:** the backend sets it from the logged-in user, so the database rejects an announcement without an owner. A test that saves an announcement directly through the repository has to set an owner, for example `userRepository.findByEmail("ana@safepaws.dev")`.
- **Old announcements without an owner go to Ana:** on a local database, announcements posted before the backend set the owner had none. `V8` gives them to `ana@safepaws.dev` instead of deleting them, so the migration doesn't fail and they can still be edited. A fresh database has none.
- **Demo accounts:** the 3 accounts from the contract, all with `SafePaws1!`, saved as BCrypt hashes (cost 10, a different hash per account). Each one owns LOST and FOUND, active and resolved announcements and one with 2 photos. The table is in `database.md`.
- **The contact stays on the announcement:** the owner's display name and the announcement's `contact_name` can be different people, as in ADR 014.
- **`V7` is a first design:** the contract does not describe messages and sightings yet. A conversation is all the messages about one announcement between the same two users, so there is no conversation table. `read_at` is empty until the message is read. A sighting needs coordinates and the time the pet was seen; the note is optional. Messages and sightings need an account, and they are deleted with their announcement. Changes are still easy while this PR is open; after it is merged they need a new migration.
- **Only I write migrations for now.** The next version is in the DEMO 3 section.

### Messages

#### Antonia

> V6 and V7 are on main, so you can write the `User` entity: table `users`, columns `id`, `email`, `password_hash`, `display_name`, `created_at`. The backend runs with `ddl-auto=validate`, so the names have to match exactly. Save the email trimmed and in lowercase: the database rejects anything else, and `uq_users_email` is a second safety net behind your 409 check. `announcement.owner_id` already exists, but you don't need to map it in your first PR; Patrick does.

#### Crișan

> The demo hashes are `$2a$10$...`, which the default `BCryptPasswordEncoder` (strength 10) accepts. With your security config on main, a test can check that `SafePaws1!` matches the hashes of the 3 demo accounts. Look the user up by the lowercase email, because that's how it's saved.

#### Patrick

> `announcement.owner_id` is a `BIGINT` foreign key to `users(id)`, with an index. It's empty only for announcements posted before your card, because the 22 demo ones already have owners. Please write in the chat when your PR is merged, so I can add `V8` (owner required). For tests: Ana owns announcement 1, Mihai owns 2, Elena owns 3.
>
> `V8` makes `owner_id` required, so `announcement.getOwner()` is never null for a saved announcement. The 403 message in `AnnouncementOwnershipService` is `Only the owner can modify this announcement`, but the contract (`vanessza-decisions.md`, `api.md`) says `You can only change your own announcements`. Please use the contract's text, because the frontend may show it. (Done: the code uses the contract's text since `fix/auth-route-guards`.)

#### Jessica

> The 3 demo accounts and the announcements each one owns are in `database.md`. For "another user" tests: log in as Mihai and try to resolve announcement 1, which is Ana's. Each account owns at least one resolved announcement and one with 2 photos. Reset the database (`docker compose down -v`) before testing so the owners match the table.

#### Luca and Victor

> To try the owner buttons with the real data: log in as `ana@safepaws.dev` / `SafePaws1!`, and announcements 1, 4, 7, 10, 13, 16, 19 and 22 are hers.

#### Tudor

> For the root `README.md`: the demo accounts table is in `database.md` ("Demo accounts (V6)"). `architecture.md` should list `V6` and `V7`. The ER diagram in `database.md` will get the 3 new tables from me.
>
> In `architecture.md`, line 198 lists only V1, V2, V3 and V5; it needs V6, V7 and V8. Line 263 says new announcements are saved without an owner, which is no longer true: `POST /announcements` saves the logged-in user as the owner and `V8` makes `owner_id` required. The owner check is on `main` too, so the "planned, not on main yet" notes for `403` in `api.md` and `architecture.md:262` are out of date. Every announcement in the API now has an `ownerId`.
>
> Anyone whose `.env` was copied before `JWT_SECRET` was added to `.env.example` gets `Could not resolve placeholder 'JWT_SECRET'` and the backend doesn't start. A line in the README setup ("if you already have a `.env`, add `JWT_SECRET` from `.env.example`") would save people time.

#### Vanessza

> `V7` adds the `message` and `sighting` tables, but the contract doesn't describe them yet. Please look at them in the PR before the messages and sightings cards are written. Two small things in the contract: the login example has a double comma after `"token": "<JWT>"`, and cards #46 and #47 write "Email is already registered." and "Invalid email or password." with a full stop, while the contract says no full stop.

---

## DEMO 3: notifications, optional phone, demo messages and sightings

Migration `V9__notifications_and_optional_phone.sql`. The columns, indexes and demo data are in `database.md`.

### Decisions

- **The table is `notification`, not `notifications`:** like the other tables (`announcement`, `message`, `sighting`). Only `users` is plural, because `user` is reserved. The API stays as in the contract.
- **The notification area is 3 columns on `users`:** `notify_latitude`, `notify_longitude` and `notify_radius_km`. A user has at most one area, so there is no separate table. The three are either all set or all empty (`ck_users_notify_area_complete`); empty means no area. The radius is a whole number of km from 1 to 20, as in the contract.
- **Read state is a time:** `notification.read_at` is empty until the notification is read, like `message.read_at`. The API's `"read"` is `read_at != null`.
- **One notification per user per announcement:** `uq_notification_user_announcement`. Notifications are deleted with their announcement.
- **`contact_phone` is optional since `V9`.** The demo announcements keep their phones, because migrations find them by phone. To test an announcement without a phone, create one through the API.
- **The CI guard and `DROP NOT NULL`:** the guard flags any line that alters a table and drops something, even `DROP NOT NULL`, which deletes no data. In `V9` the statement is split over two lines.
- **Demo messages and sightings are on Mitzi** (announcement 1, Ana's lost kitten): 4 sightings (3 from Mihai, 1 from Elena) and 2 conversations (Ana with Mihai, Ana with Elena). Their times are relative to when Mitzi was lost, so they are never in the future. No account has a notification area and there are no notifications.
- **Next migration is `V10`.**

### Messages

#### Crișan

> `V9` is ready for your card. The area is on `users`: `notify_latitude`, `notify_longitude` (`DOUBLE PRECISION`) and `notify_radius_km` (`INTEGER`, 1 to 20). Set all three or none, otherwise the database rejects the update; `DELETE /notifications/area` sets all three to null. Notifications are in the `notification` table: `id`, `user_id`, `announcement_id`, `created_at`, `read_at` (null = unread, so `"read"` is `read_at != null`). `uq_notification_user_announcement` allows one notification per user per announcement. For the unread count, `WHERE user_id = ? AND read_at IS NULL` uses the index `idx_notification_unread`. The distance check can reuse the Haversine formula in `AnnouncementService`. Remember: no notification for the author, and none for an announcement without coordinates.
>
> About the CI migration guard: it flags `ALTER TABLE ... DROP NOT NULL` on one line as destructive, even though no data is deleted. `V9` splits it over two lines. If you want, the check could ignore `DROP NOT NULL` and `DROP DEFAULT`.

#### Antonia

> `contact_phone` is optional in the database since `V9`. In the `Announcement` entity, `@Column(name = "contact_phone", nullable = false, ...)` should become `nullable = true`. In `AnnouncementRequest`, removing `@NotBlank` from `contactPhone` is enough, because the `@Pattern` already accepts an empty value. An empty string should probably be saved as `null`, so the API answers `"contactPhone": null` as the contract says. For messages, the tables are from `V7`: `message` has `announcement_id`, `sender_id`, `recipient_id`, `body`, `created_at` and `read_at`. The database already rejects a blank body and a message to yourself (`ck_message_body_not_blank`, `ck_message_not_to_self`), but give your own 400 first. On a fresh database, Ana has 2 conversations about Mitzi (with Mihai and with Elena), so you can test the list right away.

#### Patrick

> Mitzi (announcement 1) has a trail of 4 sightings on a fresh database: 3 from Mihai and 1 from Elena, in the days after she was lost. `sighting` has `latitude`, `longitude`, `seen_at` (required) and `note` (optional). The database checks the coordinate ranges, but not that `seen_at` is in the past or after the date the pet was lost; your service has to.

#### Luca

> To see the trail on the map: open announcement 1 (Mitzi). It has 4 sightings around Mănăștur, so the map should zoom to show them all with the point where she was lost. Announcement 2 (Rex) is also lost and active, with no sightings, for the empty case.

#### Victor

> To try the messages: log in as `ana@safepaws.dev` (2 conversations about Mitzi, 1 unread from Mihai) or `elena@safepaws.dev` (1 unread from Ana). Mihai has no unread messages. Every demo announcement still has a phone, so create one without a phone through the form to test "only Send a message".

#### Jessica

> After `docker compose down -v`: Mitzi (1) has 4 sightings and 6 messages. Ana has 2 conversations and 1 unread message, Elena has 1 unread, Mihai has none. No account has a notification area, so set one first to test notifications. The dates of the sightings and messages depend on when the database was created, so don't use exact dates in scenarios.

#### Tudor

> For `architecture.md`: `V9` adds the `notification` table, the notification area on `users` and the optional `contact_phone`, plus demo messages and sightings. The details are in `database.md`, and the ER diagram has the new table and columns.
