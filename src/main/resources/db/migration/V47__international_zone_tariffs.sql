-- Международная доставка: страна → тарифная зона → цена (Авиа/Наземная).
-- Тарифная зона страны (заполняется импортом таблицы «страна → зона»; NULL = доставка недоступна).
ALTER TABLE countries ADD COLUMN intl_zone VARCHAR(20);

-- Цена за зону и тип перевозки. Данные загружаются импортом таблицы «зона → стоимость»
-- (отдельная data-миграция) — в коде никаких захардкоженных цен нет.
CREATE TABLE intl_zone_tariffs (
    id        BIGSERIAL PRIMARY KEY,
    zone      VARCHAR(20)   NOT NULL,
    kind      VARCHAR(10)   NOT NULL CHECK (kind IN ('AIR', 'GROUND')),
    price_kzt NUMERIC(12,2) NOT NULL,
    CONSTRAINT uq_intl_zone_tariffs UNIQUE (zone, kind)
);
