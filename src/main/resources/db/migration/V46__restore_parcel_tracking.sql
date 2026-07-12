CREATE TABLE IF NOT EXISTS parcel_trackings (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    carrier          VARCHAR(50)  NOT NULL,
    tracking_number  VARCHAR(100) NOT NULL,
    last_status      VARCHAR(50),
    status_detail    TEXT,
    events           JSONB        NOT NULL DEFAULT '[]',
    provider         VARCHAR(50)  NOT NULL DEFAULT '17TRACK',
    error_message    TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_parcel_trackings_order_carrier
    ON parcel_trackings(order_id, carrier, tracking_number);

CREATE INDEX IF NOT EXISTS idx_parcel_trackings_order_id
    ON parcel_trackings(order_id);
