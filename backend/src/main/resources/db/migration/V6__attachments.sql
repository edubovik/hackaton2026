CREATE TABLE attachments (
    id            BIGSERIAL PRIMARY KEY,
    message_id    BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    uploader_id   BIGINT NOT NULL REFERENCES users(id),
    filename      VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(255) NOT NULL UNIQUE,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT NOT NULL,
    comment       TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_message ON attachments(message_id);
