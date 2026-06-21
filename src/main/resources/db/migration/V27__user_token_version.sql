-- V27: Add token_version to users for refresh token revocation.
-- Incrementing token_version on logout invalidates all existing refresh tokens for that user.
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
