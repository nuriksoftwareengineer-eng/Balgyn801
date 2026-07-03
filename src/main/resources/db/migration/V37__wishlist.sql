CREATE TABLE wishlist_items (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    design_id BIGINT NOT NULL REFERENCES designs(id) ON DELETE CASCADE,
    added_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, design_id)
);
CREATE INDEX idx_wishlist_user ON wishlist_items(user_id);
