#!/usr/bin/env bash
# Start infra + app stack on Linux (EC2). Requires images: ./scripts/ecr-pull.sh
#
# Usage:
#   cd Back-End-NDuy
#   cp .env.docker.example .env.docker   # edit secrets
#   ./scripts/ecr-pull.sh
#   ./scripts/compose-up.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

ENV_FILE="${ENV_FILE:-.env.docker}"
USE_SUDO="${USE_SUDO:-}"

DOCKER=(docker)
COMPOSE=(docker compose)
if [[ -n "$USE_SUDO" ]] || ! docker info &>/dev/null; then
  DOCKER=(sudo docker)
  COMPOSE=(sudo docker compose)
fi

if [[ ! -f "$ENV_FILE" ]]; then
  if [[ -f .env.docker.example ]]; then
    cp .env.docker.example "$ENV_FILE"
    echo "Created $ENV_FILE from example — edit AWS/S3/JWT before production."
  else
    echo "Missing $ENV_FILE" >&2
    exit 1
  fi
fi

IMAGE_TAG=v1
# shellcheck disable=SC1090
set -a
source <(grep -E '^[[:space:]]*IMAGE_TAG=' "$ENV_FILE" | sed 's/\r$//')
set +a
IMAGE_TAG="${IMAGE_TAG:-v1}"

PREFIX="${IMAGE_PREFIX:-mychatapp}"
MISSING=()
for svc in gateway auth chat user notification media magika; do
  img="${PREFIX}:${svc}-${IMAGE_TAG}"
  if ! "${DOCKER[@]}" image inspect "$img" &>/dev/null; then
    MISSING+=("$img")
  fi
done

if [[ ${#MISSING[@]} -gt 0 ]]; then
  echo "Missing images (run ./scripts/ecr-pull.sh first):" >&2
  printf '  - %s\n' "${MISSING[@]}" >&2
  exit 1
fi

echo "==> Starting infra (postgres, rabbitmq, valkey)..."
"${COMPOSE[@]}" -f docker-compose.yml up -d postgres rabbitmq valkey

echo "==> Starting app containers..."
"${COMPOSE[@]}" --env-file "$ENV_FILE" \
  -f docker-compose.yml \
  -f docker-compose.apps.yml \
  up -d

cat <<EOF

Ready:
  Gateway:  http://localhost:8080/api/auth/health
  RabbitMQ: http://localhost:15673 (guest/guest)

Stop: ./scripts/compose-down.sh

EOF
