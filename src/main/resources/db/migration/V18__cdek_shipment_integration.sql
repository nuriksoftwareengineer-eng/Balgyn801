-- V18: Готовность к реальной интеграции CDEK API v2.
-- Расширяем СУЩЕСТВУЮЩУЮ таблицу cdek_shipments (OneToOne с orders) недостающими полями
-- отправления. uuid/track/status уже есть (cdek_order_uuid, tracking_number, status) —
-- не дублируем. На orders ничего не добавляем: данные доставки живут в выделенной таблице.

ALTER TABLE cdek_shipments ADD COLUMN IF NOT EXISTS delivery_point_code    VARCHAR(64);
ALTER TABLE cdek_shipments ADD COLUMN IF NOT EXISTS delivery_point_address VARCHAR(512);
ALTER TABLE cdek_shipments ADD COLUMN IF NOT EXISTS delivery_price         NUMERIC(10, 2);
-- Режим/способ доставки CDEK (например "door-pvz"); тариф уже в tariff_code.
ALTER TABLE cdek_shipments ADD COLUMN IF NOT EXISTS cdek_delivery_mode     VARCHAR(32);
ALTER TABLE cdek_shipments ADD COLUMN IF NOT EXISTS invoice_url            VARCHAR(512);
ALTER TABLE cdek_shipments ADD COLUMN IF NOT EXISTS barcode_url            VARCHAR(512);
-- Полный сырой ответ API провайдера (аудит/диагностика); в mock-режиме — mock-payload.
ALTER TABLE cdek_shipments ADD COLUMN IF NOT EXISTS raw_response           TEXT;

-- Габариты товара для упаковки CDEK (опциональны). Только для legacy-товаров:
-- вес design-вариантов берётся из garment_type_weights через GarmentWeightService
-- (логику веса не дублируем). NULL = взять значения по умолчанию из конфигурации.
ALTER TABLE products ADD COLUMN IF NOT EXISTS weight_grams INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS length_cm    INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS width_cm     INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS height_cm    INTEGER;
