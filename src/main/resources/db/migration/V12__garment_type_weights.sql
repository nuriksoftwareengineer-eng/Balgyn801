-- V12: Admin-editable physical weight per garment type (kg).
-- Weight is a property of the garment TYPE, not of a specific design, so one row per type
-- applies to every design that uses it. This table is the authoritative source for order-weight
-- calculation; the GarmentType enum carries the same values only as a runtime fallback.

CREATE TABLE garment_type_weights (
    garment_type VARCHAR(30)   PRIMARY KEY,
    weight_kg    NUMERIC(6, 3) NOT NULL CHECK (weight_kg > 0)
);

INSERT INTO garment_type_weights (garment_type, weight_kg) VALUES
    ('T_SHIRT',         0.400),
    ('OVERSIZE_TSHIRT', 0.500),
    ('LONGSLEEVE',      0.500),
    ('SWEATSHIRT',      0.800),
    ('HOODIE',          1.000),
    ('ZIP_HOODIE',      1.100);
