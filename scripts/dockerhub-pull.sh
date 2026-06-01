#!/usr/bin/env bash
# Pull MyChatApp images from Docker Hub and tag for docker-compose (mychatapp:*-<tag>)
#
# Usage (on Ubuntu EC2):
#   chmod +x scripts/dockerhub-pull.sh
#   ./scripts/dockerhub-pull.sh --tag ci-abc1234
#
# With login (private repo):
#   export DOCKERHUB_USERNAME=...
#   export DOCKERHUB_TOKEN=...
#   ./scripts/dockerhub-pull.sh --tag ci-abc1234
#
# Env overrides:
#   DOCKERHUB_REPO, IMAGE_TAG, IMAGE_PREFIX, USE_SUDO=1

set -euo pipefail

DOCKERHUB_REPO="${DOCKERHUB_REPO:-docker.io/baonguyen6f6562/mychatapp}"
IMAGE_TAG="${IMAGE_TAG:-}"
IMAGE_PREFIX="${IMAGE_PREFIX:-mychatapp}"
USE_SUDO="${USE_SUDO:-}"

SERVICES=(gateway auth chat user notification media magika agent)

usage() {
  sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
  echo ""
  echo "Options:"
  echo "  --tag TAG       Required. Tag suffix -> gateway-<tag>, auth-<tag>, ..."
  echo "  --repo URI      Docker Hub repo (default: docker.io/baonguyen6f6562/mychatapp)"
  echo "  --prefix NAME   Local tag prefix (default: mychatapp)"
  echo "  --no-login      Skip docker login"
  echo "  --sudo          Run docker via sudo"
  echo "  -h, --help      Show this help"
}

NO_LOGIN=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag) IMAGE_TAG="$2"; shift 2 ;;
    --repo) DOCKERHUB_REPO="$2"; shift 2 ;;
    --prefix) IMAGE_PREFIX="$2"; shift 2 ;;
    --no-login) NO_LOGIN=1; shift ;;
    --sudo) USE_SUDO=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$IMAGE_TAG" ]]; then
  echo "Error: --tag is required (e.g. ci-\$(git rev-parse HEAD))" >&2
  usage
  exit 1
fi

DOCKER=(docker)
if [[ -n "$USE_SUDO" ]] || ! docker info &>/dev/null; then
  DOCKER=(sudo docker)
fi

echo "==> Docker Hub:  $DOCKERHUB_REPO"
echo "==> Tag suffix:  *-${IMAGE_TAG}"
echo "==> Local names: ${IMAGE_PREFIX}:<service>-${IMAGE_TAG}"
echo "==> Docker:      ${DOCKER[*]}"
echo ""

if [[ -z "$NO_LOGIN" ]] && [[ -n "${DOCKERHUB_USERNAME:-}" ]] && [[ -n "${DOCKERHUB_TOKEN:-}" ]]; then
  echo "==> Docker Hub login..."
  echo "$DOCKERHUB_TOKEN" | "${DOCKER[@]}" login -u "$DOCKERHUB_USERNAME" --password-stdin
elif [[ -z "$NO_LOGIN" ]]; then
  echo "==> Docker Hub login skipped (set DOCKERHUB_USERNAME + DOCKERHUB_TOKEN for private repos)"
fi

for svc in "${SERVICES[@]}"; do
  remote="${DOCKERHUB_REPO}:${svc}-${IMAGE_TAG}"
  local="${IMAGE_PREFIX}:${svc}-${IMAGE_TAG}"
  echo ""
  echo "==> Pull $remote"
  "${DOCKER[@]}" pull "$remote"
  echo "==> Tag  $local"
  "${DOCKER[@]}" tag "$remote" "$local"
done

echo ""
echo "Done. Images ready for compose (IMAGE_TAG=${IMAGE_TAG} in .env.docker)."
