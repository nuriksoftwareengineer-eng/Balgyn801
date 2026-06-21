-- V21: Replace KASPI/YOOKASSA/PAYPAL with FREEDOM_PAY.
-- No real payments existed for YOOKASSA/PAYPAL — only stub/test KASPI rows.
-- Migrate any existing KASPI rows, then delete any YOOKASSA/PAYPAL rows.

UPDATE payments
    SET provider = 'FREEDOM_PAY'
    WHERE provider = 'KASPI';

DELETE FROM payments
    WHERE provider IN ('YOOKASSA', 'PAYPAL');

-- Migrate or clean replay-protection records for the same providers.
UPDATE processed_webhook_events
    SET provider = 'FREEDOM_PAY'
    WHERE provider = 'KASPI';

DELETE FROM processed_webhook_events
    WHERE provider IN ('YOOKASSA', 'PAYPAL');
