#!/usr/bin/env bash
# Deploy on EC2: pull images from Docker Hub, update IMAGE_TAG, restart compose stack.
#
# Usage:
#   cd Back-End
#   export DOCKERHUB_USERNAME=... DOCKERHUB_TOKEN=...   # if private
#   ./scripts/deploy-ec2.sh --tag ci-<full-git-sha>
#
# Env:
#   ENV_FILE        default .env.docker
#   USE_SUDO=1      use sudo docker

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

IMAGE_TAG=""
USE_SUDO="${USE_SUDO:-}"
NO_LOGIN=""

usage() {
  sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
  echo ""
  echo "Options:"
  echo "  --tag TAG       Required (must match CI push, e.g. ci-<sha>)"
  echo "  --no-login      Skip Docker Hub login"
  echo "  --sudo          Run docker via sudo"
  echo "  -h, --help      Show this help"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag) IMAGE_TAG="$2"; shift 2 ;;
    --no-login) NO_LOGIN=1; shift ;;
    --sudo) USE_SUDO=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$IMAGE_TAG" ]]; then
  echo "Error: --tag is required" >&2
  usage
  exit 1
fi

ENV_FILE="${ENV_FILE:-.env.docker}"
if [[ ! -f "$ENV_FILE" ]] && [[ -f .env.docker.example ]]; then
  cp .env.docker.example "$ENV_FILE"
  echo "Created $ENV_FILE from example — edit secrets before production."
fi

PULL_ARGS=(--tag "$IMAGE_TAG")
[[ -n "$NO_LOGIN" ]] && PULL_ARGS+=(--no-login)
[[ -n "$USE_SUDO" ]] && PULL_ARGS+=(--sudo)

chmod +x scripts/dockerhub-pull.sh
./scripts/dockerhub-pull.sh "${PULL_ARGS[@]}"

if grep -q '^IMAGE_TAG=' "$ENV_FILE" 2>/dev/null; then
  sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=${IMAGE_TAG}/" "$ENV_FILE"
else
  echo "IMAGE_TAG=${IMAGE_TAG}" >> "$ENV_FILE"
fi

COMPOSE=(docker compose)
if [[ -n "$USE_SUDO" ]] || ! docker info &>/dev/null; then
  COMPOSE=(sudo docker compose)
fi

echo ""
echo "==> Starting infra (if not running)..."
"${COMPOSE[@]}" -f docker-compose.yml up -d postgres rabbitmq valkey

echo "==> Restarting app containers (IMAGE_TAG=${IMAGE_TAG})..."
"${COMPOSE[@]}" --env-file "$ENV_FILE" \
  -f docker-compose.yml \
  -f docker-compose.apps.yml \
  up -d --remove-orphans

echo ""
echo "Deploy complete. Gateway: http://localhost:8080/api/auth/health"
