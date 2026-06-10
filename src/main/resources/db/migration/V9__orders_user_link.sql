-- Link orders to authenticated users for purchase verification (reviews, history).
-- Nullable: orders placed anonymously (existing behaviour) keep user_id = NULL.
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS user_id BIGINT NULL
        REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
