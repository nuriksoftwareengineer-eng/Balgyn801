-- V54: Product page content — material description per garment type, plus four
-- new store-wide site_settings for the product-page info cards/accordion.

-- ── 1. Materials — belongs to the clothing type, not duplicated per design ─────
-- Free-text, no length cap (same convention as Design.description / Collection.description).
ALTER TABLE garment_profiles
    ADD COLUMN material_description TEXT;

-- ── 2. Production / Delivery / Shipping / Care Instructions — global, admin-editable ──
-- Reuses the existing key-value site_settings table (see V33); seeded with sensible
-- defaults so the product page doesn't ship with an empty info block on day one.
INSERT INTO site_settings (key, value, updated_at) VALUES
    ('production_description', 'Handmade after order.
Production takes 5–10 business days.', NOW()),
    ('delivery_description', 'Worldwide delivery.
Delivery time depends on destination country.', NOW()),
    ('shipping_description', 'Orders are produced within 7–10 business days.
Delivery time:
Europe — 2–3 weeks.
USA — 2–4 weeks.
Asia — 3–5 weeks.', NOW()),
    ('care_instructions', 'Care instructions will be available soon.', NOW());
