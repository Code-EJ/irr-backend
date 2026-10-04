-- Additive recovery authorization; plaintext tokens are never persisted.
-- Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
CREATE TABLE password_recovery_token (
    token_hash char(64) PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    issued_email varchar(255) NOT NULL,
    credential_version bigint NOT NULL CHECK (credential_version >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    CONSTRAINT recovery_expiry CHECK (expires_at > created_at)
);
CREATE INDEX recovery_expiry_idx ON password_recovery_token(expires_at);
CREATE INDEX recovery_user_idx ON password_recovery_token(user_id);
