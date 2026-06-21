-- V20: Admin catalog enrichment (non-breaking, additive only).
--   Collection: описание + обложка + баннер для лендингов категории/коллекции.
--   Design:     галерея изображений в дополнение к существующему главному фото (main_image_url).

ALTER TABLE collections
    ADD COLUMN description      TEXT,
    ADD COLUMN cover_image_url  VARCHAR(512),
    ADD COLUMN banner_image_url VARCHAR(512);

ALTER TABLE designs
    ADD COLUMN gallery jsonb NOT NULL DEFAULT '[]'::jsonb;
