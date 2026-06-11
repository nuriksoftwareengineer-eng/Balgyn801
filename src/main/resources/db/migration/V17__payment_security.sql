-- V17: Payment security hardening.
-- 1. Prevent duplicate PENDING payments for the same order+provider (idempotency safety net).
-- 2. Track processed webhook events for replay protection.

-- Partial unique index: at most one PENDING payment per order+provider at a time.
-- Terminal states (SUCCEEDED, CANCELLED, FAILED, REFUNDED, EXPIRED) allow a new attempt.
CREATE UNIQUE INDEX idx_payments_order_provider_pending
    ON payments (order_id, provider)
    WHERE status = 'PENDING';

-- Replay protection table: each provider+event_id pair may only be processed once.
CREATE TABLE processed_webhook_events (
    id           BIGSERIAL    PRIMARY KEY,
    provider     VARCHAR(30)  NOT NULL,
    event_id     VARCHAR(128) NOT NULL,
    payment_id   BIGINT       REFERENCES payments (id),
    processed_at TIMESTAMP    NOT NULL,
    CONSTRAINT uq_webhook_event UNIQUE (provider, event_id)
);

CREATE INDEX idx_pwe_provider_event ON processed_webhook_events (provider, event_id);
