-- V43: Fix garment_profiles column types to match the JPA entity (Integer / INT4).
-- V42 created length_cm, width_cm, height_cm, sort_order as SMALLINT which causes
-- Hibernate schema validation to fail (expects INTEGER / INT4).

ALTER TABLE garment_profiles
    ALTER COLUMN length_cm  TYPE INTEGER,
    ALTER COLUMN width_cm   TYPE INTEGER,
    ALTER COLUMN height_cm  TYPE INTEGER,
    ALTER COLUMN sort_order TYPE INTEGER;
