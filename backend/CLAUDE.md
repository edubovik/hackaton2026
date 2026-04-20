# backend/CLAUDE.md — Backend Conventions

Read this file at the start of every backend session. These rules override general habits.

---

## Language & Framework

- Java 21, Spring Boot 3.3.4, Maven
- Base Docker image: `eclipse-temurin:21`

## Package Structure

Feature-based, not layer-based:

```
com.chatapp.
  health/        ← HealthController
  auth/          ← registration, login, JWT, sessions
  user/          ← user profile, account deletion, password change
  presence/      ← online/AFK/offline states
  contact/       ← friends, friend requests, user bans
  room/          ← chat rooms, members, admins, bans, invitations
  message/       ← messages, replies, edit, delete, history
  attachment/    ← file upload, download, access control
  common/        ← shared DTOs, exceptions, config
```

## REST API Rules

- All endpoints: `/api/v1/<resource>`
- Use `ResponseEntity` when status code matters
- Use DTOs for all request/response bodies — never expose JPA entities directly
- Global error handler: `@ControllerAdvice` in `com.chatapp.common`

## Authentication

- JWT stored in `httpOnly` cookies
- Access token TTL: 15 minutes
- Refresh token TTL: 30 days ("keep me signed in"), session-only otherwise
- Spring Security added in Phase 1 — not present in Phase 0

## WebSocket / STOMP / RabbitMQ

- WebSocket endpoint: `/ws`
- STOMP broker: RabbitMQ relay via `StompBrokerRelayMessageHandler`
- Topic naming: `/topic/<feature>` for broadcasts
- Personal queue naming: `/queue/user.<userId>` for private delivery
- Per-user queues declared with `x-message-ttl: 86400000` (1 day)
- Never push to WebSocket from a REST controller — always publish to RabbitMQ queue first

## Database

- PostgreSQL only — no H2, no in-memory
- Schema managed by Flyway: `src/main/resources/db/migration/`
- Migration naming: `V1__description.sql`, `V2__description.sql`, …
- `spring.jpa.hibernate.ddl-auto: validate` — Flyway owns the schema

## Testing

- JUnit 5 + Mockito for unit tests
- `@SpringBootTest` + Testcontainers (`postgres:16`) for integration tests
- `@DynamicPropertySource` to inject container DB URL
- Exclude RabbitMQ auto-config in tests that don't need it:
  ```java
  @SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
  })
  ```
- Never mock the database in integration tests — use Testcontainers

## Configuration

- `application.yml` — base config (local dev reference)
- `application-docker.yml` — Docker overrides, reads from env vars
- All secrets via environment variables — never hardcoded

## Code Style

- No comments unless the WHY is non-obvious
- No class-level Javadoc blocks
- No unused imports or fields
- Prefer `record` for DTOs where appropriate (Java 21)
