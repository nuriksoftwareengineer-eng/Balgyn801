-- V14: Payment-gated order state + delivery snapshot fields.
-- Orders are created as PENDING_PAYMENT and become visible to admin only after a successful
-- payment (CONFIRMED) or are EXPIRED by the cleanup job after the payment window. The snapshot
-- columns freeze the backend-computed delivery inputs onto the order at creation time.

-- Delivery snapshot on the order (all nullable; populated by the delivery pricing engine).
ALTER TABLE orders ADD COLUMN total_weight_kg       NUMERIC(7, 3);
ALTER TABLE orders ADD COLUMN shipping_zone         VARCHAR(20);
ALTER TABLE orders ADD COLUMN delivery_fee_usd      NUMERIC(10, 2);
ALTER TABLE orders ADD COLUMN exchange_rate_kzt_usd NUMERIC(12, 4);

-- Delivery snapshot on the address.
ALTER TABLE delivery_addresses ADD COLUMN pvz_code     VARCHAR(64);
ALTER TABLE delivery_addresses ADD COLUMN country_iso2 VARCHAR(2);
ALTER TABLE delivery_addresses ADD COLUMN city_code    INTEGER;

-- Speeds up both the admin list (status filter) and the expiry scan (status + created_at).
CREATE INDEX idx_orders_status            ON orders (status);
CREATE INDEX idx_orders_status_created_at ON orders (status, created_at);
