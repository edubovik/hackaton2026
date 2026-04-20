CREATE TYPE friend_request_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED');

CREATE TABLE friend_requests (
    id           BIGSERIAL PRIMARY KEY,
    from_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message      TEXT,
    status       friend_request_status NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(from_user_id, to_user_id)
);

CREATE TABLE friendships (
    user_id_a   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_id_b   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id_a, user_id_b),
    CHECK (user_id_a < user_id_b)
);

CREATE TABLE user_bans (
    banner_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (banner_id, banned_id)
);

CREATE INDEX idx_friend_requests_to   ON friend_requests(to_user_id, status);
CREATE INDEX idx_friendships_user_a   ON friendships(user_id_a);
CREATE INDEX idx_friendships_user_b   ON friendships(user_id_b);
