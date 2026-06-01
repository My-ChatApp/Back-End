#!/usr/bin/env bash
# Run Spring unit tests (no infra). Excludes *ApplicationTests (full context).
# Usage (from Back-End): ./scripts/ci-test.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

SUREFIRE_EXCLUDES='**/*ApplicationTests.java'

prepare_mvnw() {
  if [[ -f mvnw ]]; then
    sed -i 's/\r$//' mvnw 2>/dev/null || sed -i '' 's/\r$//' mvnw 2>/dev/null || true
    chmod +x mvnw
  fi
}

run_module_test() {
  local pom="$1"
  local name="$2"
  echo ""
  echo "=== Testing ${name} (${pom}) ==="
  ./mvnw -f "$pom" test \
    -Dsurefire.excludes="$SUREFIRE_EXCLUDES"
}

prepare_mvnw

echo "=== Installing common (required by services) ==="
./mvnw -f common/pom.xml install -DskipTests

MODULES=(
  "common/pom.xml:common"
  "api-gateway/pom.xml:api-gateway"
  "auth-service/pom.xml:auth-service"
  "chat-service/pom.xml:chat-service"
  "user-service/pom.xml:user-service"
  "notification-service/pom.xml:notification-service"
)

for entry in "${MODULES[@]}"; do
  pom="${entry%%:*}"
  name="${entry##*:}"
  run_module_test "$pom" "$name"
done

echo ""
echo "All unit tests passed."
