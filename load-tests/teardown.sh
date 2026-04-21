#!/usr/bin/env bash
# teardown.sh — delete all 300 load-test users and clean up generated files.
#
# The loadtest-room is deleted first so that all messages cascade-delete,
# removing the FK constraint that would otherwise block user deletion.
#
# Usage:
#   bash load-tests/teardown.sh
#   BASE_URL=http://my-host:8080 bash load-tests/teardown.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASSWORD="LoadTest@123456!"
COUNT=300
BATCH=20

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── Prerequisites ─────────────────────────────────────────────────────────────
for cmd in curl jq; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "[teardown] ERROR: '$cmd' is required but not installed." >&2
    exit 1
  fi
done

# ── Delete the loadtest-room first (cascades to all messages) ─────────────────
# messages.sender_id has no ON DELETE CASCADE, so users cannot be deleted while
# their messages exist. Deleting the room first removes all messages via cascade.
ROOM_FILE="${SCRIPT_DIR}/room_id.txt"
if [ -f "$ROOM_FILE" ]; then
  room_id=$(cat "$ROOM_FILE")
  echo "[teardown] Logging in as loadtest_user_001 to delete room ${room_id} ..."
  login_resp=$(curl -s \
    -X POST "${BASE_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"loadtest_user_001@lt.test","password":"LoadTest@123456!"}' \
    --max-time 15 2>/dev/null || echo "{}")
  owner_token=$(echo "$login_resp" | jq -r '.accessToken // empty' 2>/dev/null || true)
  if [ -n "$owner_token" ]; then
    del_room_status=$(curl -s -o /dev/null -w "%{http_code}" \
      -X DELETE "${BASE_URL}/api/v1/rooms/${room_id}" \
      -H "Authorization: Bearer ${owner_token}" \
      --max-time 15 2>/dev/null || echo "000")
    if [[ "$del_room_status" == "204" || "$del_room_status" == "200" ]]; then
      echo "[teardown] Room ${room_id} deleted (messages cascaded)."
    elif [[ "$del_room_status" == "404" ]]; then
      echo "[teardown] Room ${room_id} already gone."
    else
      echo "[teardown] WARN: room deletion returned HTTP ${del_room_status}." >&2
    fi
  else
    echo "[teardown] WARN: could not login as user_001 to delete room — messages may block user deletion." >&2
  fi
else
  echo "[teardown] room_id.txt not found — skipping room deletion."
fi

echo "[teardown] Deleting $COUNT load-test users (batch size $BATCH) ..."

success=0
skip=0
fail=0
tmp_dir=$(mktemp -d)

delete_user() {
  local i="$1" base_url="$2" password="$3" tmp="$4"
  local username email

  username=$(printf "loadtest_user_%03d" "$i")
  email="${username}@lt.test"

  # Login to get a fresh access token
  login_resp=$(curl -s \
    -X POST "${base_url}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${email}\",\"password\":\"${password}\"}" \
    --max-time 15 2>/dev/null || echo "{}")

  token=$(echo "$login_resp" | jq -r '.accessToken // empty' 2>/dev/null || true)
  if [ -z "$token" ]; then
    # User may have already been deleted or never created
    echo "skip" > "${tmp}/${i}"
    return
  fi

  # Delete the account
  del_status=$(curl -s -o /dev/null -w "%{http_code}" \
    -X DELETE "${base_url}/api/v1/users/me" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${token}" \
    -d "{\"password\":\"${password}\"}" \
    --max-time 15 2>/dev/null || echo "000")

  echo "$del_status" > "${tmp}/${i}"
}
export -f delete_user

for i in $(seq 1 $COUNT); do
  delete_user "$i" "$BASE_URL" "$PASSWORD" "$tmp_dir" &
  if (( i % BATCH == 0 )); then
    wait
  fi
done
wait

# Tally results
for i in $(seq 1 $COUNT); do
  result=$(cat "${tmp_dir}/${i}" 2>/dev/null || echo "000")
  if [ "$result" = "skip" ]; then
    ((skip++)) || true
  elif [[ "$result" == "204" || "$result" == "200" ]]; then
    ((success++)) || true
  else
    ((fail++)) || true
    echo "[teardown]   WARN: loadtest_user_$(printf '%03d' $i) → HTTP $result"
  fi
done
rm -rf "$tmp_dir"

echo "[teardown] Deleted: ${success}, skipped (not found): ${skip}, failed: ${fail}."

# ── Remove generated files ────────────────────────────────────────────────────
for f in "${SCRIPT_DIR}/users.json" "${SCRIPT_DIR}/room_id.txt"; do
  if [ -f "$f" ]; then
    rm "$f"
    echo "[teardown] Removed ${f}."
  fi
done

echo "[teardown] Done."
