#!/usr/bin/env bash
# Pull MyChatApp images from ECR and tag for docker-compose (mychatapp:*-v1)
#
# Usage (on Ubuntu EC2):
#   chmod +x scripts/ecr-pull.sh
#   ./scripts/ecr-pull.sh
#
# With options:
#   ./scripts/ecr-pull.sh --tag v1 --ecr 322725461022.dkr.ecr.ap-southeast-1.amazonaws.com/mychat-app
#
# Env overrides:
#   ECR_URI, IMAGE_TAG, AWS_REGION, IMAGE_PREFIX, USE_SUDO=1

set -euo pipefail

REGION="${AWS_REGION:-ap-southeast-1}"
ECR_URI="${ECR_URI:-322725461022.dkr.ecr.ap-southeast-1.amazonaws.com/mychat-app}"
IMAGE_TAG="${IMAGE_TAG:-v1}"
IMAGE_PREFIX="${IMAGE_PREFIX:-mychatapp}"
USE_SUDO="${USE_SUDO:-}"

SERVICES=(gateway auth chat user notification media magika agent)

usage() {
  sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
  echo ""
  echo "Options:"
  echo "  --tag TAG       Image tag suffix (default: v1) -> auth-v1, gateway-v1, ..."
  echo "  --ecr URI       ECR repository URI (no :tag)"
  echo "  --region REGION AWS region (default: ap-southeast-1)"
  echo "  --prefix NAME   Local tag prefix (default: mychatapp)"
  echo "  --no-login      Skip aws ecr login"
  echo "  --sudo          Run docker via sudo"
  echo "  -h, --help      Show this help"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag) IMAGE_TAG="$2"; shift 2 ;;
    --ecr) ECR_URI="$2"; shift 2 ;;
    --region) REGION="$2"; shift 2 ;;
    --prefix) IMAGE_PREFIX="$2"; shift 2 ;;
    --no-login) NO_LOGIN=1; shift ;;
    --sudo) USE_SUDO=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
done

# Strip accidental :tag from ECR_URI
if [[ "$ECR_URI" =~ ^(.+/.+):([^/]+)$ ]]; then
  echo "Note: stripped tag from ECR_URI, using repository '${BASH_REMATCH[1]}'" >&2
  ECR_URI="${BASH_REMATCH[1]}"
fi

DOCKER=(docker)
if [[ -n "$USE_SUDO" ]] || ! docker info &>/dev/null; then
  DOCKER=(sudo docker)
fi

REGISTRY="${ECR_URI%%/*}"

echo "==> Region:      $REGION"
echo "==> ECR repo:    $ECR_URI"
echo "==> Tag suffix:  *-${IMAGE_TAG}"
echo "==> Local names: ${IMAGE_PREFIX}:<service>-${IMAGE_TAG}"
echo "==> Docker:      ${DOCKER[*]}"
echo ""

if [[ -z "${NO_LOGIN:-}" ]]; then
  echo "==> ECR login..."
  aws ecr get-login-password --region "$REGION" \
    | "${DOCKER[@]}" login --username AWS --password-stdin "$REGISTRY"
fi

for svc in "${SERVICES[@]}"; do
  remote="${ECR_URI}:${svc}-${IMAGE_TAG}"
  local="${IMAGE_PREFIX}:${svc}-${IMAGE_TAG}"
  echo ""
  echo "==> Pull $remote"
  "${DOCKER[@]}" pull "$remote"
  echo "==> Tag  $local"
  "${DOCKER[@]}" tag "$remote" "$local"
done

echo ""
echo "Done. Images ready for compose (IMAGE_TAG=${IMAGE_TAG} in .env.docker)."
echo "  ${DOCKER[*]} images | grep ${IMAGE_PREFIX}"
