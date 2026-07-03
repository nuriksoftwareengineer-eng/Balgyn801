-- V33: Key-value site settings table.
-- Enables admin-manageable content without code changes (CEO photo, etc.).

CREATE TABLE site_settings (
    key        VARCHAR(100) PRIMARY KEY,
    value      TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed: placeholder so the key exists and admin page can display it immediately.
INSERT INTO site_settings (key, value, updated_at)
VALUES ('ceo_photo_url', NULL, NOW());
