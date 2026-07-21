-- V55: Localize product-page content added in V54 — Materials gets RU/KK columns
-- (existing material_description stays as the base/EN value, same convention as
-- garment_profiles.name vs name_ru/name_kk). Production/Delivery/Shipping/Care
-- Instructions move from one key each to three (_ru/_kk/_en), same site_settings
-- table, no new structure.

-- ── 1. Materials ─────────────────────────────────────────────────────────────
ALTER TABLE garment_profiles
    ADD COLUMN material_description_ru TEXT,
    ADD COLUMN material_description_kk TEXT;

-- ── 2. Site settings — replace V54's 4 single-language keys with 12 ────────────
DELETE FROM site_settings
WHERE key IN ('production_description', 'delivery_description', 'shipping_description', 'care_instructions');

INSERT INTO site_settings (key, value, updated_at) VALUES
    ('production_description_ru', 'Изготовление вручную под заказ.
Производство занимает 5–10 рабочих дней.', NOW()),
    ('production_description_kk', 'Тапсырыс бойынша қолмен жасалады.
Өндіріс 5–10 жұмыс күнін алады.', NOW()),
    ('production_description_en', 'Handmade after order.
Production takes 5–10 business days.', NOW()),

    ('delivery_description_ru', 'Доставка по всему миру.
Срок зависит от страны получателя.', NOW()),
    ('delivery_description_kk', 'Дүние жүзі бойынша жеткізу.
Мерзімі алушы еліне байланысты.', NOW()),
    ('delivery_description_en', 'Worldwide delivery.
Delivery time depends on destination country.', NOW()),

    ('shipping_description_ru', 'Заказы изготавливаются за 7–10 рабочих дней.
Сроки доставки:
Европа — 2–3 недели.
США — 2–4 недели.
Азия — 3–5 недель.', NOW()),
    ('shipping_description_kk', 'Тапсырыстар 7–10 жұмыс күнінде дайындалады.
Жеткізу мерзімдері:
Еуропа — 2–3 апта.
АҚШ — 2–4 апта.
Азия — 3–5 апта.', NOW()),
    ('shipping_description_en', 'Orders are produced within 7–10 business days.
Delivery time:
Europe — 2–3 weeks.
USA — 2–4 weeks.
Asia — 3–5 weeks.', NOW()),

    ('care_instructions_ru', 'Информация по уходу появится позже.', NOW()),
    ('care_instructions_kk', 'Күтім туралы ақпарат жақында қосылады.', NOW()),
    ('care_instructions_en', 'Care instructions will be available soon.', NOW());
