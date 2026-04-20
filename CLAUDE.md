# CLAUDE.md — Project Instructions

This file defines how Claude Code should behave throughout this project.
Read and follow these instructions at the start of every session.

---

## Project Overview

- **Type:** Full-stack web application
- **Backend:** Java 21 with Spring Boot (Maven)
- **Frontend:** React (JavaScript)
- **Database:** PostgreSQL
- **Base Docker image for backend:** `eclipse-temurin:21`
- **Status:** Greenfield — being built from scratch

---

## Golden Rule: git clone → docker compose up → working app

The entire application (backend, frontend, database) must start with a single command:
```bash
git clone <repo>
cd <project>
docker compose up
```
No manual setup steps, no local installs required (beyond Docker). If something requires extra steps, those must be documented in README.md and automated where possible.

---

## Workflow Rules

### 1. Always plan before coding
- When given a task or requirements, produce a written plan first.
- The plan must include: what will be created, what will be changed, file structure, and any assumptions.
- **Do not write any code until the plan is explicitly approved.**
- If requirements are unclear, ask clarifying questions before planning.

### 2. Ask before making big decisions
Big decisions include:
- Choosing a library or dependency not already in the project
- Changing folder/package structure
- Introducing a new architectural pattern
- Deleting or significantly refactoring existing code
- Anything that affects more than one layer of the app

When in doubt — ask, don't assume.

### 3. Write tests after each feature
- After implementing any feature, write tests before moving on.
- Backend: JUnit 5 + Mockito for unit tests; Spring Boot Test for integration tests.
- Frontend: React Testing Library for component tests.
- Run the tests and report results. Fix failures before declaring a feature done.

### 4. Keep code simple over clever
- Prefer readable, straightforward code over complex or over-engineered solutions.
- Avoid premature optimization.
- Follow YAGNI (You Aren't Gonna Need It) — don't build what isn't asked for.

## Development Environment

- **Docker only** — there is no local development setup outside of Docker.
- Developers only need Docker installed. No local Java, Node, or PostgreSQL required.
- All services start with `docker compose up --build`.

--- Conventions (WebSocket + RabbitMQ)

### Architecture
- Clients connect to the backend via **WebSocket** (using Spring WebSocket + STOMP protocol).
- To avoid overloading the WebSocket layer, messages go through **RabbitMQ** first:
  - Client sends a message → backend publishes to a RabbitMQ queue
  - A consumer picks it up from the queue → processes it → pushes result back to clients via WebSocket
- This decouples message production from delivery and handles traffic spikes gracefully.

### Backend rules:
- Use `spring-boot-starter-websocket` and configure a STOMP message broker backed by RabbitMQ.
- RabbitMQ acts as the STOMP broker relay (not an in-memory broker) — use `StompBrokerRelayMessageHandler`.
- Define clear queue/topic naming conventions, e.g. `/topic/<feature>` for broadcasts, `/queue/<user>` for private messages.
- Never push directly to WebSocket from a REST controller — always go through the queue.

### RabbitMQ rules:
- Add `rabbitmq` as a service in `docker-compose.yml` (use the `rabbitmq:3-management` image so the management UI is available at port `15672`).
- Connection config goes in `.env` and `application-docker.yml`.
- Add to `.env.example`:
```
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
```

---

## Database Conventions (PostgreSQL)

- Use PostgreSQL as the only database — no in-memory alternatives (no H2).
- In tests, use **Testcontainers** to spin up a real PostgreSQL instance.
- Database credentials and config go in `.env` file (never hardcoded).
- `.env.example` must always be kept up to date for other developers.
- Schema managed via **Flyway** migrations in `backend/src/main/resources/db/migration/`.
- Migration files named: `V1__description.sql`, `V2__description.sql`, etc.

---

## Docker & Docker Compose Conventions

The app must work out of the box with `docker compose up`. This is non-negotiable.

### docker-compose.yml must include:
- `postgres` — database service with a named volume for persistence
- `rabbitmq` — message broker using `rabbitmq:3-management` image
- `backend` — Spring Boot app, built from `./backend/Dockerfile`
- `frontend` — React app served via Nginx, built from `./frontend/Dockerfile`

### Rules:
- Backend must wait for Postgres to be healthy before starting (`depends_on` with `healthcheck`).
- All environment variables come from `.env` (use `env_file` in compose).
- Ports exposed: frontend on `3000`, backend on `8080`, postgres on `5432`, rabbitmq on `5672` (management UI on `15672`).
- Backend must wait for both Postgres and RabbitMQ to be healthy before starting.
- Each service must have its own `Dockerfile` (multi-stage builds for backend and frontend).

### .env.example (always maintain this):
```
POSTGRES_DB=appdb
POSTGRES_USER=appuser
POSTGRES_PASSWORD=changeme
SPRING_PROFILES_ACTIVE=docker
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
```

---

## Backend Conventions (Spring Boot)

- **Package structure:** `com.<appname>.{feature}` (feature-based, not layer-based)
- Use `@RestController` for API endpoints, `@Service` for business logic, `@Repository` for data access.
- Return proper HTTP status codes (`ResponseEntity` where needed).
- All REST endpoints must be prefixed with `/api/v1/` (e.g. `/api/v1/users`, `/api/v1/messages`).
- Use DTOs to separate API contracts from internal domain models.
- Handle errors with a global `@ControllerAdvice` exception handler.
- Use `application.yml` for configuration (not `application.properties`).
- Authentication uses **JWT tokens** — specific implementation details are defined in `REQUIREMENTS.md`.
- Database migrations via **Flyway**.

## Frontend Conventions (React)

- Functional components with hooks only — no class components.
- Keep components small and focused on one responsibility.
- API calls go in a dedicated `/src/api/` folder, not inside components.
- Use `async/await` for async logic.
- Name component files in PascalCase (`UserCard.jsx`), utilities in camelCase.
- WebSocket connections use **STOMP** protocol via the `@stomp/stompjs` library.
- All WebSocket logic goes in `/src/api/socket.js` — never directly inside components.
- Connect to the backend WebSocket endpoint at `ws://localhost:8080/ws` (proxied via Nginx in production).

---

## Project Structure (target)

```
/
├── backend/                  # Spring Boot project
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/com/<appname>/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-docker.yml
│       │       └── db/migration/   # Flyway SQL files
│       └── test/java/com/<appname>/
├── frontend/                 # React app
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── api/
│       ├── components/
│       └── pages/
├── docker-compose.yml
├── .env.example
├── REQUIREMENTS.md
└── CLAUDE.md
```

---

## Git Workflow

- After each phase is fully implemented and all tests pass, stop and notify the user.
- Do not commit anything until the user explicitly approves.
- Once approved, commit with a clear message in this format:
  `feat: phase X — short description of what was built`
- Never commit broken code, failing tests, or work-in-progress changes.
- Never push to remote unless explicitly asked by the user.
- Each commit should represent one complete, working, tested phase.

---

## Session Checklist

At the start of each session, Claude should:
1. Read `REQUIREMENTS.md` if it exists.
2. Check what has already been built (existing files/folders).
3. Confirm the current task with the user before starting.

After completing any feature or significant change, Claude should:
1. Update `README.md` to reflect the current state of the project.
2. Ensure `README.md` always contains: project description, how to run (`docker compose up --build`), available ports, and how to run tests.
