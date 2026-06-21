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
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_inventory_quantity_non_negative'
          AND conrelid = 'inventory'::regclass
    ) THEN
        ALTER TABLE inventory
            ADD CONSTRAINT chk_inventory_quantity_non_negative
            CHECK (quantity >= 0);
    END IF;
END $$;

-- DB-M3: status column constraints
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_status'
          AND conrelid = 'orders'::regclass
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT chk_orders_status
            CHECK (status IN ('PENDING_PAYMENT','NEW','IN_PROGRESS','SHIPPED','DELIVERED','CANCELLED','EXPIRED'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_payments_status'
          AND conrelid = 'payments'::regclass
    ) THEN
        ALTER TABLE payments
            ADD CONSTRAINT chk_payments_status
            CHECK (status IN ('PENDING','COMPLETED','FAILED','REFUNDED'));
    END IF;
END $$;
