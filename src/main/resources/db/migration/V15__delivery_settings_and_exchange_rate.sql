-- V15: Admin-editable delivery settings + cached exchange rate.
-- Both are DB-authoritative and read by checkout without any external API call, so orders place
-- fine even if the rate provider is unavailable.

CREATE TABLE delivery_settings (
    setting_key   VARCHAR(64)    PRIMARY KEY,
    numeric_value NUMERIC(12, 2) NOT NULL
);

-- Kazakhstan flat delivery fee (same for every domestic destination; no weight/distance/city).
INSERT INTO delivery_settings (setting_key, numeric_value) VALUES
    ('KZ_DELIVERY_FLAT_KZT', 1600.00);

CREATE TABLE exchange_rates (
    code       VARCHAR(16)    PRIMARY KEY,
    rate       NUMERIC(14, 4) NOT NULL CHECK (rate > 0),
    source     VARCHAR(16)    NOT NULL,
    frozen     BOOLEAN        NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP      NOT NULL
);

-- Bootstrap KZT per 1 USD; the scheduled updater refreshes it, admin can override/freeze.
INSERT INTO exchange_rates (code, rate, source, frozen, updated_at) VALUES
    ('KZT_USD', 480.0000, 'BOOTSTRAP', FALSE, CURRENT_TIMESTAMP);
