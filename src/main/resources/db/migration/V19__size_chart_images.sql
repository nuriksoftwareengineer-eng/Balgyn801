-- V19: size chart images per garment type.
-- One image per garment type (UPSERT by garment_type in the service layer).
CREATE TABLE IF NOT EXISTS size_chart_images (
    id           BIGSERIAL PRIMARY KEY,
    garment_type VARCHAR(30)  NOT NULL UNIQUE,
    image_url    VARCHAR(512) NOT NULL,
    title        VARCHAR(128),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
