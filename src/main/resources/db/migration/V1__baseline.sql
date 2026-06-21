-- V1: baseline snapshot of the schema managed by ddl-auto=update.
-- This migration only runs on FRESH databases.
-- Existing databases are baselined by spring.flyway.baseline-on-migrate=true.

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id),
    role    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS customers (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255),
    phone               VARCHAR(255),
    telegram_user_name  VARCHAR(255),
    create_at           DATE
);

CREATE TABLE IF NOT EXISTS products (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255),
    description VARCHAR(255),
    price       NUMERIC(38, 2),
    image_url   VARCHAR(255),
    in_stock    BOOLEAN,
    category    VARCHAR(255),
    sizes       JSONB,
    colors      JSONB,
    created_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS custom_designs (
    id                  BIGSERIAL PRIMARY KEY,
    customer_id         BIGINT REFERENCES customers (id),
    description         TEXT,
    reference_image_url VARCHAR(255),
    status              VARCHAR(255),
    created_at          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT REFERENCES customers (id),
    total_price   NUMERIC(10, 2),
    delivery_fee  NUMERIC(10, 2),
    comment       TEXT,
    delivery_type VARCHAR(255),
    status        VARCHAR(255),
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT REFERENCES orders (id),
    product_id       BIGINT REFERENCES products (id),
    custom_design_id BIGINT REFERENCES custom_designs (id),
    quantity         INTEGER,
    unit_price       NUMERIC(10, 2),
    size_label       VARCHAR(255),
    color_name       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS payments (
    id                   BIGSERIAL PRIMARY KEY,
    order_id             BIGINT NOT NULL REFERENCES orders (id),
    provider             VARCHAR(30) NOT NULL,
    status               VARCHAR(30) NOT NULL,
    amount               NUMERIC(10, 2) NOT NULL,
    currency             VARCHAR(10) NOT NULL,
    provider_payment_id  VARCHAR(255),
    payment_url          VARCHAR(512),
    webhook_event_id     VARCHAR(128),
    last_webhook_payload TEXT,
    error_message        TEXT,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP
);

CREATE TABLE IF NOT EXISTS delivery_addresses (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT REFERENCES orders (id),
    city             VARCHAR(255),
    street           VARCHAR(255),
    apartment        VARCHAR(255),
    postal_code      VARCHAR(255),
    recipient_name   VARCHAR(255),
    recipient_phone  VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS cdek_shipments (
    id                       BIGSERIAL PRIMARY KEY,
    order_id                 BIGINT REFERENCES orders (id),
    cdek_order_uuid          VARCHAR(255),
    tracking_number          VARCHAR(255),
    tariff_code              INTEGER,
    from_city                VARCHAR(255),
    to_city                  VARCHAR(255),
    estimated_delivery_date  DATE,
    status                   VARCHAR(255),
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP
);

CREATE SEQUENCE IF NOT EXISTS order_status_hist_id_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS order_history (
    id         BIGINT NOT NULL DEFAULT nextval('order_status_hist_id_seq') PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders (id),
    status     VARCHAR(255),
    date_added TIMESTAMP NOT NULL
);
