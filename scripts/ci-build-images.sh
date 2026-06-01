#!/usr/bin/env bash
# Build all backend Docker images; optional push to a registry.
# Usage (from Back-End):
#   IMAGE_TAG=ci-local ./scripts/ci-build-images.sh
#   IMAGE_PREFIX=docker.io/baonguyen6f6562/mychatapp IMAGE_TAG=v1 PUSH=1 ./scripts/ci-build-images.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

IMAGE_TAG="${IMAGE_TAG:-ci-local}"
IMAGE_PREFIX="${IMAGE_PREFIX:-mychatapp}"
PUSH="${PUSH:-0}"

is_push_enabled() {
  case "${PUSH,,}" in
    1 | true | yes) return 0 ;;
    *) return 1 ;;
  esac
}

BUILT_TAGS=()

build_image() {
  local key="$1"
  local dockerfile="$2"
  local context="$3"
  local suffix="$4"
  local tag="${IMAGE_PREFIX}:${suffix}-${IMAGE_TAG}"

  echo ""
  echo "=== Building ${key} -> ${tag} ==="
  docker build -f "$dockerfile" -t "$tag" "$context"
  BUILT_TAGS+=("$tag")
}

# gateway, auth, chat, user, notification, media (context .)
build_image gateway api-gateway/Dockerfile . gateway
build_image auth auth-service/Dockerfile . auth
build_image chat chat-service/Dockerfile . chat
build_image user user-service/Dockerfile . user
build_image notification notification-service/Dockerfile . notification
build_image media media-service/Dockerfile . media
build_image magika magika-service/Dockerfile magika-service magika
build_image agent agent-service/Dockerfile agent-service agent

if is_push_enabled; then
  echo ""
  echo "=== Pushing ${#BUILT_TAGS[@]} image(s) ==="
  for tag in "${BUILT_TAGS[@]}"; do
    echo "  docker push ${tag}"
    docker push "$tag"
  done
fi

echo ""
if is_push_enabled; then
  echo "Done. Built and pushed tags *-${IMAGE_TAG} under ${IMAGE_PREFIX}"
else
  echo "Done. Images tagged *-${IMAGE_TAG} under ${IMAGE_PREFIX} (PUSH not set)"
fi
