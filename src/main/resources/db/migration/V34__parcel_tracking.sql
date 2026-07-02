-- V34: Parcel tracking table.
-- Stores last-fetched status + raw event log per shipment.
-- Designed for multiple carrier support (carrier field is extensible).

CREATE TABLE parcel_trackings (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    carrier          VARCHAR(50)  NOT NULL,               -- e.g. 'KAZPOST', 'CDEK', '17TRACK'
    tracking_number  VARCHAR(100) NOT NULL,
    last_status      VARCHAR(50),                         -- 'IN_TRANSIT', 'DELIVERED', 'PENDING', etc.
    status_detail    TEXT,                                -- human-readable last event
    events           JSONB        NOT NULL DEFAULT '[]',  -- full event history
    provider         VARCHAR(50)  NOT NULL DEFAULT '17TRACK', -- API provider
    error_message    TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_parcel_trackings_order_carrier
    ON parcel_trackings(order_id, carrier, tracking_number);

CREATE INDEX idx_parcel_trackings_order_id ON parcel_trackings(order_id);
