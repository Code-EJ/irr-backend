-- Durable cleanup jobs become visible only after attachment metadata deletion commits.
-- Author: Enzo Ribas (https://github.com/oEnzoRibas)
CREATE TABLE attachment_file_deletion (
    id UUID PRIMARY KEY,
    storage_path TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(300)
);
CREATE INDEX ix_attachment_file_deletion_due ON attachment_file_deletion(next_attempt_at, created_at);
