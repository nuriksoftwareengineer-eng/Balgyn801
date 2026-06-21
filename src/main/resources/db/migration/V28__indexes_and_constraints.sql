-- V28: Performance indexes and data-integrity constraints.
-- DB-C1: index for CDEK webhook lookup (findByCdekOrderUuid)
CREATE INDEX IF NOT EXISTS idx_cdek_shipments_cdek_order_uuid
    ON cdek_shipments (cdek_order_uuid);

-- DB-L1: admin order list sorts/filters by created_at
CREATE INDEX IF NOT EXISTS idx_orders_created_at
    ON orders (created_at DESC);

-- DB-L2: payment lookup by order
CREATE INDEX IF NOT EXISTS idx_payments_order_id
    ON payments (order_id);

-- DB-M2: prevent negative inventory (business invariant)
ALTER TABLE inventory
    ADD CONSTRAINT IF NOT EXISTS chk_inventory_quantity_non_negative
    CHECK (quantity >= 0);

-- DB-M3: status column constraints
ALTER TABLE orders
    ADD CONSTRAINT IF NOT EXISTS chk_orders_status
    CHECK (status IN ('PENDING_PAYMENT','NEW','IN_PROGRESS','SHIPPED','DELIVERED','CANCELLED','EXPIRED'));

ALTER TABLE payments
    ADD CONSTRAINT IF NOT EXISTS chk_payments_status
    CHECK (status IN ('PENDING','COMPLETED','FAILED','REFUNDED'));
