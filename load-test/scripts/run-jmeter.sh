#!/usr/bin/env bash
# Run JMeter load test (non-GUI) against api-gateway.
#
# Example:
#   ./load-test/scripts/run-jmeter.sh --threads 10 --rampup 10 --loops 2
#   ./load-test/scripts/run-jmeter.sh --threads 500 --rampup 180 --loops 20

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOAD_TEST_ROOT="$BACKEND_ROOT/load-test"
JMX="$LOAD_TEST_ROOT/jmeter/MyChatApp-500users-http.jmx"
CSV="$LOAD_TEST_ROOT/data/users.csv"

HOST="${HOST:-localhost}"
PORT="${PORT:-8080}"
THREADS="${THREADS:-500}"
RAMPUP="${RAMPUP:-180}"
LOOPS="${LOOPS:-20}"
THINK_MIN="${THINK_MIN:-1000}"
THINK_MAX="${THINK_MAX:-3000}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) HOST="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --threads) THREADS="$2"; shift 2 ;;
    --rampup) RAMPUP="$2"; shift 2 ;;
    --loops) LOOPS="$2"; shift 2 ;;
    -h|--help)
      echo "Usage: $0 [--host localhost] [--port 8080] [--threads 500] [--rampup 180] [--loops 20]"
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

THINK_RANGE=$((THINK_MAX - THINK_MIN))
[[ "$THINK_RANGE" -ge 0 ]] || THINK_RANGE=0

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
RESULT_DIR="$LOAD_TEST_ROOT/results/$TIMESTAMP"
JTL="$RESULT_DIR/results.jtl"
HTML_REPORT="$RESULT_DIR/html-report"

[[ -f "$JMX" ]] || { echo "JMX not found: $JMX"; exit 1; }
[[ -f "$CSV" ]] || {
  echo "users.csv not found: $CSV"
  echo "Run: node load-test/scripts/generate-loadtest-data.mjs --count 500 --out-dir load-test/data"
  exit 1
}

if [[ -n "${JMETER_HOME:-}" && -x "$JMETER_HOME/bin/jmeter" ]]; then
  JMETER="$JMETER_HOME/bin/jmeter"
elif command -v jmeter >/dev/null 2>&1; then
  JMETER="$(command -v jmeter)"
else
  echo "JMeter not found. Set JMETER_HOME or add jmeter to PATH."
  exit 1
fi

mkdir -p "$RESULT_DIR"
export HEAP="-Xms1g -Xmx4g"

echo "MyChatApp JMeter load test"
echo "  Target:  http://${HOST}:${PORT}"
echo "  Threads: $THREADS  Ramp-up: ${RAMPUP}s  Loops: $LOOPS"
echo "  CSV:     $CSV"
echo "  Results: $RESULT_DIR"
echo ""

"$JMETER" -n \
  -t "$JMX" \
  -l "$JTL" \
  -j "$RESULT_DIR/jmeter.log" \
  -Jhost="$HOST" \
  -Jport="$PORT" \
  -Jthreads="$THREADS" \
  -Jrampup="$RAMPUP" \
  -Jloops="$LOOPS" \
  -Jthink_min="$THINK_MIN" \
  -Jthink_range="$THINK_RANGE" \
  -Jcsvfile="$CSV"

if [[ ! -s "$JTL" ]] || [[ "$(wc -l < "$JTL")" -le 1 ]]; then
  echo ""
  echo "WARNING: No samples recorded. Check gateway and $RESULT_DIR/jmeter.log"
  exit 1
fi

echo ""
echo "Generating HTML report..."
"$JMETER" -g "$JTL" -o "$HTML_REPORT"

echo ""
echo "Done."
echo "  JTL:         $JTL"
echo "  HTML report: $HTML_REPORT/index.html"
