# Load Tests — Chat Application

Full user simulation load test using [k6](https://k6.io/).
Validates REQUIREMENTS.md §3.1 (300 concurrent users) and §3.2 (performance targets).

## Prerequisites

| Tool | Purpose |
|------|---------|
| Docker | Runs the app AND k6 (no local k6 install needed) |
| `curl` | Used by setup/teardown scripts |
| `jq` | JSON parsing in setup/teardown scripts |

Install jq on macOS: `brew install jq`

## File structure

```
load-tests/
├── README.md
├── scenario.js          ← k6 test (smoke + full run)
├── setup.sh             ← creates 300 users + loadtest-room
├── teardown.sh          ← deletes all test users + room
├── helpers/
│   └── stomp.js         ← STOMP 1.2 frame helpers for k6
└── reports/             ← test output (gitignored)
```

Generated at runtime (gitignored):
- `users.json` — credentials for the 300 VUs
- `room_id.txt` — ID of the shared loadtest-room

---

## How to run

### Step 0 — start the application

```bash
docker compose up --build -d
# Wait until all services are healthy:
docker compose ps
```

### Step 1 — create test data

```bash
bash load-tests/setup.sh
```

This:
- Creates 300 users (`loadtest_user_001` … `loadtest_user_300`, password `LoadTest@123456!`)
- Creates a public room named `loadtest-room`
- Writes `users.json` and `room_id.txt`

### Step 2 — smoke test (10 VUs, ~2 minutes)

Run this first to verify the scenario works end-to-end before committing to 300 VUs.

```bash
docker run --rm --network host \
  -v "$(pwd)/load-tests:/scripts" \
  -e SMOKE=true \
  grafana/k6 run /scripts/scenario.js \
  --out json=/scripts/reports/report-smoke-$(date +%s).json
```

Expected results:
- All 10 VUs log in successfully
- WebSocket + STOMP connections established (ws_connect_time p95 < 5 s)
- Messages sent and received with no errors
- `login_success` rate = 100 %

**Do not proceed to the full test until the smoke test passes cleanly.**

### Step 3 — full load test (300 VUs, ~8 minutes)

```bash
docker run --rm --network host \
  -v "$(pwd)/load-tests:/scripts" \
  grafana/k6 run /scripts/scenario.js \
  --out json=/scripts/reports/report-full-$(date +%s).json
```

During the test, open a second terminal and monitor:

```bash
# Docker resource usage
docker stats

# RabbitMQ management UI
open http://localhost:15672   # guest / guest

# Backend logs
docker compose logs -f backend
```

### Step 4 — teardown

```bash
bash load-tests/teardown.sh
```

Deletes all 300 test accounts (which also removes the loadtest-room) and
removes `users.json` and `room_id.txt`.

---

## Load profile

| Phase     | Duration | Target VUs |
|-----------|----------|------------|
| Ramp up   | 2 min    | 0 → 300    |
| Sustained | 5 min    | 300        |
| Ramp down | 1 min    | 300 → 0    |
| **Total** | **8 min**|            |

---

## What each VU does

1. `POST /api/v1/auth/login` — obtain JWT
2. `POST /api/v1/rooms/{id}/join` — join loadtest-room
3. Open WebSocket to `ws://localhost:8080/ws?token=<jwt>`
4. Send STOMP `CONNECT`, wait for `CONNECTED`
5. Subscribe to `/topic/room.{id}`
6. Send a message every 10–15 s (random) to `/app/chat.room.{id}`
7. Send a presence heartbeat every 30 s to `/app/presence/heartbeat`
8. Measure delivery latency: each message embeds a send-timestamp; when received back from the topic the round-trip is recorded
9. Stay connected until the test ends

---

## Thresholds (fail test if violated)

| Metric | Threshold | Requirement |
|--------|-----------|-------------|
| `http_req_failed` | < 1 % | reliability |
| `login p95` | < 2 000 ms | usability |
| `ws_connect_time p95` | < 5 000 ms | connection stability |
| `message_delivery_latency p95` | **< 3 000 ms** | §3.2 |
| `login_success` | > 99 % | §3.1 |

---

## Custom metrics

| Metric | Type | Description |
|--------|------|-------------|
| `ws_connect_time` | Trend | ms from WS `open` event to STOMP `CONNECTED` frame |
| `message_delivery_latency` | Trend | ms from STOMP `SEND` to own message received back from topic |
| `messages_sent` | Counter | STOMP SEND frames fired by all VUs |
| `messages_received` | Counter | MESSAGE frames received by all VUs |
| `login_success` | Rate | fraction of logins that returned HTTP 200 + accessToken |

---

## Custom BASE_URL

If the app runs on a different host (e.g. a remote VM):

```bash
BASE_URL=http://192.168.1.100:8080 bash load-tests/setup.sh

docker run --rm \
  -v "$(pwd)/load-tests:/scripts" \
  -e BASE_URL=http://192.168.1.100:8080 \
  -e WS_URL=ws://192.168.1.100:8080/ws \
  grafana/k6 run /scripts/scenario.js \
  --out json=/scripts/reports/report-full-$(date +%s).json

BASE_URL=http://192.168.1.100:8080 bash load-tests/teardown.sh
```
