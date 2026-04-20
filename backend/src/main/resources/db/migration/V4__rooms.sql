CREATE TABLE rooms (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_public   BOOLEAN NOT NULL DEFAULT TRUE,
    owner_id    BIGINT  NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_rooms_name_lower ON rooms (LOWER(name));

CREATE TABLE room_members (
    room_id     BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(10) NOT NULL DEFAULT 'MEMBER'
                    CHECK (role IN ('MEMBER', 'ADMIN', 'OWNER')),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (room_id, user_id)
);

CREATE TABLE room_bans (
    room_id     BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_by   BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (room_id, user_id)
);

CREATE TABLE room_invitations (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    inviter_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invitee_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(room_id, invitee_id)
);

CREATE INDEX idx_room_members_user ON room_members(user_id);
CREATE INDEX idx_room_bans_room    ON room_bans(room_id);
