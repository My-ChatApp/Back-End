#!/usr/bin/env bash
# WebSocket load: N users, STOMP send × M, then GET messages.
#
#   ./load-test/scripts/run-ws-load.sh
#   USERS=100 CONCURRENCY=100 RAMP_UP_SEC=60 ./load-test/scripts/run-ws-load.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOAD_TEST_ROOT="$BACKEND_ROOT/load-test"
NODE_SCRIPT="$LOAD_TEST_ROOT/scripts/ws-send-then-get.mjs"
CSV="$LOAD_TEST_ROOT/data/users.csv"

HOST="${HOST:-localhost}"
PORT="${PORT:-8080}"
USERS="${USERS:-500}"
CONCURRENCY="${CONCURRENCY:-500}"
RAMP_UP_SEC="${RAMP_UP_SEC:-180}"
MESSAGES="${MESSAGES:-5}"
SETTLE_MS="${SETTLE_MS:-2000}"
SEND_GAP_MS="${SEND_GAP_MS:-0}"
SERVER_WAIT_MS="${SERVER_WAIT_MS:-8000}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="$LOAD_TEST_ROOT/results/ws-$TIMESTAMP"

[[ -f "$NODE_SCRIPT" ]] || { echo "Missing $NODE_SCRIPT"; exit 1; }
[[ -f "$CSV" ]] || { echo "Missing $CSV — run generate-loadtest-data.mjs"; exit 1; }

cd "$LOAD_TEST_ROOT"
npm install --omit=dev

ARGS=(
  "$NODE_SCRIPT"
  --host "$HOST"
  --port "$PORT"
  --csv "$CSV"
  --users "$USERS"
  --concurrency "$CONCURRENCY"
  --ramp-up-sec "$RAMP_UP_SEC"
  --messages "$MESSAGES"
  --settle-ms "$SETTLE_MS"
  --send-gap-ms "$SEND_GAP_MS"
  --server-wait-ms "$SERVER_WAIT_MS"
  --out-dir "$OUT_DIR"
)
[[ "${DEBUG:-}" == "1" ]] && ARGS+=(--debug)

node "${ARGS[@]}"
echo ""
echo "Results: $OUT_DIR"
