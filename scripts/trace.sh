#!/usr/bin/env bash
# Reconstructs everything that happened for one request using only its trace id: collects the JSON
# log lines carrying that traceId from every service container, merges them in time order and
# writes reports/trace-<id>.log (readable) and reports/trace-<id>.jsonl (raw). It also prints a link
# that opens the same trace in Kibana (logs are shipped there by Filebeat).
#
# Usage: scripts/trace.sh <trace-id>
# TRACE_CONTAINERS overrides the containers searched (default: wallet-service wallet-event-consumer).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TRACE_ID="${1:?usage: scripts/trace.sh <trace-id>}"
read -r -a CONTAINERS <<< "${TRACE_CONTAINERS:-wallet-service wallet-event-consumer}"
OUT="$ROOT/reports/trace-$TRACE_ID"
mkdir -p "$ROOT/reports"

for container in "${CONTAINERS[@]}"; do
  if docker inspect "$container" >/dev/null 2>&1; then
    docker logs "$container" 2>&1 | grep -F "\"traceId\":\"$TRACE_ID\"" || true
  else
    echo "warning: container '$container' not found, skipping" >&2
  fi
done | jq -sc 'sort_by(."@timestamp")[]' > "$OUT.jsonl"

KIBANA_URL="http://localhost:5602/app/discover#/view/wallet-trace?_g=(time:(from:now-24h,to:now))&_a=(columns:!(service.name,event.action,message,traceId),sort:!(!('@timestamp',asc),!(log.offset,asc)),query:(language:kuery,query:'traceId:%22$TRACE_ID%22'))"

{
  echo "Trace $TRACE_ID: $(wc -l < "$OUT.jsonl") log lines from ${CONTAINERS[*]}"
  echo "Kibana (same trace, in the UI; logs arrive there a few seconds later):"
  echo "  $KIBANA_URL"
  echo
  jq -r '[."@timestamp", .service.name, .log.level, (.event.action // "-"), .message] | @tsv' "$OUT.jsonl" \
    | column -t -s $'\t'
} | tee "$OUT.log"
