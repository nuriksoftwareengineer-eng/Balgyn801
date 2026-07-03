-- V41: Server-side refresh-token store for rotation + reuse detection.
--
-- Each issued refresh token gets a row keyed by its JWT id (jti). On refresh, the used token is
-- revoked and a fresh one issued (rotation); replaying an already-revoked token triggers a
-- full-session revoke (reuse detection). Pre-existing refresh tokens (no jti record) become invalid
-- on deploy — users simply re-login once.
CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    jti        VARCHAR(64) NOT NULL UNIQUE,
    user_id    BIGINT      NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
