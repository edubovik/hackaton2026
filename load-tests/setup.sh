#!/usr/bin/env bash
# setup.sh — create 300 load-test users and the shared loadtest-room.
# Writes:
#   load-tests/users.json    — credentials for k6 VUs
#   load-tests/room_id.txt   — room ID used in the scenario
#
# Usage:
#   bash load-tests/setup.sh
#   BASE_URL=http://my-host:8080 bash load-tests/setup.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASSWORD="LoadTest@123456!"
COUNT=300
BATCH=20                   # concurrent curl calls per batch
ROOM_NAME="loadtest-room"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
USERS_FILE="${SCRIPT_DIR}/users.json"
ROOM_FILE="${SCRIPT_DIR}/room_id.txt"

# ── Prerequisites ─────────────────────────────────────────────────────────────
for cmd in curl jq; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "[setup] ERROR: '$cmd' is required but not installed." >&2
    exit 1
  fi
done

# ── Wait for backend to be reachable ─────────────────────────────────────────
echo "[setup] Waiting for backend at ${BASE_URL} ..."
for i in $(seq 1 30); do
  if curl -sf "${BASE_URL}/api/v1/health" -o /dev/null 2>&1; then
    echo "[setup] Backend is up."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "[setup] ERROR: backend did not respond after 30 attempts." >&2
    exit 1
  fi
  sleep 2
done

# ── Generate users.json (deterministic — no API calls needed) ─────────────────
echo "[setup] Generating ${USERS_FILE} ..."
{
  echo "["
  for i in $(seq 1 $COUNT); do
    username=$(printf "loadtest_user_%03d" "$i")
    email="${username}@lt.test"
    comma=","
    [ "$i" -eq "$COUNT" ] && comma=""
    printf '  {"username":"%s","email":"%s","password":"%s"}%s\n' \
      "$username" "$email" "$PASSWORD" "$comma"
  done
  echo "]"
} > "$USERS_FILE"
echo "[setup] users.json written."

# ── Create users in parallel batches ─────────────────────────────────────────
echo "[setup] Creating $COUNT users (batch size $BATCH) ..."
success=0
fail=0
tmp_dir=$(mktemp -d)

create_user() {
  local i="$1" base_url="$2" password="$3" tmp="$4"
  local username email status
  username=$(printf "loadtest_user_%03d" "$i")
  email="${username}@lt.test"
  status=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "${base_url}/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${username}\",\"email\":\"${email}\",\"password\":\"${password}\"}" \
    --max-time 15 2>/dev/null || echo "000")
  echo "$status" > "${tmp}/${i}"
}
export -f create_user

for i in $(seq 1 $COUNT); do
  create_user "$i" "$BASE_URL" "$PASSWORD" "$tmp_dir" &
  # Throttle: wait for the batch to finish before starting the next
  if (( i % BATCH == 0 )); then
    wait
  fi
done
wait  # remaining jobs

# Tally results
for i in $(seq 1 $COUNT); do
  status=$(cat "${tmp_dir}/${i}" 2>/dev/null || echo "000")
  if [[ "$status" == "201" || "$status" == "409" ]]; then
    ((success++)) || true
  else
    ((fail++)) || true
    username=$(printf "loadtest_user_%03d" "$i")
    echo "[setup]   WARN: user $username → HTTP $status"
  fi
done
rm -rf "$tmp_dir"

echo "[setup] Users: ${success} OK, ${fail} failed."
if [ "$fail" -gt 0 ]; then
  echo "[setup] WARNING: some users failed to register — check the backend logs." >&2
fi

# ── Login as user_001 to get a token ─────────────────────────────────────────
echo "[setup] Logging in as loadtest_user_001 ..."
login_resp=$(curl -s \
  -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"loadtest_user_001@lt.test\",\"password\":\"${PASSWORD}\"}" \
  --max-time 15)

token=$(echo "$login_resp" | jq -r '.accessToken // empty')
if [ -z "$token" ]; then
  echo "[setup] ERROR: failed to login as loadtest_user_001." >&2
  echo "[setup] Response: $login_resp" >&2
  exit 1
fi
echo "[setup] Login OK."

AUTH_HEADER="Authorization: Bearer ${token}"

# ── Create (or find) the loadtest-room ───────────────────────────────────────
echo "[setup] Creating room '${ROOM_NAME}' ..."
create_resp=$(curl -s \
  -X POST "${BASE_URL}/api/v1/rooms" \
  -H "Content-Type: application/json" \
  -H "${AUTH_HEADER}" \
  -d "{\"name\":\"${ROOM_NAME}\",\"description\":\"Load test shared room\",\"isPublic\":true}" \
  --max-time 15)

http_status=$(echo "$create_resp" | jq -r '.id // empty')

if [ -n "$http_status" ]; then
  # Room created fresh
  room_id=$(echo "$create_resp" | jq -r '.id')
  echo "[setup] Room created (id=${room_id})."
else
  # Room may already exist — search for it
  echo "[setup] Room may already exist, searching ..."
  search_resp=$(curl -s \
    "${BASE_URL}/api/v1/rooms?search=$(python3 -c "import urllib.parse; print(urllib.parse.quote('${ROOM_NAME}'))" 2>/dev/null || echo "${ROOM_NAME}")&size=20" \
    -H "${AUTH_HEADER}" \
    --max-time 15)
  room_id=$(echo "$search_resp" | jq -r '.content[] | select(.name == "'"${ROOM_NAME}"'") | .id' | head -1)
  if [ -z "$room_id" ]; then
    echo "[setup] ERROR: could not create or find room '${ROOM_NAME}'." >&2
    echo "[setup] Create response: $create_resp" >&2
    exit 1
  fi
  echo "[setup] Found existing room (id=${room_id})."
fi

echo "$room_id" > "$ROOM_FILE"
echo "[setup] room_id.txt written (id=${room_id})."

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "┌─────────────────────────────────────────────────┐"
echo "│  Setup complete                                  │"
echo "│  Users created : ${success}                            │"
echo "│  Room ID       : ${room_id}                              │"
echo "│  Password      : ${PASSWORD}            │"
echo "└─────────────────────────────────────────────────┘"
echo ""
echo "[setup] You can now run the load test. See load-tests/README.md."
