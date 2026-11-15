-- Messages about an announcement between two users, so they can talk without a public phone number.
-- A conversation is every message about one announcement between the same two users.
CREATE TABLE message (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    announcement_id BIGINT        NOT NULL,
    sender_id       BIGINT        NOT NULL,
    recipient_id    BIGINT        NOT NULL,
    body            VARCHAR(1000) NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Empty until the recipient opens the message, for the unread count
    read_at         TIMESTAMP,

    CONSTRAINT fk_message_announcement FOREIGN KEY (announcement_id)
        REFERENCES announcement (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT fk_message_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT ck_message_not_to_self CHECK (sender_id <> recipient_id),
    CONSTRAINT ck_message_body_not_blank CHECK (btrim(body) <> '')
);

CREATE INDEX idx_message_announcement_id ON message (announcement_id, created_at);
CREATE INDEX idx_message_sender_id ON message (sender_id);
-- The inbox: the newest messages received by a user first
CREATE INDEX idx_message_recipient_id ON message (recipient_id, created_at DESC);

-- "I saw it here": where and when someone saw the pet, shown on the map as a trail
CREATE TABLE sighting (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    announcement_id BIGINT           NOT NULL,
    reporter_id     BIGINT           NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    seen_at         TIMESTAMP        NOT NULL,
    note            VARCHAR(500),
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sighting_announcement FOREIGN KEY (announcement_id)
        REFERENCES announcement (id) ON DELETE CASCADE,
    CONSTRAINT fk_sighting_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT ck_sighting_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_sighting_longitude CHECK (longitude BETWEEN -180 AND 180)
);

-- The trail of one announcement, in the order the pet was seen
CREATE INDEX idx_sighting_announcement_id ON sighting (announcement_id, seen_at);
CREATE INDEX idx_sighting_reporter_id ON sighting (reporter_id);
