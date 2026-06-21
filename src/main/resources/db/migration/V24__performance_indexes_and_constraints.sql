-- V24: Performance indexes on high-traffic FK columns + DB-level one-to-one constraints.
-- Audit finding: several hot FK columns had no indexes, causing full table scans at scale.

-- ── Critical read-path indexes ────────────────────────────────────────────────

-- Every order detail page loads items by order_id — was a full scan.
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);

-- Every payment lookup for an order was a full scan.
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments (order_id);

-- Order status history lookup by order.
CREATE INDEX IF NOT EXISTS idx_order_history_order_id ON order_history (order_id);

-- Auth: every login checks user_roles by user_id.
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles (user_id);

-- Webhook event lookup by payment.
CREATE INDEX IF NOT EXISTS idx_processed_webhook_events_payment_id
    ON processed_webhook_events (payment_id);

-- Custom designs by customer.
CREATE INDEX IF NOT EXISTS idx_custom_designs_customer_id ON custom_designs (customer_id);

-- ── One-to-one DB enforcement ─────────────────────────────────────────────────
-- JPA maps these as @OneToOne but there was no DB-level UNIQUE to back it up.

CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_addresses_order_id
    ON delivery_addresses (order_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cdek_shipments_order_id
    ON cdek_shipments (order_id);

-- ── Prevent double-succeeded payment per order+provider ───────────────────────
-- V17 already prevents two PENDING payments. This closes the gap for SUCCEEDED.
CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_order_provider_succeeded
    ON payments (order_id, provider)
    WHERE status = 'SUCCEEDED';

-- ── Delivery tariff deduplication ─────────────────────────────────────────────
-- Without this a duplicate bracket row causes non-deterministic tariff lookups.
CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_tariffs_kind_upto_kg
    ON delivery_tariffs (kind, upto_kg);
