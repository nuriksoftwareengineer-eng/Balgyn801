-- V29: Fix payments status constraint.
-- V28 had 'COMPLETED' which doesn't exist in PaymentStatus enum.
-- Correct values: PENDING, SUCCEEDED, CANCELLED, FAILED, REFUNDED.
ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payments_status;
ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING','SUCCEEDED','CANCELLED','FAILED','REFUNDED'));
