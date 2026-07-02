CREATE TABLE shop_reviews (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    avatar_url  TEXT,
    city        VARCHAR(100),
    rating      SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    body        TEXT         NOT NULL,
    photo_urls  JSONB        NOT NULL DEFAULT '[]',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_shop_reviews_status     ON shop_reviews(status);
CREATE INDEX idx_shop_reviews_created_at ON shop_reviews(created_at DESC);
