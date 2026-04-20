# PHASES.md — Self-Contained Phase Specifications

Each section below is a complete, standalone specification for one implementation phase.
To execute a phase in a fresh Claude session, provide:
1. This file (or the relevant phase section)
2. `REQUIREMENTS.md`
3. `backend/CLAUDE.md` and/or `frontend/CLAUDE.md`
4. Access to the cloned repository

**Rules that apply to every phase:**
- Write a detailed plan and wait for user approval before writing any code
- After implementation, write tests and run them — all must pass before declaring done
- Do not commit until the user explicitly approves
- Commit message format: `feat: phase N — short description`
- Never move to the next phase without user instruction

---

## Phase 1 — User Registration & Authentication

### Goal
Implement user registration, login, JWT-based authentication with refresh tokens, session management (view and revoke), password change, and account deletion.

### Prerequisites (already in repo)
- `docker-compose.yml` with postgres, rabbitmq, backend, frontend services
- Backend skeleton: `ChatAppApplication`, `HealthController`, `application.yml`, `application-docker.yml`
- Flyway wired but no migrations yet
- Spring Security NOT yet present in `pom.xml`

### New dependencies to add to `backend/pom.xml`
| Dependency | Version |
|---|---|
| `spring-boot-starter-security` | 3.3.4 |
| `io.jsonwebtoken:jjwt-api` | 0.12.6 |
| `io.jsonwebtoken:jjwt-impl` | 0.12.6 (runtime) |
| `io.jsonwebtoken:jjwt-jackson` | 0.12.6 (runtime) |
| `org.springframework.security:spring-security-test` | 6.3.3 (test) |

### Flyway migrations to create
**`V1__users.sql`**
```sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    user_agent  TEXT,
    ip_address  VARCHAR(64),
    expires_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens(token);
```

### Backend files to create
```
com.chatapp.auth/
  AuthController.java
  AuthService.java
  JwtService.java
  TokenCookieHelper.java
  dto/
    RegisterRequest.java
    LoginRequest.java
    SessionResponse.java
  entity/
    User.java
    RefreshToken.java
  repository/
    UserRepository.java
    RefreshTokenRepository.java
com.chatapp.common/
  GlobalExceptionHandler.java
  SecurityConfig.java
```

### REST API contracts

| Method | Path | Auth | Request body | Response |
|---|---|---|---|---|
| POST | `/api/v1/auth/register` | none | `{email, username, password}` | 201 Created |
| POST | `/api/v1/auth/login` | none | `{email, password, keepMeSignedIn}` | 200, sets `access_token` + `refresh_token` httpOnly cookies |
| POST | `/api/v1/auth/refresh` | refresh cookie | — | 200, new `access_token` cookie |
| POST | `/api/v1/auth/logout` | access cookie | — | 204, clears cookies, deletes refresh token |
| GET  | `/api/v1/auth/sessions` | access cookie | — | 200, list of `SessionResponse` |
| DELETE | `/api/v1/auth/sessions/{id}` | access cookie | — | 204 |
| POST | `/api/v1/users/me/password` | access cookie | `{currentPassword, newPassword}` | 204 |
| DELETE | `/api/v1/users/me` | access cookie | `{password}` | 204 |

### JWT / cookie rules
- Access token: 15-minute TTL, signed with HS256
- Refresh token: 30-day TTL when `keepMeSignedIn=true`, session-scoped (1-day TTL) otherwise
- Both stored as `httpOnly; Secure; SameSite=Strict` cookies
- JWT secret from env var `JWT_SECRET` (add to `.env.example`)
- Add `JWT_SECRET=changeme_use_a_long_random_string` to `.env.example` and `.env`

### Account deletion rules (REQUIREMENTS §2.1.4)
- Delete the user record
- Rooms owned by this user are deleted (cascades messages, files)
- Membership in other rooms is removed

### Frontend files to create
```
src/pages/
  SignInPage.jsx + SignInPage.module.css
  RegisterPage.jsx + RegisterPage.module.css
src/api/
  auth.js          ← register(), login(), logout(), refresh(), getSessions(), deleteSession()
src/hooks/
  useAuth.js       ← AuthContext + useAuth hook
src/App.jsx        ← update: add basic routing (sign-in / register / app placeholder)
```

### Frontend routing
Use React Router DOM 6. Add `react-router-dom@6.26.2` to `package.json`.
- `/signin` → `SignInPage`
- `/register` → `RegisterPage`
- `/` → protected placeholder ("You are logged in as {username}")

### Tests to write

**Backend (unit)**
- `AuthServiceTest` — register with duplicate email throws; register with duplicate username throws; login with wrong password throws; refresh token rotation works
- `JwtServiceTest` — token generation, parsing, expiry

**Backend (integration)**
- `AuthControllerIntegrationTest` — full register/login/refresh/logout flow using Testcontainers; session list and delete

**Frontend (RTL)**
- `SignInPage.test.jsx` — renders form fields; shows error on empty submit
- `RegisterPage.test.jsx` — renders form fields; shows error on password mismatch

### Definition of done
- User can register, log in, refresh token, log out
- Duplicate email/username returns 400 with clear error message
- All sessions visible and individually revocable
- Password change and account deletion work
- All tests pass

---

## Phase 2 — User Presence & WebSocket Infrastructure

### Goal
Wire up the WebSocket/STOMP endpoint backed by RabbitMQ as the broker relay. Implement presence states (online / AFK / offline) with multi-tab support and sub-2-second propagation.

### Prerequisites (already in repo)
- Phase 1 complete: users table, JWT auth, Spring Security
- `spring-boot-starter-websocket` and `spring-boot-starter-amqp` already in `pom.xml`

### New dependencies to add to `backend/pom.xml`
| Dependency | Version |
|---|---|
| `org.springframework.boot:spring-boot-starter-reactor-netty` | 3.3.4 |
| `io.projectreactor.netty:reactor-netty-core` | 1.1.21 (needed by STOMP relay) |

### Flyway migrations to create
**`V2__presence.sql`**
```sql
CREATE TYPE presence_state AS ENUM ('ONLINE', 'AFK', 'OFFLINE');

CREATE TABLE user_presence (
    user_id     BIGINT        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    state       presence_state NOT NULL DEFAULT 'OFFLINE',
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
```

### Backend files to create
```
com.chatapp.websocket/
  WebSocketConfig.java        ← StompBrokerRelayMessageHandler config, endpoint /ws
  WebSocketAuthInterceptor.java ← validates JWT from cookie on CONNECT frame
com.chatapp.presence/
  PresenceController.java     ← STOMP @MessageMapping for heartbeats
  PresenceService.java        ← state machine: ONLINE/AFK/OFFLINE
  PresenceScheduler.java      ← @Scheduled: sweep stale connections → OFFLINE
  dto/
    PresenceUpdate.java
  entity/
    UserPresence.java
  repository/
    UserPresenceRepository.java
```

### WebSocket / STOMP config
- Endpoint: `/ws` (SockJS disabled — use native WebSocket)
- STOMP broker relay: RabbitMQ at `${RABBITMQ_HOST}:${RABBITMQ_PORT}`
- Application destination prefix: `/app`
- Broker destination prefixes: `/topic`, `/queue`
- RabbitMQ STOMP plugin must be enabled — add to docker-compose rabbitmq command: `rabbitmq-plugins enable rabbitmq_stomp`

### RabbitMQ queue / topic naming
- Presence broadcast: `/topic/presence` — payload: `{userId, username, state}`
- Per-user queue: `/queue/user.{userId}` — declared with `x-message-ttl: 86400000`

### Presence state machine
- Client connects → `ONLINE`
- Client sends heartbeat to `/app/presence/heartbeat` every 30s while active
- No heartbeat for 60s → `AFK` (backend detects via scheduler)
- WebSocket disconnect → `OFFLINE`
- Multi-tab: track connections per userId; user is ONLINE if any connection is active

### Frontend files to create
```
src/api/
  socket.js         ← STOMP client, connect(), disconnect(), subscribe(), publish()
src/hooks/
  usePresence.js    ← subscribe to /topic/presence, maintain presence map
src/components/
  PresenceIndicator.jsx  ← renders ●/◐/○ based on state
```

### AFK detection (frontend)
- Track `mousemove`, `keydown`, `click`, `touchstart` events
- If no activity for 55s, stop sending heartbeats
- Backend's 60s sweep marks the user AFK

### Tests to write

**Backend (unit)**
- `PresenceServiceTest` — state transitions; multi-connection tracking

**Backend (integration)**
- `PresenceIntegrationTest` — connect via STOMP, verify presence broadcast (use Testcontainers RabbitMQ)

**Frontend (RTL)**
- `PresenceIndicator.test.jsx` — renders correct icon for each state
- `usePresence.test.js` — mock STOMP; verify state map updates

### Definition of done
- WebSocket connects and authenticates via JWT cookie
- Presence updates broadcast within 2 seconds
- AFK triggers after 60s of inactivity across all tabs
- Offline triggers on disconnect
- All tests pass

---

## Phase 3 — Contacts / Friends

### Goal
Implement the friend system: send/accept/reject friend requests (by username or from room user list), remove friends, and user-to-user bans.

### Prerequisites (already in repo)
- Phase 1 complete: users, JWT auth
- Phase 2 complete: presence (used to show friend status)

### Flyway migrations to create
**`V3__contacts.sql`**
```sql
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
```

### Backend files to create
```
com.chatapp.contact/
  ContactController.java
  ContactService.java
  dto/
    FriendRequestDto.java
    FriendDto.java
  entity/
    FriendRequest.java
    Friendship.java
    UserBan.java
  repository/
    FriendRequestRepository.java
    FriendshipRepository.java
    UserBanRepository.java
```

### REST API contracts

| Method | Path | Request body | Response |
|---|---|---|---|
| POST | `/api/v1/contacts/requests` | `{toUsername, message?}` | 201 |
| GET | `/api/v1/contacts/requests/incoming` | — | 200, list of pending requests |
| POST | `/api/v1/contacts/requests/{id}/accept` | — | 200 |
| POST | `/api/v1/contacts/requests/{id}/reject` | — | 200 |
| GET | `/api/v1/contacts` | — | 200, friend list with presence |
| DELETE | `/api/v1/contacts/{userId}` | — | 204 |
| POST | `/api/v1/contacts/{userId}/ban` | — | 204 |
| DELETE | `/api/v1/contacts/{userId}/ban` | — | 204 |

### Business rules (REQUIREMENTS §2.3)
- Friend request requires confirmation from recipient
- Cannot send request to someone who has banned you
- Ban terminates friendship; existing personal message history frozen (read-only)
- Personal messaging allowed only between mutual friends where neither side banned the other

### Real-time notifications
- When a friend request is received → push to `/queue/user.{toUserId}`: `{type: "FRIEND_REQUEST", ...}`
- When accepted → push to `/queue/user.{fromUserId}`: `{type: "FRIEND_ACCEPTED", ...}`

### Frontend files to create
```
src/pages/
  ContactsPage.jsx
src/components/
  FriendRequestItem.jsx
  FriendListItem.jsx
  ContactsPanel.jsx
src/api/
  contacts.js
```

### Tests to write

**Backend (unit)**
- `ContactServiceTest` — ban prevents request; duplicate request rejected; ban removes friendship

**Backend (integration)**
- `ContactControllerIntegrationTest` — full request/accept flow; ban effects

**Frontend (RTL)**
- `ContactsPanel.test.jsx` — renders friend list; shows pending requests

### Definition of done
- Friend requests sent, accepted, rejected
- Friend list shows with presence indicators
- Banned user cannot send messages or requests
- All tests pass

---

## Phase 4 — Chat Rooms

### Goal
Implement chat room creation, public catalog with search, join/leave, private rooms with invitations, owner/admin roles, and full moderation (ban, remove, promote/demote).

### Prerequisites (already in repo)
- Phase 1: auth
- Phase 2: WebSocket
- Phase 3: contacts (user search reused)

### Flyway migrations to create
**`V4__rooms.sql`**
```sql
CREATE TABLE rooms (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_public   BOOLEAN NOT NULL DEFAULT TRUE,
    owner_id    BIGINT  NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
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
```

### Backend files to create
```
com.chatapp.room/
  RoomController.java
  RoomService.java
  RoomModerationService.java
  dto/
    CreateRoomRequest.java
    RoomSummaryDto.java
    RoomDetailDto.java
    MemberDto.java
    BannedUserDto.java
  entity/
    Room.java
    RoomMember.java
    RoomBan.java
    RoomInvitation.java
  repository/
    RoomRepository.java
    RoomMemberRepository.java
    RoomBanRepository.java
    RoomInvitationRepository.java
```

### REST API contracts

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/rooms` | Create room |
| GET | `/api/v1/rooms?search=&page=&size=` | Public catalog, paginated |
| GET | `/api/v1/rooms/{id}` | Room detail (members must be member) |
| PUT | `/api/v1/rooms/{id}` | Update name/description/visibility (owner only) |
| DELETE | `/api/v1/rooms/{id}` | Delete room (owner only) |
| POST | `/api/v1/rooms/{id}/join` | Join public room |
| DELETE | `/api/v1/rooms/{id}/leave` | Leave room (owner cannot) |
| GET | `/api/v1/rooms/{id}/members` | List members |
| POST | `/api/v1/rooms/{id}/members/{userId}/ban` | Ban member (admin) |
| DELETE | `/api/v1/rooms/{id}/members/{userId}/ban` | Unban (admin) |
| GET | `/api/v1/rooms/{id}/bans` | List bans (admin) |
| POST | `/api/v1/rooms/{id}/admins/{userId}` | Promote to admin (owner) |
| DELETE | `/api/v1/rooms/{id}/admins/{userId}` | Demote admin (owner) |
| POST | `/api/v1/rooms/{id}/invitations` | Invite user (any member, private rooms) |
| POST | `/api/v1/rooms/{id}/invitations/accept` | Accept invitation |

### Business rules (REQUIREMENTS §2.4)
- Owner is always ADMIN and cannot be demoted
- Owner cannot leave — only delete
- Banning = remove + block rejoin unless unbanned
- Private rooms not visible in catalog
- Room name uniqueness is case-insensitive

### Real-time events (push to room topic on change)
- `/topic/room.{id}.members` — member join/leave/ban events

### Frontend files to create
```
src/pages/
  RoomCatalogPage.jsx
src/components/
  RoomListItem.jsx
  RoomPanel.jsx           ← left sidebar room list
  MembersPanel.jsx        ← right sidebar members + presence
  ManageRoomModal.jsx     ← tabs: Members, Admins, Banned, Invitations, Settings
  CreateRoomModal.jsx
src/api/
  rooms.js
```

### Tests to write

**Backend (unit)**
- `RoomServiceTest` — owner cannot leave; ban prevents rejoin; non-owner cannot delete
- `RoomModerationServiceTest` — admin permission matrix

**Backend (integration)**
- `RoomControllerIntegrationTest` — create, join, leave, ban, delete lifecycle

**Frontend (RTL)**
- `ManageRoomModal.test.jsx` — tab switching; renders member list
- `RoomCatalogPage.test.jsx` — search filters results

### Definition of done
- Public rooms listed and searchable
- Private rooms only accessible via invitation
- All owner/admin permission rules enforced
- Manage Room modal functional with all tabs
- All tests pass

---

## Phase 5 — Messaging

### Goal
Implement real-time messaging for both room chats and personal dialogs. Messages flow through RabbitMQ. Features: send, reply/quote, edit, delete, persistent history with infinite scroll, unread indicators.

### Prerequisites (already in repo)
- Phase 2: WebSocket/STOMP/RabbitMQ wired
- Phase 3: contacts (friendship gate for personal messages)
- Phase 4: rooms (room membership gate for room messages)

### Flyway migrations to create
**`V5__messages.sql`**
```sql
CREATE TABLE messages (
    id            BIGSERIAL PRIMARY KEY,
    room_id       BIGINT REFERENCES rooms(id) ON DELETE CASCADE,
    sender_id     BIGINT NOT NULL REFERENCES users(id),
    recipient_id  BIGINT REFERENCES users(id),   -- NULL for room messages
    reply_to_id   BIGINT REFERENCES messages(id),
    content       TEXT NOT NULL CHECK (octet_length(content) <= 3072),
    edited        BOOLEAN NOT NULL DEFAULT FALSE,
    deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (
        (room_id IS NOT NULL AND recipient_id IS NULL) OR
        (room_id IS NULL AND recipient_id IS NOT NULL)
    )
);

CREATE TABLE unread_counts (
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id     BIGINT REFERENCES rooms(id) ON DELETE CASCADE,
    partner_id  BIGINT REFERENCES users(id) ON DELETE CASCADE,
    count       INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, COALESCE(room_id, 0), COALESCE(partner_id, 0))
);

CREATE INDEX idx_messages_room    ON messages(room_id, created_at DESC);
CREATE INDEX idx_messages_dm      ON messages(sender_id, recipient_id, created_at DESC);
CREATE INDEX idx_messages_dm_rev  ON messages(recipient_id, sender_id, created_at DESC);
```

### Backend files to create
```
com.chatapp.message/
  MessageController.java     ← REST: history, edit, delete
  MessageListener.java       ← RabbitMQ consumer: persist + push
  MessagePublisher.java      ← publishes to RabbitMQ exchange
  MessageService.java
  dto/
    SendMessageRequest.java
    MessageDto.java
    MessagePage.java
  entity/
    Message.java
  repository/
    MessageRepository.java
    UnreadCountRepository.java
```

### Message flow
1. Client publishes to `/app/chat.room.{roomId}` or `/app/chat.dm.{partnerId}` via STOMP
2. Backend `@MessageMapping` receives, validates membership/friendship, publishes to RabbitMQ exchange
3. `MessageListener` consumes, persists to DB, increments unread counts, then pushes to:
   - Room: `/topic/room.{roomId}` (all members)
   - DM: `/queue/user.{recipientId}` + `/queue/user.{senderId}`

### REST API contracts (history + moderation)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/rooms/{id}/messages?before=<cursor>&limit=50` | Paginated history (cursor = message ID) |
| GET | `/api/v1/messages/dm/{partnerId}?before=<cursor>&limit=50` | DM history |
| PATCH | `/api/v1/messages/{id}` | Edit own message |
| DELETE | `/api/v1/messages/{id}` | Delete own message or admin delete in room |
| POST | `/api/v1/rooms/{id}/messages/read` | Mark room as read (clears unread) |
| POST | `/api/v1/messages/dm/{partnerId}/read` | Mark DM as read |
| GET | `/api/v1/messages/unread` | Unread counts for all rooms and DMs |

### Business rules
- Max message size: 3 KB (enforced in DB CHECK and backend validation)
- Only author can edit; author + room admins can delete
- Deleted messages stay in DB (`deleted=true`) but content shown as "This message was deleted"
- Personal messages: only between mutual friends where neither side banned the other
- Infinite scroll: cursor-based pagination (no offset), newest-first in UI

### Frontend files to create
```
src/pages/
  ChatPage.jsx              ← main layout: sidebar + message area + members
src/components/
  MessageList.jsx           ← infinite scroll, auto-scroll behavior
  MessageItem.jsx           ← renders text, reply quote, edited indicator, attachments
  MessageComposer.jsx       ← multiline input, emoji picker, reply bar, send button
  ReplyBar.jsx              ← "Replying to: {name} ×"
  UnreadBadge.jsx
src/hooks/
  useMessages.js            ← load history, receive real-time messages
  useUnread.js              ← unread counts
src/api/
  messages.js
```

### Tests to write

**Backend (unit)**
- `MessageServiceTest` — membership gate; friendship gate for DMs; edit permission; delete permission

**Backend (integration)**
- `MessageControllerIntegrationTest` — send, retrieve history, edit, delete
- `MessageFlowIntegrationTest` — full STOMP send → RabbitMQ → persist → push (uses Testcontainers for both Postgres and RabbitMQ)

**Frontend (RTL)**
- `MessageList.test.jsx` — renders messages; shows "edited" indicator
- `MessageComposer.test.jsx` — submit on Enter; shows reply bar when replying

### Definition of done
- Messages deliver in real time (under 3 seconds)
- History loads via infinite scroll
- Reply/quote, edit, delete all work
- Unread badges appear and clear on open
- Offline users receive messages on reconnect (from DB history)
- All tests pass

---

## Phase 6 — File Attachments

### Goal
Implement file and image upload (button + clipboard paste), access-controlled download, and attachment display in messages. Files stored on local filesystem (Docker volume).

### Prerequisites (already in repo)
- Phase 5: messages (attachments are linked to messages)
- `uploads` Docker volume defined in `docker-compose.yml`

### Flyway migrations to create
**`V6__attachments.sql`**
```sql
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
```

### Backend files to create
```
com.chatapp.attachment/
  AttachmentController.java
  AttachmentService.java
  AttachmentAccessGuard.java   ← checks room membership / DM friendship before download
  dto/
    AttachmentDto.java
    UploadResponse.java
  entity/
    Attachment.java
  repository/
    AttachmentRepository.java
```

### File storage rules (REQUIREMENTS §3.4)
- Stored at `/app/uploads/{yyyy-MM}/{randomUUID}.{ext}`
- Max file size: 20 MB; max image size: 3 MB (enforce in controller + Spring `max-file-size`)
- Original filename preserved in DB; served with `Content-Disposition: attachment; filename="{original}"`

### REST API contracts

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/attachments` | Multipart upload; returns `AttachmentDto` |
| GET | `/api/v1/attachments/{id}` | Download; access-controlled |

### Access control rules (REQUIREMENTS §2.6.4)
- Room attachment: caller must be current room member
- DM attachment: caller must be a participant (friendship check NOT required — if chat exists, access stands)
- If user loses room access → immediately loses download access
- If uploader loses room access → file stays stored, uploader loses access too

### Add to `application.yml`
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB
app:
  uploads:
    path: /app/uploads
```

### Frontend files to create / modify
```
src/components/
  AttachmentUploader.jsx     ← file input button + clipboard paste listener
  AttachmentPreview.jsx      ← image thumbnail or file icon + filename + size
src/api/
  attachments.js             ← upload(), download URL helper
```

### Modify `MessageComposer.jsx`
- Add attachment button (opens file picker)
- Add clipboard paste handler (`paste` event on window)
- Show pending attachment preview before send
- Allow optional comment on attachment

### Tests to write

**Backend (unit)**
- `AttachmentServiceTest` — file size limits enforced; stored name uniqueness

**Backend (integration)**
- `AttachmentControllerIntegrationTest` — upload, download, access denied after ban

**Frontend (RTL)**
- `AttachmentUploader.test.jsx` — renders button; paste event triggers upload

### Definition of done
- Files and images upload via button and paste
- Original filename preserved
- Non-members cannot download
- Access revoked immediately on room ban
- Max size limits enforced
- All tests pass

---

## Phase 7 — Password Reset & Account Management

### Goal
Implement password reset via email link (Mailhog in dev), account self-deletion with ownership rules, and the Sessions management screen.

### Prerequisites (already in repo)
- Phase 1: users, auth, refresh tokens, basic sessions endpoint

### New services to add to `docker-compose.yml`
```yaml
  mailhog:
    image: mailhog/mailhog:v1.0.1
    ports:
      - "1025:1025"   # SMTP
      - "8025:8025"   # Web UI
```

### New dependencies to add to `backend/pom.xml`
| Dependency | Version |
|---|---|
| `org.springframework.boot:spring-boot-starter-mail` | 3.3.4 |

### Add to `.env.example` and `.env`
```
MAIL_HOST=mailhog
MAIL_PORT=1025
MAIL_FROM=noreply@chatapp.local
```

### Add to `application-docker.yml`
```yaml
spring:
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false
app:
  mail:
    from: ${MAIL_FROM}
  frontend:
    url: http://localhost:3000
```

### Flyway migrations to create
**`V7__password_reset.sql`**
```sql
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);
```

### Backend files to create / modify
```
com.chatapp.auth/
  PasswordResetController.java
  PasswordResetService.java
  EmailService.java
  dto/
    ForgotPasswordRequest.java
    ResetPasswordRequest.java
  entity/
    PasswordResetToken.java
  repository/
    PasswordResetTokenRepository.java
```

### REST API contracts

| Method | Path | Auth | Request body | Notes |
|---|---|---|---|---|
| POST | `/api/v1/auth/forgot-password` | none | `{email}` | Always 200 (prevent email enumeration) |
| POST | `/api/v1/auth/reset-password` | none | `{token, newPassword}` | 204 on success; 400 if token invalid/expired |

### Password reset rules
- Token TTL: 1 hour
- Token single-use: mark `used=true` on consumption
- Email contains link: `{frontend.url}/reset-password?token={token}`
- If email not found → still return 200 (prevent enumeration)

### Account deletion (already partially in Phase 1 — verify complete)
Rules from REQUIREMENTS §2.1.4:
- Delete user record
- Rooms owned by user → deleted (cascades to messages, attachments)
- Membership in other rooms → removed
- Friendships → removed
- Personal message history → remains in DB (orphaned, visible to other participant)

### Frontend files to create
```
src/pages/
  ForgotPasswordPage.jsx
  ResetPasswordPage.jsx
  SessionsPage.jsx            ← lists active sessions, revoke button per session
src/components/
  ChangePasswordModal.jsx     ← for logged-in users
  DeleteAccountModal.jsx      ← confirm with password
```

### Update routing in `App.jsx`
- `/forgot-password` → `ForgotPasswordPage`
- `/reset-password` → `ResetPasswordPage`
- `/sessions` → `SessionsPage` (protected)

### Tests to write

**Backend (unit)**
- `PasswordResetServiceTest` — token expiry; used token rejected; unknown email returns no error

**Backend (integration)**
- `PasswordResetControllerIntegrationTest` — full forgot/reset flow; verify token consumed; verify email sent (mock `JavaMailSender`)

**Frontend (RTL)**
- `ForgotPasswordPage.test.jsx` — form submit; success message shown
- `ResetPasswordPage.test.jsx` — password mismatch error; success redirect
- `SessionsPage.test.jsx` — renders session list; revoke calls API

### Definition of done
- Password reset email sent and visible in Mailhog at `localhost:8025`
- Reset link works; token is single-use and expires after 1 hour
- Account deletion follows all ownership rules
- Sessions screen lists and revokes sessions
- All tests pass
