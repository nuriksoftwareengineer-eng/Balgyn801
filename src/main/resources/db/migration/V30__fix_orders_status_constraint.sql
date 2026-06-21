-- V30: Fix orders status constraint.
--
-- V28 (chk_orders_status) still encoded the OLD status model: it allowed 'IN_PROGRESS' and was
-- MISSING the current OrderStatus enum values CONFIRMED, IN_PRODUCTION and READY.
-- After a successful PayPal / Freedom Pay capture the code runs `order.setStatus(CONFIRMED)`,
-- which violated the stale constraint:
--     ERROR: new row for relation "orders" violates check constraint "chk_orders_status"
-- so the payment succeeded but the order was never confirmed.
--
-- Current OrderStatus enum (com.nurba.java.enums.OrderStatus):
--   PENDING_PAYMENT, NEW, CONFIRMED, IN_PRODUCTION, READY, SHIPPED, DELIVERED, CANCELLED, EXPIRED
--
-- Same class of bug as V29 (which fixed chk_payments_status: V28 had a non-existent 'COMPLETED').

-- Order matters: drop the stale constraint FIRST, then migrate legacy data, then add the new
-- constraint. Migrating before the drop would fail — the OLD constraint forbids 'IN_PRODUCTION'.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_status;

-- Migrate any legacy rows still holding the removed 'IN_PROGRESS' value to its closest current
-- equivalent (IN_PRODUCTION = «в работе»), otherwise the rebuilt constraint would reject them.
UPDATE orders SET status = 'IN_PRODUCTION' WHERE status = 'IN_PROGRESS';

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status
        CHECK (status IN (
            'PENDING_PAYMENT',
            'NEW',
            'CONFIRMED',
            'IN_PRODUCTION',
            'READY',
            'SHIPPED',
            'DELIVERED',
            'CANCELLED',
            'EXPIRED'
        ));
