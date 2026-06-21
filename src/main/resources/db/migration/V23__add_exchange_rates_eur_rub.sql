-- Bootstrap KZT per 1 EUR and KZT per 1 RUB.
-- The hourly scheduler (NbkExchangeRateProvider) will update these automatically.
INSERT INTO exchange_rates (code, rate, source, frozen, updated_at) VALUES
    ('KZT_EUR', 530.0000, 'BOOTSTRAP', FALSE, CURRENT_TIMESTAMP),
    ('KZT_RUB', 5.3000,  'BOOTSTRAP', FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;
