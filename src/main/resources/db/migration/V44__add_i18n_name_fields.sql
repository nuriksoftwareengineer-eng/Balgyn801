-- V44: Add i18n name fields for multilingual catalog and garment profile display.
-- Catalog entities (name = Russian primary): add optional Kazakh + English columns.
-- GarmentProfile (name = English internal key): add Russian + Kazakh columns.

ALTER TABLE garment_profiles
    ADD COLUMN name_ru VARCHAR(100),
    ADD COLUMN name_kk VARCHAR(100),
    ADD COLUMN name_en VARCHAR(100);

ALTER TABLE catalog_groups
    ADD COLUMN name_kk VARCHAR(200),
    ADD COLUMN name_en VARCHAR(200);

ALTER TABLE collections
    ADD COLUMN name_kk VARCHAR(200),
    ADD COLUMN name_en VARCHAR(200);

ALTER TABLE designs
    ADD COLUMN name_kk VARCHAR(200),
    ADD COLUMN name_en VARCHAR(200);

-- Seed Russian + Kazakh translations for the 6 seeded garment profiles.
-- name_en = name (English is already the primary key).
UPDATE garment_profiles SET
    name_ru = CASE name
        WHEN 'T-Shirt'          THEN 'Футболка'
        WHEN 'Oversize T-Shirt' THEN 'Оверсайз футболка'
        WHEN 'Long Sleeve'      THEN 'Лонгслив'
        WHEN 'Sweatshirt'       THEN 'Свитшот'
        WHEN 'Hoodie'           THEN 'Худи'
        WHEN 'Zip Hoodie'       THEN 'Худи на молнии'
        ELSE name
    END,
    name_kk = CASE name
        WHEN 'T-Shirt'          THEN 'Футболка'
        WHEN 'Oversize T-Shirt' THEN 'Оверсайз жейде'
        WHEN 'Long Sleeve'      THEN 'Ұзын жеңді жейде'
        WHEN 'Sweatshirt'       THEN 'Свитшот'
        WHEN 'Hoodie'           THEN 'Худи'
        WHEN 'Zip Hoodie'       THEN 'Молниялы худи'
        ELSE name
    END,
    name_en = name;
