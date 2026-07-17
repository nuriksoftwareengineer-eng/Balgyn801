-- Telegram Mini App auth: identity fields on users, additive only.
-- email/password_hash stay NOT NULL -- Telegram-only signups get a synthetic
-- namespaced email + unusable random password hash (see AuthServiceImpl), so every
-- existing query/constraint built around "email is the universal key" keeps working
-- unmodified.
ALTER TABLE users
    ADD COLUMN telegram_id BIGINT,
    ADD COLUMN telegram_username VARCHAR(64),
    ADD COLUMN telegram_first_name VARCHAR(128),
    ADD COLUMN telegram_last_name VARCHAR(128),
    ADD COLUMN telegram_photo_url VARCHAR(512),
    ADD COLUMN provider VARCHAR(16) NOT NULL DEFAULT 'LOCAL';

-- Partial unique index: NULL (not yet linked) never collides, but a real telegram_id
-- can only ever belong to one account.
CREATE UNIQUE INDEX uq_users_telegram_id ON users (telegram_id) WHERE telegram_id IS NOT NULL;
