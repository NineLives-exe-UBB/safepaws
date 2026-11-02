CREATE TABLE announcement (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type              VARCHAR(10)  NOT NULL,
    species           VARCHAR(20)  NOT NULL,
    pet_name          VARCHAR(50),
    breed             VARCHAR(100),
    color             VARCHAR(100),
    distinctive_marks VARCHAR(500),
    event_date        TIMESTAMP    NOT NULL,
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    area              VARCHAR(150) NOT NULL,
    contact_name      VARCHAR(100) NOT NULL,
    contact_phone     VARCHAR(20)  NOT NULL,
    status            VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_announcement_type CHECK (type IN ('LOST', 'FOUND')),
    CONSTRAINT ck_announcement_species CHECK (species IN ('CAT', 'DOG', 'OTHER')),
    CONSTRAINT ck_announcement_status CHECK (status IN ('ACTIVE', 'RESOLVED')),
    CONSTRAINT ck_announcement_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_announcement_longitude CHECK (longitude BETWEEN -180 AND 180),
    -- A map pin needs both coordinates, so they are set together or not at all
    CONSTRAINT ck_announcement_coordinates CHECK ((latitude IS NULL) = (longitude IS NULL))
);

CREATE INDEX idx_announcement_created_at ON announcement (created_at DESC);

CREATE TABLE photo (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    announcement_id    BIGINT       NOT NULL,
    data               BYTEA        NOT NULL,
    content_type       VARCHAR(50)  NOT NULL,
    file_size          BIGINT       NOT NULL,
    original_file_name VARCHAR(255),
    is_main            BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_photo_announcement FOREIGN KEY (announcement_id)
        REFERENCES announcement (id) ON DELETE CASCADE,
    CONSTRAINT ck_photo_content_type CHECK (content_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT ck_photo_file_size CHECK (file_size > 0)
);

CREATE INDEX idx_photo_announcement_id ON photo (announcement_id);

-- At most one main photo per announcement
CREATE UNIQUE INDEX uq_photo_main_per_announcement ON photo (announcement_id) WHERE is_main;
