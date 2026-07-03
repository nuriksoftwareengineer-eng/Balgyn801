-- V42: Replace enum-based garment type weights with a proper DB entity (GarmentProfile).
--
-- Before: design_garments.garment_type = VARCHAR(30) with enum string values.
--         garment_type_weights has only weight_kg per type.
-- After:  garment_profiles table stores name + weight + L/W/H dimensions.
--         design_garments references garment_profiles via FK.
--         garment_type_weights is kept untouched (still used by size_chart_images).

-- ── 1. Create garment_profiles ────────────────────────────────────────────────
CREATE TABLE garment_profiles (
    id         BIGSERIAL     PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL UNIQUE,
    weight_kg  NUMERIC(6,3)  NOT NULL CHECK (weight_kg > 0),
    length_cm  SMALLINT      NOT NULL CHECK (length_cm > 0),
    width_cm   SMALLINT      NOT NULL CHECK (width_cm > 0),
    height_cm  SMALLINT      NOT NULL CHECK (height_cm > 0),
    sort_order SMALLINT      NOT NULL DEFAULT 0
);

-- ── 2. Seed initial profiles from existing garment type data ─────────────────
-- Weights are taken from garment_type_weights (admin-edited values).
-- Dimensions are standard folded-clothing defaults; admin can update via UI.
INSERT INTO garment_profiles (name, weight_kg, length_cm, width_cm, height_cm, sort_order)
SELECT 'T-Shirt',
       COALESCE((SELECT weight_kg FROM garment_type_weights WHERE garment_type = 'T_SHIRT'), 0.400),
       30, 25, 4, 1
WHERE NOT EXISTS (SELECT 1 FROM garment_profiles WHERE name = 'T-Shirt');

INSERT INTO garment_profiles (name, weight_kg, length_cm, width_cm, height_cm, sort_order)
SELECT 'Oversize T-Shirt',
       COALESCE((SELECT weight_kg FROM garment_type_weights WHERE garment_type = 'OVERSIZE_TSHIRT'), 0.500),
       35, 28, 5, 2
WHERE NOT EXISTS (SELECT 1 FROM garment_profiles WHERE name = 'Oversize T-Shirt');

INSERT INTO garment_profiles (name, weight_kg, length_cm, width_cm, height_cm, sort_order)
SELECT 'Long Sleeve',
       COALESCE((SELECT weight_kg FROM garment_type_weights WHERE garment_type = 'LONGSLEEVE'), 0.500),
       32, 26, 5, 3
WHERE NOT EXISTS (SELECT 1 FROM garment_profiles WHERE name = 'Long Sleeve');

INSERT INTO garment_profiles (name, weight_kg, length_cm, width_cm, height_cm, sort_order)
SELECT 'Sweatshirt',
       COALESCE((SELECT weight_kg FROM garment_type_weights WHERE garment_type = 'SWEATSHIRT'), 0.800),
       38, 30, 8, 4
WHERE NOT EXISTS (SELECT 1 FROM garment_profiles WHERE name = 'Sweatshirt');

INSERT INTO garment_profiles (name, weight_kg, length_cm, width_cm, height_cm, sort_order)
SELECT 'Hoodie',
       COALESCE((SELECT weight_kg FROM garment_type_weights WHERE garment_type = 'HOODIE'), 1.000),
       40, 32, 10, 5
WHERE NOT EXISTS (SELECT 1 FROM garment_profiles WHERE name = 'Hoodie');

INSERT INTO garment_profiles (name, weight_kg, length_cm, width_cm, height_cm, sort_order)
SELECT 'Zip Hoodie',
       COALESCE((SELECT weight_kg FROM garment_type_weights WHERE garment_type = 'ZIP_HOODIE'), 1.100),
       42, 34, 10, 6
WHERE NOT EXISTS (SELECT 1 FROM garment_profiles WHERE name = 'Zip Hoodie');

-- ── 3. Add garment_profile_id column to design_garments ───────────────────────
ALTER TABLE design_garments ADD COLUMN garment_profile_id BIGINT;

-- ── 4. Populate FK from existing garment_type values ─────────────────────────
UPDATE design_garments dg
SET garment_profile_id = gp.id
FROM garment_profiles gp
WHERE (dg.garment_type = 'T_SHIRT'        AND gp.name = 'T-Shirt')
   OR (dg.garment_type = 'OVERSIZE_TSHIRT' AND gp.name = 'Oversize T-Shirt')
   OR (dg.garment_type = 'LONGSLEEVE'      AND gp.name = 'Long Sleeve')
   OR (dg.garment_type = 'SWEATSHIRT'      AND gp.name = 'Sweatshirt')
   OR (dg.garment_type = 'HOODIE'          AND gp.name = 'Hoodie')
   OR (dg.garment_type = 'ZIP_HOODIE'      AND gp.name = 'Zip Hoodie');

-- ── 5. Make NOT NULL + add FK constraint ──────────────────────────────────────
ALTER TABLE design_garments
    ALTER COLUMN garment_profile_id SET NOT NULL;

ALTER TABLE design_garments
    ADD CONSTRAINT fk_dg_garment_profile
    FOREIGN KEY (garment_profile_id) REFERENCES garment_profiles(id);

-- ── 6. Replace old unique constraint + drop garment_type column ───────────────
ALTER TABLE design_garments
    DROP CONSTRAINT uq_design_garment_type;

ALTER TABLE design_garments
    ADD CONSTRAINT uq_design_garment_profile
    UNIQUE (design_id, garment_profile_id);

ALTER TABLE design_garments
    DROP COLUMN garment_type;
