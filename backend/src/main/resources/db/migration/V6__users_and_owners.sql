-- User accounts (ADR 014). The table is plural because "user" is a reserved word in PostgreSQL.
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    -- BCrypt hash ($2a$10$...), always 60 characters; the password itself is never stored
    password_hash VARCHAR(60)  NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    -- The backend trims the email and saves it in lowercase, so the unique check cannot be bypassed
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT ck_users_display_name_not_blank CHECK (btrim(display_name) <> '')
);

-- Every announcement belongs to the user who posted it. The column stays nullable until V8,
-- so posting keeps working until the backend sets the owner.
ALTER TABLE announcement ADD COLUMN owner_id BIGINT;

ALTER TABLE announcement ADD CONSTRAINT fk_announcement_owner
    FOREIGN KEY (owner_id) REFERENCES users (id);

CREATE INDEX idx_announcement_owner_id ON announcement (owner_id);

-- Demo accounts, all with the password "SafePaws1!" (listed in the README), saved as BCrypt hashes
INSERT INTO users (email, password_hash, display_name)
VALUES ('ana@safepaws.dev', '$2a$10$G1ImbrhJGVGqqR1klzQNFeytHmxZM.94vxa1rLR5LsEY0K2at.rvO', 'Ana Pop'),
       ('mihai@safepaws.dev', '$2a$10$SZ1Y3GiQdHfVw7WqLGmQfes7ONyV5Us0q1cdGg7Ixwmvyl8jVCc5O', 'Mihai Rus'),
       ('elena@safepaws.dev', '$2a$10$qxjF7SLjHthXvW.ESWU2X.ljGFSPCzcMHR4iQDKBcrQE7Lqx9sIW2', 'Elena Moldovan');

-- The demo announcements are shared between the three accounts, matched by their placeholder phone.
-- Each account gets lost and found announcements, at least one resolved one and one with 2 photos.
UPDATE announcement SET owner_id = (SELECT id FROM users WHERE email = 'ana@safepaws.dev')
WHERE contact_phone IN ('0700000001', '0700000004', '0700000007', '0700000010',
                        '0700000013', '0700000016', '0700000019', '0700000022');

UPDATE announcement SET owner_id = (SELECT id FROM users WHERE email = 'mihai@safepaws.dev')
WHERE contact_phone IN ('0700000002', '0700000008', '0700000009', '0700000011',
                        '0700000012', '0700000017', '0700000020');

UPDATE announcement SET owner_id = (SELECT id FROM users WHERE email = 'elena@safepaws.dev')
WHERE contact_phone IN ('0700000003', '0700000005', '0700000006', '0700000014',
                        '0700000015', '0700000018', '0700000021');
