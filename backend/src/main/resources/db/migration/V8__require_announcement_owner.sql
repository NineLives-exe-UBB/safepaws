-- Every announcement must have an owner, now that the backend sets it from the logged-in user.

-- A fresh database has no announcement without an owner. A local one can have some, posted before
-- the backend set the owner. They go to Ana, so this migration doesn't fail and they can still be edited.
UPDATE announcement
SET owner_id = (SELECT id FROM users WHERE email = 'ana@safepaws.dev')
WHERE owner_id IS NULL;

ALTER TABLE announcement ALTER COLUMN owner_id SET NOT NULL;
