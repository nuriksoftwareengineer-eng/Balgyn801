-- V45: Drop legacy parcel_trackings table; tracking number is now stored directly on orders.
-- Add tracking_number to orders so admins can record any carrier's tracking number.

DROP TABLE IF EXISTS parcel_trackings;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(100);
