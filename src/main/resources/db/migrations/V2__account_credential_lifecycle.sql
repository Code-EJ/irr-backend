-- Enzo Ribas (https://github.com/oEnzoRibas): additive upgrade for existing deployed accounts.
ALTER TABLE users
    ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT users_credential_version_nonnegative CHECK (credential_version >= 0);
