-- Seed catalog groups
INSERT INTO catalog_groups (name, slug, sort_order, active, created_at) VALUES
  (E'Аниме',  'anime',  1, true, NOW()),
  (E'Игры',   'games',  2, true, NOW()),
  (E'Музыка', 'music',  3, true, NOW()),
  (E'Спорт',  'sport',  4, true, NOW()),
  (E'Кино',   'movies', 5, true, NOW())
ON CONFLICT (slug) DO NOTHING;

-- Anime collections
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, E'Берсерк', 'berserk', 1, true, NOW()
  FROM catalog_groups WHERE slug='anime' ON CONFLICT (slug) DO NOTHING;
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, E'Наруто', 'naruto', 2, true, NOW()
  FROM catalog_groups WHERE slug='anime' ON CONFLICT (slug) DO NOTHING;
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, E'Атака Титанов', 'attack-on-titan', 3, true, NOW()
  FROM catalog_groups WHERE slug='anime' ON CONFLICT (slug) DO NOTHING;

-- Games collections
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, 'CS2', 'cs2', 1, true, NOW()
  FROM catalog_groups WHERE slug='games' ON CONFLICT (slug) DO NOTHING;
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, 'Dota 2', 'dota-2', 2, true, NOW()
  FROM catalog_groups WHERE slug='games' ON CONFLICT (slug) DO NOTHING;
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, 'Valorant', 'valorant', 3, true, NOW()
  FROM catalog_groups WHERE slug='games' ON CONFLICT (slug) DO NOTHING;
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, 'GTA', 'gta', 4, true, NOW()
  FROM catalog_groups WHERE slug='games' ON CONFLICT (slug) DO NOTHING;
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, 'Minecraft', 'minecraft', 5, true, NOW()
  FROM catalog_groups WHERE slug='games' ON CONFLICT (slug) DO NOTHING;

-- Music collections
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, 'Metallica', 'metallica', 1, true, NOW()
  FROM catalog_groups WHERE slug='music' ON CONFLICT (slug) DO NOTHING;
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, 'Linkin Park', 'linkin-park', 2, true, NOW()
  FROM catalog_groups WHERE slug='music' ON CONFLICT (slug) DO NOTHING;

-- Sport collections
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, E'ММА', 'mma', 1, true, NOW()
  FROM catalog_groups WHERE slug='sport' ON CONFLICT (slug) DO NOTHING;
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, E'Футбол', 'football', 2, true, NOW()
  FROM catalog_groups WHERE slug='sport' ON CONFLICT (slug) DO NOTHING;

-- Movies collections
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, 'Marvel', 'marvel', 1, true, NOW()
  FROM catalog_groups WHERE slug='movies' ON CONFLICT (slug) DO NOTHING;
INSERT INTO collections (group_id, name, slug, sort_order, active, created_at)
SELECT id, 'Star Wars', 'star-wars', 2, true, NOW()
  FROM catalog_groups WHERE slug='movies' ON CONFLICT (slug) DO NOTHING;

-- Colors
INSERT INTO colors (name, hex_code) VALUES
  ('Black',  '#000000'),
  ('White',  '#FFFFFF'),
  ('Navy',   '#1B2A4A'),
  ('Red',    '#C0392B')
ON CONFLICT DO NOTHING;

-- Sizes
INSERT INTO sizes (label) VALUES ('S'),('M'),('L'),('XL'),('XXL')
ON CONFLICT DO NOTHING;

-- One sample design: Brand of Sacrifice (Berserk collection)
INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id,
  'Brand of Sacrifice',
  'brand-of-sacrifice',
  E'ܘнак Жертвы из манги Берсерк. Вышитая вручную, полноцветными нитями.',
  NULL,
  'PUBLISHED',
  NOW()
FROM collections c WHERE c.slug = 'berserk'
ON CONFLICT (slug) DO NOTHING;

-- DesignGarment rows for brand-of-sacrifice
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE', true FROM designs d WHERE d.slug='brand-of-sacrifice'
ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT', true FROM designs d WHERE d.slug='brand-of-sacrifice'
ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='brand-of-sacrifice'
ON CONFLICT DO NOTHING;

-- DesignGarmentPrices (KZT)
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 12000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='brand-of-sacrifice' AND dg.garment_type='HOODIE'
ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 8500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='brand-of-sacrifice' AND dg.garment_type='T_SHIRT'
ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 10500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='brand-of-sacrifice' AND dg.garment_type='SWEATSHIRT'
ON CONFLICT DO NOTHING;

-- Colors on each garment
INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id,
  colors col
WHERE d.slug='brand-of-sacrifice' AND col.name IN ('Black','White','Navy')
ON CONFLICT DO NOTHING;

-- Sizes on each garment
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id,
  sizes sz
WHERE d.slug='brand-of-sacrifice' AND sz.label IN ('S','M','L','XL')
ON CONFLICT DO NOTHING;

-- Inventory
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 10
FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','White','Navy')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL')) sz
WHERE d.slug='brand-of-sacrifice'
ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- Second sample design: Counter-Strike (CS2 collection)
INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id,
  'Counter-Strike',
  'counter-strike',
  E'Классическая эмблема CS2 с вышивкой CT vs T.',
  NULL,
  'PUBLISHED',
  NOW()
FROM collections c WHERE c.slug='cs2'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE', true FROM designs d WHERE d.slug='counter-strike' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT', true FROM designs d WHERE d.slug='counter-strike' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 11000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='counter-strike' AND dg.garment_type='HOODIE'
ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 7500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='counter-strike' AND dg.garment_type='T_SHIRT'
ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='counter-strike' AND col.name IN ('Black','White')
ON CONFLICT DO NOTHING;

INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='counter-strike' AND sz.label IN ('M','L','XL','XXL')
ON CONFLICT DO NOTHING;

INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 8
FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','White')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('M','L','XL','XXL')) sz
WHERE d.slug='counter-strike'
ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ────────────────────────────────────────────────────────────────────────────
-- Extended catalog: 14 additional designs across all remaining collections
-- ────────────────────────────────────────────────────────────────────────────

-- Helper: a macro-style block is not available in plain SQL, so each design
-- follows the same 6-step pattern: design → garments → prices → colors →
-- sizes → inventory.  All inserts are idempotent (ON CONFLICT DO NOTHING).

-- Additional colors (Olive and Gray for variety)
INSERT INTO colors (name, hex_code) VALUES
  ('Olive', '#4B5320'),
  ('Gray',  '#6B7280')
ON CONFLICT DO NOTHING;

-- ── АНИМЕ > БЕРСЕРК ─────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Guts', 'guts',
  'Силуэт Гатса с мечом Убийцей Дракона. Монохромная вышивка.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='berserk' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='guts' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='guts' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='guts' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 13000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='guts' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 9000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='guts' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 11000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='guts' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='guts' AND col.name IN ('Black','Gray') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='guts' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 12 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','Gray')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='guts' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── АНИМЕ > НАРУТО ──────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Sharingan', 'sharingan',
  'Одиночный шаринган Учиха Итачи. Трёхтомаэ, полноцветная вышивка.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='naruto' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='sharingan' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='sharingan' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='sharingan' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 12500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='sharingan' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 8500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='sharingan' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 10500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='sharingan' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='sharingan' AND col.name IN ('Black','Navy') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='sharingan' AND sz.label IN ('S','M','L','XL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 15 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','Navy')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL')) sz
WHERE d.slug='sharingan' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── АНИМЕ > АТАКА ТИТАНОВ ───────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Survey Corps', 'survey-corps',
  'Эмблема Разведывательного Корпуса. Крылья Свободы, вышитые нитями металлик.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='attack-on-titan' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='survey-corps' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='survey-corps' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='survey-corps' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 13500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='survey-corps' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 9500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='survey-corps' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 11500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='survey-corps' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='survey-corps' AND col.name IN ('Black','White','Olive') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='survey-corps' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 10 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','White','Olive')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='survey-corps' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── ИГРЫ > CS2 ──────────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'AK-47 Redline', 'ak47-redline',
  'Культовый АК-47 в скине Redline. Точная вышивка с деталями текстуры.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='cs2' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='ak47-redline' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='ak47-redline' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 11500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='ak47-redline' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 8000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='ak47-redline' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='ak47-redline' AND col.name IN ('Black','Red') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='ak47-redline' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 8 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','Red')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='ak47-redline' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── ИГРЫ > DOTA 2 ───────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Invoker', 'invoker',
  'Carl — Invoker в полном облачении. Орбы Quas, Wex, Exort вышиты трёхцветной нитью.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='dota-2' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='invoker' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='invoker' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='invoker' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 12000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='invoker' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 8500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='invoker' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 10000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='invoker' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='invoker' AND col.name IN ('Black','Navy','White') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='invoker' AND sz.label IN ('S','M','L','XL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 10 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','Navy','White')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL')) sz
WHERE d.slug='invoker' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── ИГРЫ > VALORANT ─────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Vandal', 'vandal',
  'Культовый Vandal с оскалом. Оружие-символ Valorant, вышитое по контуру.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='valorant' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='vandal' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='vandal' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 12500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='vandal' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 8500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='vandal' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='vandal' AND col.name IN ('Black','Red') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='vandal' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 10 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','Red')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='vandal' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── ИГРЫ > GTA ──────────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Los Santos', 'los-santos',
  'Логотип Los Santos с пальмой и закатом. Цветная вышивка в стиле неон.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='gta' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='los-santos' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='los-santos' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='los-santos' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 12000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='los-santos' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 8000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='los-santos' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 10000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='los-santos' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='los-santos' AND col.name IN ('Black','White','Gray') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='los-santos' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 10 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','White','Gray')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='los-santos' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── ИГРЫ > MINECRAFT ────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Creeper', 'creeper',
  'Культовый Крипер в пиксельном стиле. Зелёная пиксельная вышивка на чёрном.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='minecraft' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='creeper' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='creeper' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='creeper' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 11500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='creeper' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 7500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='creeper' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 9500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='creeper' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='creeper' AND col.name IN ('Black','White') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='creeper' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 15 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','White')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='creeper' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── МУЗЫКА > METALLICA ──────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Master of Puppets', 'master-of-puppets',
  'Обложка альбома Master of Puppets. Кресты и нити — культовый металл-образ.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='metallica' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='master-of-puppets' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='master-of-puppets' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='master-of-puppets' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 13000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='master-of-puppets' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 9000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='master-of-puppets' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 11000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='master-of-puppets' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='master-of-puppets' AND col.name IN ('Black','White') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='master-of-puppets' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 8 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','White')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='master-of-puppets' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── МУЗЫКА > LINKIN PARK ────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Hybrid Theory', 'hybrid-theory',
  'Обложка дебютного альбома Hybrid Theory с воином-ангелом. Вышивка в два цвета.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='linkin-park' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='hybrid-theory' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='hybrid-theory' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='hybrid-theory' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 12000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='hybrid-theory' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 8000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='hybrid-theory' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 10000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='hybrid-theory' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='hybrid-theory' AND col.name IN ('Black','White','Gray') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='hybrid-theory' AND sz.label IN ('S','M','L','XL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 10 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','White','Gray')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL')) sz
WHERE d.slug='hybrid-theory' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── СПОРТ > ММА ─────────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'UFC Octagon', 'ufc-octagon',
  'Логотип UFC с октагоном. Металлик-нить, объёмная вышивка на груди.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='mma' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='ufc-octagon' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='ufc-octagon' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='ufc-octagon' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 14000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='ufc-octagon' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 9500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='ufc-octagon' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 12000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='ufc-octagon' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='ufc-octagon' AND col.name IN ('Black','White','Red') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='ufc-octagon' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 12 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','White','Red')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='ufc-octagon' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── СПОРТ > ФУТБОЛ ──────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'El Clasico', 'el-clasico',
  'Эмблема Эль-Класико — противостояние Real Madrid vs Barcelona. Золотые нити.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='football' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='el-clasico' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='el-clasico' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='el-clasico' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 13500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='el-clasico' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 9000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='el-clasico' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 11500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='el-clasico' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='el-clasico' AND col.name IN ('Black','White','Navy') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='el-clasico' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 10 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','White','Navy')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='el-clasico' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── КИНО > MARVEL ───────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Spider-Man', 'spider-man',
  'Маска Человека-Паука с паутиной. Красно-синяя вышивка, объёмный паук на груди.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='marvel' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='spider-man' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='spider-man' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='spider-man' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 13000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='spider-man' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 8500.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='spider-man' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 11000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='spider-man' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='spider-man' AND col.name IN ('Black','Navy','Red') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='spider-man' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 15 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','Navy','Red')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='spider-man' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;

-- ── КИНО > STAR WARS ────────────────────────────────────────────────────────

INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Darth Vader', 'darth-vader',
  'Шлем Дарта Вейдера в анфас. Классика Тёмной Стороны, монохромная вышивка.',
  NULL, 'PUBLISHED', NOW()
FROM collections c WHERE c.slug='star-wars' ON CONFLICT (slug) DO NOTHING;

INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'HOODIE',     true FROM designs d WHERE d.slug='darth-vader' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'T_SHIRT',    true FROM designs d WHERE d.slug='darth-vader' ON CONFLICT DO NOTHING;
INSERT INTO design_garments (design_id, garment_type, active)
SELECT d.id, 'SWEATSHIRT', true FROM designs d WHERE d.slug='darth-vader' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 13000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='darth-vader' AND dg.garment_type='HOODIE' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 9000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='darth-vader' AND dg.garment_type='T_SHIRT' ON CONFLICT DO NOTHING;
INSERT INTO design_garment_prices (design_garment_id, currency, amount)
SELECT dg.id, 'KZT', 11000.00 FROM design_garments dg
  JOIN designs d ON dg.design_id=d.id WHERE d.slug='darth-vader' AND dg.garment_type='SWEATSHIRT' ON CONFLICT DO NOTHING;

INSERT INTO design_garment_colors (design_garment_id, color_id)
SELECT dg.id, col.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, colors col
WHERE d.slug='darth-vader' AND col.name IN ('Black','White','Gray') ON CONFLICT DO NOTHING;
INSERT INTO design_garment_sizes (design_garment_id, size_id)
SELECT dg.id, sz.id FROM design_garments dg JOIN designs d ON dg.design_id=d.id, sizes sz
WHERE d.slug='darth-vader' AND sz.label IN ('S','M','L','XL','XXL') ON CONFLICT DO NOTHING;
INSERT INTO inventory (design_garment_id, color_id, size_id, quantity)
SELECT dg.id, col.id, sz.id, 12 FROM design_garments dg JOIN designs d ON dg.design_id=d.id
  CROSS JOIN (SELECT id FROM colors WHERE name IN ('Black','White','Gray')) col
  CROSS JOIN (SELECT id FROM sizes WHERE label IN ('S','M','L','XL','XXL')) sz
WHERE d.slug='darth-vader' ON CONFLICT (design_garment_id, color_id, size_id) DO NOTHING;
