CREATE TABLE IF NOT EXISTS user_addresses (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label           VARCHAR(100) NOT NULL,
    city            VARCHAR(150) NOT NULL,
    street          VARCHAR(250) NOT NULL,
    apartment       VARCHAR(50)  NOT NULL,
    postal_code     VARCHAR(20)  NOT NULL,
    recipient_name  VARCHAR(200) NOT NULL,
    recipient_phone VARCHAR(30)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON user_addresses(user_id);
