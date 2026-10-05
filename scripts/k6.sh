#!/usr/bin/env bash
# Runs a k6 scenario from ./k6 inside the official grafana/k6 container (no local install needed).
# Usage: scripts/k6.sh <scenario.js> [extra k6 args]
# Reports are written to ./reports.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCENARIO="${1:?usage: scripts/k6.sh <scenario.js> [k6 args]}"
shift

mkdir -p "$ROOT/reports"
exec docker run --rm -i --network host \
  --user "$(id -u):$(id -g)" \
  -e WALLET_URL="${WALLET_URL:-http://localhost:8090}" \
  -e CONSUMER_URL="${CONSUMER_URL:-http://localhost:8091}" \
  -e REPORT_DIR=/reports \
  -e NO_PROXY="localhost,127.0.0.1" -e no_proxy="localhost,127.0.0.1" \
  -v "$ROOT/k6:/scripts:ro" \
  -v "$ROOT/reports:/reports" \
  grafana/k6:latest run --quiet "$@" "/scripts/$SCENARIO"
