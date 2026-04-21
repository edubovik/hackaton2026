# frontend/CLAUDE.md — Frontend Conventions

Read this file at the start of every frontend session. These rules override general habits.

---

## Stack

- React 18.3.1, Vite 5.4.8
- Test runner: Vitest 2.1.1 + React Testing Library 16
- WebSocket: `@stomp/stompjs` 7.0.0

## Component Rules

- Functional components with hooks only — no class components
- One component per file, one responsibility per component
- Component files: `PascalCase.jsx` (e.g. `UserCard.jsx`)
- Utility files: `camelCase.js` (e.g. `formatDate.js`)

## Directory Structure

```
src/
  api/           ← all HTTP calls and WebSocket logic
    http.js      ← fetch wrapper / axios instance
    auth.js      ← auth API calls
    rooms.js     ← room API calls
    messages.js  ← message API calls
    socket.js    ← ALL WebSocket/STOMP logic lives here
  components/    ← reusable UI components
  pages/         ← top-level page components (one per route)
  hooks/         ← custom React hooks
  setupTests.js  ← @testing-library/jest-dom import
```

## API & Async

- All HTTP calls go in `src/api/` — never inside components
- Use `async/await` for all async logic
- Backend base URL: `/api/v1` (proxied by Nginx in Docker, direct in dev)

## WebSocket / STOMP

- ALL WebSocket logic in `src/api/socket.js` — never in components
- Use `@stomp/stompjs` Client
- Backend WebSocket endpoint: `ws://localhost:8080/ws` (dev) / `/ws` (via Nginx proxy in Docker)
- Topics to subscribe: `/topic/<feature>` for broadcasts
- Personal queue: `/queue/user.<userId>` for private messages

## Authentication

- JWT stored in `httpOnly` cookies — frontend never reads the token directly
- Auth state managed via React Context
- On 401 response → attempt token refresh → retry → redirect to login

## Testing (unit)

- Vitest + React Testing Library for all component tests
- Test files colocated: `ComponentName.test.jsx` next to `ComponentName.jsx`
- Use `screen` queries, not `container` queries
- Prefer `getByRole` over `getByText` where possible

## E2E Testing (Playwright)

- Config: `playwright.config.ts` — Chromium only, baseURL `http://localhost:3000`, 1 retry
- Specs in `e2e/` — TypeScript, one file per feature area
- Shared helpers in `e2e/helpers.ts`: `uniqueUser`, `registerAndSignIn`, `createRoom`, `enterRoom`, `waitForComposer`
- `waitForComposer` checks textarea `[placeholder="Type a message…"]` (not Send button — disabled when empty)
- Room catalog has pagination: always search by name before clicking Join, then `await page.waitForURL('/')`
- CSS Modules produce hashed class names — use role/text/placeholder selectors, never `.className`
- Strict mode: when a locator matches multiple elements, use `.first()` or scope with a parent locator
- DM unread badge only updates on page reload (by design); reload the page, then wait for friend in sidebar before asserting the badge
- After `sendRequest(page, username)`, the helper waits for the input to clear before returning — signals API completed

## Styling

- Plain CSS modules (`Component.module.css`) — no UI library
- Global styles in `src/App.css`
- No inline styles except for truly dynamic values

## Code Style

- No comments unless the WHY is non-obvious
- No prop-types (TypeScript is not used in this project)
- Use named exports for components, default export for pages
