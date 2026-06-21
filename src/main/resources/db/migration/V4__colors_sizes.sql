-- V4: Global color and size catalogs.

CREATE TABLE colors (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    hex_code   VARCHAR(7),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE sizes (
    id         BIGSERIAL PRIMARY KEY,
    label      VARCHAR(50) NOT NULL,
    sort_order INTEGER     NOT NULL DEFAULT 0
);
