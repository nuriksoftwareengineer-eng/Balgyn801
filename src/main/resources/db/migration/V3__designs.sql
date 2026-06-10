-- V3: Designs — the primary product entity.

CREATE TABLE designs (
    id              BIGSERIAL PRIMARY KEY,
    collection_id   BIGINT       NOT NULL REFERENCES collections (id),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL UNIQUE,
    description     TEXT,
    main_image_url  VARCHAR(512),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_designs_collection_id ON designs (collection_id);
