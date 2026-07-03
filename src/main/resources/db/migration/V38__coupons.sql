CREATE TABLE coupons (
    id               BIGSERIAL    PRIMARY KEY,
    code             VARCHAR(50)  NOT NULL UNIQUE,
    discount_type    VARCHAR(20)  NOT NULL CHECK (discount_type IN ('PERCENTAGE','FIXED')),
    discount_value   DECIMAL(10,2) NOT NULL CHECK (discount_value > 0),
    min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    max_uses         INTEGER,
    used_count       INTEGER      NOT NULL DEFAULT 0,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at       TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
);

ALTER TABLE orders
    ADD COLUMN coupon_code     VARCHAR(50),
    ADD COLUMN discount_amount DECIMAL(10,2) DEFAULT 0;
