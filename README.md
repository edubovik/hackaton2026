# ChatApp

A classic web-based chat server supporting real-time messaging, public and private rooms, contacts, file sharing, and presence — built for up to 300 simultaneous users.

## Implemented phases

| Phase | Feature | Status |
|---|---|---|
| 0 | Project skeleton (Docker, DB, health check) | ✅ |
| 1 | User registration & authentication (JWT) | ✅ |
| 2 | Presence (online / AFK / offline via WebSocket) | ✅ |
| 3 | Contacts / Friends (requests, friends list, bans) | ✅ |
| 4 | Chat Rooms (catalog, join/leave, roles, moderation, invitations) | ✅ |
| 5 | Messaging (real-time room & DM chat, history, edit, delete, unread badges) | ✅ |
| 6 | File Attachments (upload, download, image preview) | ✅ |
| 7 | Password Reset & Account Management (email reset link, sessions screen) | ✅ |

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Maven |
| Frontend | React 18, Vite, STOMP/WebSocket |
| Database | PostgreSQL 16 |
| Messaging | RabbitMQ 3 (STOMP broker relay) |
| Auth | JWT (httpOnly cookies) + refresh tokens |

## Running the app

**Prerequisites:** Docker and Docker Compose only. No local Java, Node, or PostgreSQL needed.

```bash
git clone <repo-url>
cd chatapp
cp .env.example .env   # edit values if needed
docker compose up --build
```

## Ports

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api/v1 |
| Health check | http://localhost:8080/api/v1/health |
| WebSocket | ws://localhost:8080/ws |
| PostgreSQL | localhost:5432 |
| RabbitMQ | localhost:5672 |
| RabbitMQ Management UI | http://localhost:15672 (guest/guest) |
| Mailhog (email UI) | http://localhost:8025 |

## Running tests

### Backend

```bash
cd backend
./mvnw test
```

Tests use Testcontainers — Docker must be running. No other setup needed.

> **Colima users:** Create `~/.testcontainers.properties` with:
> ```
> docker.host=unix:///Users/<you>/.colima/default/docker.sock
> ryuk.disabled=true
> ```

### Frontend

```bash
cd frontend
npm install
npm test
```

## Password Reset API (Phase 7)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/forgot-password` | none | Send reset email `{email}` — always returns 200 |
| POST | `/api/v1/auth/reset-password` | none | Reset password `{token, newPassword}` — 204 on success, 400 if invalid/expired |
| GET | `/api/v1/auth/sessions` | required | List active sessions |
| DELETE | `/api/v1/auth/sessions/{id}` | required | Revoke a session |
| POST | `/api/v1/users/me/password` | required | Change password `{currentPassword, newPassword}` |
| DELETE | `/api/v1/users/me` | required | Delete account `{password}` |

Password reset tokens expire after 1 hour and are single-use. Reset emails are delivered to **Mailhog** in development — view them at http://localhost:8025.

## Contacts API (Phase 3)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/contacts/requests` | Send friend request `{toUsername, message?}` |
| GET | `/api/v1/contacts/requests/incoming` | List pending incoming requests |
| POST | `/api/v1/contacts/requests/{id}/accept` | Accept a request |
| POST | `/api/v1/contacts/requests/{id}/reject` | Reject a request |
| GET | `/api/v1/contacts` | Friend list with presence |
| DELETE | `/api/v1/contacts/{userId}` | Remove friend |
| POST | `/api/v1/contacts/{userId}/ban` | Ban user (removes friendship) |
| DELETE | `/api/v1/contacts/{userId}/ban` | Unban user |

Real-time notifications are pushed to `/queue/user.{id}` on WebSocket:
- `FRIEND_REQUEST` — when someone sends you a request
- `FRIEND_ACCEPTED` — when your request is accepted

## Rooms API (Phase 4)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/rooms` | Create room `{name, description, isPublic}` |
| GET | `/api/v1/rooms?search=&page=&size=` | Public catalog (paginated) |
| GET | `/api/v1/rooms/{id}` | Room detail (members must be member for private) |
| PUT | `/api/v1/rooms/{id}` | Update room settings (owner only) |
| DELETE | `/api/v1/rooms/{id}` | Delete room (owner only) |
| POST | `/api/v1/rooms/{id}/join` | Join public room |
| DELETE | `/api/v1/rooms/{id}/leave` | Leave room (owner cannot) |
| GET | `/api/v1/rooms/{id}/members` | List members |
| POST | `/api/v1/rooms/{id}/members/{userId}/ban` | Ban member (admin+) |
| DELETE | `/api/v1/rooms/{id}/members/{userId}/ban` | Unban member (admin+) |
| GET | `/api/v1/rooms/{id}/bans` | List bans (admin+) |
| POST | `/api/v1/rooms/{id}/admins/{userId}` | Promote to admin (owner only) |
| DELETE | `/api/v1/rooms/{id}/admins/{userId}` | Demote admin (owner only) |
| POST | `/api/v1/rooms/{id}/invitations` | Invite user `{username}` |
| POST | `/api/v1/rooms/{id}/invitations/accept` | Accept invitation |

Real-time member events are broadcast to `/topic/room.{id}.members`:
- `JOIN` — user joined
- `LEAVE` — user left
- `BAN` — user was banned

## Messaging API (Phase 5)

### REST

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/rooms/{id}/messages?before=<cursor>&limit=50` | Room message history (cursor-based) |
| GET | `/api/v1/messages/dm/{partnerId}?before=<cursor>&limit=50` | DM history |
| PATCH | `/api/v1/messages/{id}` | Edit own message `{content}` |
| DELETE | `/api/v1/messages/{id}` | Delete own message (or admin delete in room) |
| POST | `/api/v1/rooms/{id}/messages/read` | Mark room as read |
| POST | `/api/v1/messages/dm/{partnerId}/read` | Mark DM as read |
| GET | `/api/v1/messages/unread` | Unread counts for all rooms and DMs |
| GET | `/api/v1/users/me` | Current user profile `{id, username, email}` |

### WebSocket (STOMP)

**Send a message:**
- Room: publish to `/app/chat.room.{roomId}` with `{content, replyToId?}`
- DM: publish to `/app/chat.dm.{partnerId}` with `{content, replyToId?}`

**Receive messages:**
- Room: subscribe to `/topic/room.{roomId}`
- DM / edits: subscribe to `/queue/user.{userId}`

### Business rules
- Max message size: 3 KB
- Only author can edit; author or room admins can delete
- Deleted messages show "This message was deleted"
- DMs allowed only between mutual friends where neither side has banned the other
- Infinite scroll loads history newest-first using cursor (message ID)

## Project structure

```
/
├── backend/          # Spring Boot application
├── frontend/         # React application
├── docker-compose.yml
├── .env.example
├── REQUIREMENTS.md   # Full feature specification
├── PHASES.md         # Phased implementation plan
└── CLAUDE.md         # AI assistant instructions
```
