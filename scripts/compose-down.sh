#!/usr/bin/env bash
# Stop MyChatApp docker stack on Linux
# Usage: ./scripts/compose-down.sh [--apps-only]

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

USE_SUDO="${USE_SUDO:-}"
COMPOSE=(docker compose)
if [[ -n "$USE_SUDO" ]] || ! docker info &>/dev/null; then
  COMPOSE=(sudo docker compose)
fi

APPS_ONLY=0
[[ "${1:-}" == "--apps-only" ]] && APPS_ONLY=1

"${COMPOSE[@]}" -f docker-compose.yml -f docker-compose.apps.yml down

if [[ "$APPS_ONLY" -eq 0 ]]; then
  "${COMPOSE[@]}" -f docker-compose.yml down
  echo "All containers stopped."
else
  echo "App containers stopped. Infra still running."
fi
