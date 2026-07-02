-- V32: Add cover_image_url and banner_image_url to catalog_groups.
-- Same pattern as Collection (V20). Used on catalog index (card) and group detail (banner).

ALTER TABLE catalog_groups
    ADD COLUMN IF NOT EXISTS cover_image_url  VARCHAR(512),
    ADD COLUMN IF NOT EXISTS banner_image_url VARCHAR(512);
