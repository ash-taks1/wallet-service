#!/usr/bin/env bash
# Section 7 proof: performs a transfer (k6), then extracts all logs of that single request, across
# wallet-service and wallet-event-consumer, by its trace id.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

"$ROOT/scripts/k6.sh" trace-demo.js
TRACE_ID="$(jq -r .traceId "$ROOT/reports/trace-demo.json")"

# Give the outbox relay and the consumer a moment to finish (normally well under a second).
sleep "${SETTLE_SECONDS:-2}"
"$ROOT/scripts/trace.sh" "$TRACE_ID"
