CREATE TABLE rooms (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_public   BOOLEAN NOT NULL DEFAULT TRUE,
    owner_id    BIGINT  NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT rooms_name_unique UNIQUE (LOWER(name))
);

CREATE TYPE room_member_role AS ENUM ('MEMBER', 'ADMIN', 'OWNER');

CREATE TABLE room_members (
    room_id     BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        room_member_role NOT NULL DEFAULT 'MEMBER',
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
