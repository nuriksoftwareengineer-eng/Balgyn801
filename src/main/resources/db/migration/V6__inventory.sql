-- V6: Inventory — quantity per (design_garment, color, size).

CREATE TABLE inventory (
    id                 BIGSERIAL PRIMARY KEY,
    design_garment_id  BIGINT    NOT NULL REFERENCES design_garments (id),
    color_id           BIGINT    NOT NULL REFERENCES colors (id),
    size_id            BIGINT    NOT NULL REFERENCES sizes (id),
    quantity           INTEGER   NOT NULL DEFAULT 0,
    UNIQUE (design_garment_id, color_id, size_id)
);
