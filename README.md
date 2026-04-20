# ChatApp

A classic web-based chat server supporting real-time messaging, public and private rooms, contacts, file sharing, and presence — built for up to 300 simultaneous users.

## Implemented phases

| Phase | Feature | Status |
|---|---|---|
| 0 | Project skeleton (Docker, DB, health check) | ✅ |
| 1 | User registration & authentication (JWT) | ✅ |
| 2 | Presence (online / AFK / offline via WebSocket) | ✅ |
| 3 | Contacts / Friends (requests, friends list, bans) | ✅ |

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
