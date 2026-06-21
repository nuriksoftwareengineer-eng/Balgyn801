-- V22: Add PAYPAL as a valid payment provider.
-- The Java enum PaymentProvider.PAYPAL is added in this version.
-- No data changes needed — V21 cleaned any legacy PAYPAL rows.
-- This migration serves as a schema version marker for the new provider.

-- Extend column length if it is too narrow for future providers (safety guard).
-- Column `provider` in `payments` and `processed_webhook_events` is VARCHAR(50) per V1.
-- No DDL change required — PAYPAL (6 chars) fits within existing constraints.
SELECT 1;
