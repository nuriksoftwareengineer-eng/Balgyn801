-- V5: Design garments, multi-currency prices, and color/size availability per garment.

CREATE TABLE design_garments (
    id           BIGSERIAL    PRIMARY KEY,
    design_id    BIGINT       NOT NULL REFERENCES designs (id),
    garment_type VARCHAR(30)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_design_garments_design_id ON design_garments (design_id);

CREATE TABLE design_garment_prices (
    id                 BIGSERIAL      PRIMARY KEY,
    design_garment_id  BIGINT         NOT NULL REFERENCES design_garments (id),
    currency           VARCHAR(3)     NOT NULL,
    amount             NUMERIC(12, 2) NOT NULL,
    UNIQUE (design_garment_id, currency)
);

-- Available colors per garment variant
CREATE TABLE design_garment_colors (
    design_garment_id BIGINT NOT NULL REFERENCES design_garments (id),
    color_id          BIGINT NOT NULL REFERENCES colors (id),
    PRIMARY KEY (design_garment_id, color_id)
);

-- Available sizes per garment variant
CREATE TABLE design_garment_sizes (
    design_garment_id BIGINT NOT NULL REFERENCES design_garments (id),
    size_id           BIGINT NOT NULL REFERENCES sizes (id),
    PRIMARY KEY (design_garment_id, size_id)
);
