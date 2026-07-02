-- V31: Add VTB_KZ as a valid payment provider.
-- The Java enum PaymentProvider.VTB_KZ is added in this version.
--
-- Storage: both `payments.provider` and `processed_webhook_events.provider` are
-- VARCHAR(30) columns with no CHECK constraint on provider values (see V1 and V17).
-- No PostgreSQL enum type named "payment_provider" exists in this schema.
-- 'VTB_KZ' (6 chars) fits within the existing VARCHAR(30) without any DDL change.
--
-- No data changes needed — VTB_KZ has no legacy rows to migrate.
SELECT 1;
