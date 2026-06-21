CREATE TABLE IF NOT EXISTS reviews (
    id         BIGSERIAL PRIMARY KEY,
    design_id  BIGINT    NOT NULL REFERENCES designs(id)  ON DELETE CASCADE,
    user_id    BIGINT    NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    rating     SMALLINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_review_design_user UNIQUE (design_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_reviews_design_id ON reviews(design_id);
