-- Extend order_items to support design-based orders alongside existing product-based orders.
-- All columns are nullable so existing rows are unaffected.

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS design_garment_id BIGINT NULL
        REFERENCES design_garments(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS color_id          BIGINT NULL
        REFERENCES colors(id)          ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS size_id           BIGINT NULL
        REFERENCES sizes(id)           ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS currency          VARCHAR(3) NULL;
