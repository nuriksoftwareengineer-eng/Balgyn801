-- V13: Backend-controlled country list with shipping-zone classification.
-- The customer selects a country by ISO2 only; the backend owns the zone (KAZAKHSTAN / CIS /
-- INTERNATIONAL) and derives allowed delivery methods and pricing from it. The zone is never
-- exposed to the storefront.

CREATE TABLE countries (
    id            BIGSERIAL    PRIMARY KEY,
    iso2          VARCHAR(2)   NOT NULL,
    name_ru       VARCHAR(100) NOT NULL,
    name_en       VARCHAR(100) NOT NULL,
    shipping_zone VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_countries_iso2 UNIQUE (iso2)
);

CREATE INDEX idx_countries_active ON countries (active);

-- Home country
INSERT INTO countries (iso2, name_ru, name_en, shipping_zone, active) VALUES
    ('KZ', 'Казахстан',      'Kazakhstan',  'KAZAKHSTAN', TRUE);

-- CIS
INSERT INTO countries (iso2, name_ru, name_en, shipping_zone, active) VALUES
    ('RU', 'Россия',         'Russia',      'CIS', TRUE),
    ('BY', 'Беларусь',       'Belarus',     'CIS', TRUE),
    ('KG', 'Кыргызстан',     'Kyrgyzstan',  'CIS', TRUE),
    ('UZ', 'Узбекистан',     'Uzbekistan',  'CIS', TRUE),
    ('AM', 'Армения',        'Armenia',     'CIS', TRUE),
    ('AZ', 'Азербайджан',    'Azerbaijan',  'CIS', TRUE),
    ('TJ', 'Таджикистан',    'Tajikistan',  'CIS', TRUE),
    ('MD', 'Молдова',        'Moldova',     'CIS', TRUE);

-- International (examples; admin manages the full list)
INSERT INTO countries (iso2, name_ru, name_en, shipping_zone, active) VALUES
    ('US', 'США',            'United States',        'INTERNATIONAL', TRUE),
    ('GB', 'Великобритания', 'United Kingdom',       'INTERNATIONAL', TRUE),
    ('DE', 'Германия',       'Germany',              'INTERNATIONAL', TRUE),
    ('TR', 'Турция',         'Turkey',               'INTERNATIONAL', TRUE),
    ('AE', 'ОАЭ',            'United Arab Emirates', 'INTERNATIONAL', TRUE),
    ('CN', 'Китай',          'China',                'INTERNATIONAL', TRUE);
