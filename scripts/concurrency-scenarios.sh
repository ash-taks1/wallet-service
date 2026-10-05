#!/usr/bin/env bash
# Runs both Section 5 scenarios against the running stack, RUNS times (default 1).
# Exits non-zero as soon as any k6 threshold fails. Reports: ./reports/*-latest.md
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNS="${RUNS:-1}"

for run in $(seq 1 "$RUNS"); do
  echo "=== Run $run/$RUNS: 50 concurrent withdrawals ==="
  "$ROOT/scripts/k6.sh" concurrent-withdrawals.js
  echo "=== Run $run/$RUNS: 5 simultaneous duplicate transfers ==="
  "$ROOT/scripts/k6.sh" duplicate-transfer.js
done
echo "All scenarios passed ($RUNS run(s)). Reports in $ROOT/reports"
