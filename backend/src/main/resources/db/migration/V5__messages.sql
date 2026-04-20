CREATE TABLE messages (
    id            BIGSERIAL PRIMARY KEY,
    room_id       BIGINT REFERENCES rooms(id) ON DELETE CASCADE,
    sender_id     BIGINT NOT NULL REFERENCES users(id),
    recipient_id  BIGINT REFERENCES users(id),
    reply_to_id   BIGINT REFERENCES messages(id),
    content       TEXT NOT NULL CHECK (octet_length(content) <= 3072),
    edited        BOOLEAN NOT NULL DEFAULT FALSE,
    deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_message_target CHECK (
        (room_id IS NOT NULL AND recipient_id IS NULL) OR
        (room_id IS NULL AND recipient_id IS NOT NULL)
    )
);

CREATE TABLE unread_counts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id     BIGINT REFERENCES rooms(id) ON DELETE CASCADE,
    partner_id  BIGINT REFERENCES users(id) ON DELETE CASCADE,
    count       INT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uc_unread_room ON unread_counts(user_id, room_id)    WHERE room_id   IS NOT NULL;
CREATE UNIQUE INDEX uc_unread_dm   ON unread_counts(user_id, partner_id) WHERE partner_id IS NOT NULL;

CREATE INDEX idx_messages_room    ON messages(room_id, created_at DESC);
CREATE INDEX idx_messages_dm      ON messages(sender_id, recipient_id, created_at DESC);
CREATE INDEX idx_messages_dm_rev  ON messages(recipient_id, sender_id, created_at DESC);
