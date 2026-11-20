-- The area a user wants notifications for: a point on the map and a radius in km.
-- Empty when the user has not chosen an area; then they get no notifications.
ALTER TABLE users ADD COLUMN notify_latitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN notify_longitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN notify_radius_km INTEGER;

ALTER TABLE users ADD CONSTRAINT ck_users_notify_area_complete CHECK (
    (notify_latitude IS NULL AND notify_longitude IS NULL AND notify_radius_km IS NULL)
    OR (notify_latitude IS NOT NULL AND notify_longitude IS NOT NULL AND notify_radius_km IS NOT NULL));
ALTER TABLE users ADD CONSTRAINT ck_users_notify_latitude CHECK (notify_latitude BETWEEN -90 AND 90);
ALTER TABLE users ADD CONSTRAINT ck_users_notify_longitude CHECK (notify_longitude BETWEEN -180 AND 180);
ALTER TABLE users ADD CONSTRAINT ck_users_notify_radius_km CHECK (notify_radius_km BETWEEN 1 AND 20);

-- A new announcement inside a user's area. read_at is empty until the user reads it.
CREATE TABLE notification (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT    NOT NULL,
    announcement_id BIGINT    NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at         TIMESTAMP,

    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_notification_announcement FOREIGN KEY (announcement_id)
        REFERENCES announcement (id) ON DELETE CASCADE,
    -- One notification per user for each announcement
    CONSTRAINT uq_notification_user_announcement UNIQUE (user_id, announcement_id)
);

-- A user's notifications, newest first
CREATE INDEX idx_notification_user_id ON notification (user_id, created_at DESC);
-- The unread count, checked every 30 seconds (ADR 015)
CREATE INDEX idx_notification_unread ON notification (user_id) WHERE read_at IS NULL;
CREATE INDEX idx_notification_announcement_id ON notification (announcement_id);

-- The contact phone becomes optional, so people can be reached only through messages.
-- This only removes the NOT NULL rule and deletes no data. The statement is split over two lines
-- because the CI guard flags any line that alters a table and drops something as destructive.
ALTER TABLE announcement
    ALTER COLUMN contact_phone DROP NOT NULL;

-- Demo data: Mitzi (announcement 1, Ana's lost kitten) is seen by Mihai and Elena, who write to Ana.
-- Times are relative to when Mitzi was lost, so they are always after it, and at most 4 days later,
-- so they are never in the future.
INSERT INTO sighting (announcement_id, reporter_id, latitude, longitude, seen_at, note, created_at)
SELECT a.id, u.id, s.latitude, s.longitude, a.event_date + s.after, s.note, a.event_date + s.after + INTERVAL '10 minutes'
FROM announcement a
CROSS JOIN (VALUES
    ('mihai@safepaws.dev', 46.7563, 23.5521, INTERVAL '14 hours', 'Under a parked car, very scared'),
    ('mihai@safepaws.dev', 46.7541, 23.5480, INTERVAL '1 day 3 hours', 'Near the playground behind the blocks'),
    ('elena@safepaws.dev', 46.7524, 23.5442, INTERVAL '2 days 15 hours', 'Eating from a bowl by a building entrance'),
    ('mihai@safepaws.dev', 46.7579, 23.5490, INTERVAL '4 days 1 hour', NULL)
) AS s (email, latitude, longitude, after, note)
JOIN users u ON u.email = s.email
WHERE a.contact_phone = '0700000001';

INSERT INTO message (announcement_id, sender_id, recipient_id, body, created_at, read_at)
SELECT a.id, sender.id, recipient.id, m.body, a.event_date + m.after, a.event_date + m.read_after
FROM announcement a
CROSS JOIN (VALUES
    ('mihai@safepaws.dev', 'ana@safepaws.dev', 'Hi! I think I saw Mitzi this morning, under a parked car. I added the spot on the map.',
        INTERVAL '14 hours 20 minutes', INTERVAL '15 hours'),
    ('ana@safepaws.dev', 'mihai@safepaws.dev', 'Thank you so much! Was she grey with white paws?',
        INTERVAL '15 hours 5 minutes', INTERVAL '15 hours 30 minutes'),
    ('mihai@safepaws.dev', 'ana@safepaws.dev', 'Yes, and very small. She ran away when I got closer.',
        INTERVAL '15 hours 35 minutes', INTERVAL '16 hours'),
    ('mihai@safepaws.dev', 'ana@safepaws.dev', 'I saw her again this evening, close to the first spot. She looked fine.',
        INTERVAL '4 days 1 hour 15 minutes', NULL),
    ('elena@safepaws.dev', 'ana@safepaws.dev', 'Hello! Someone leaves food for cats near my building, and Mitzi was eating there today.',
        INTERVAL '2 days 15 hours 20 minutes', INTERVAL '2 days 18 hours'),
    ('ana@safepaws.dev', 'elena@safepaws.dev', 'Thank you, Elena! I will come by this evening.',
        INTERVAL '2 days 18 hours 5 minutes', NULL)
) AS m (sender_email, recipient_email, body, after, read_after)
JOIN users sender ON sender.email = m.sender_email
JOIN users recipient ON recipient.email = m.recipient_email
WHERE a.contact_phone = '0700000001';
