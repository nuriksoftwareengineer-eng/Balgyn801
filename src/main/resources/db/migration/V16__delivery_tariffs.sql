-- V16: Admin-editable weight-bracket tariffs for CIS postal and international (AIR) shipping.
-- A bracket applies to shipments up to upto_kg (inclusive); the smallest matching bracket wins.
-- The AIR table backs the single customer-facing "International Shipping" method; the customer
-- never sees the AIR/POSTAL distinction.

CREATE TABLE delivery_tariffs (
    id       BIGSERIAL     PRIMARY KEY,
    kind     VARCHAR(16)   NOT NULL,
    upto_kg  NUMERIC(7, 3) NOT NULL,
    base_kzt NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_delivery_tariffs_kind ON delivery_tariffs (kind, upto_kg);

-- International AIR brackets
INSERT INTO delivery_tariffs (kind, upto_kg, base_kzt) VALUES
    ('AIR', 0.500, 3000.00),
    ('AIR', 1.000, 4000.00),
    ('AIR', 2.000, 6000.00),
    ('AIR', 5.000, 10000.00),
    ('AIR', 10.000, 18000.00);

-- CIS postal brackets
INSERT INTO delivery_tariffs (kind, upto_kg, base_kzt) VALUES
    ('POSTAL', 0.500, 2000.00),
    ('POSTAL', 1.000, 2800.00),
    ('POSTAL', 2.000, 4000.00),
    ('POSTAL', 5.000, 7000.00),
    ('POSTAL', 10.000, 12000.00);
