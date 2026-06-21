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
