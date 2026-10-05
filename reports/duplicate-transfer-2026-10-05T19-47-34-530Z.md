# Duplicate transfer scenario

Run at: 2026-10-05T19:47:34.530Z

The same transfer of 12345 (one Idempotency-Key, identical body) was sent 5 times simultaneously.

| Check | Expected | Actual | Result |
|---|---|---|---|
| Requests that applied the transfer | 1 | 1 | PASS |
| Requests answered from the stored result (Idempotent-Replayed: true) | 4 | 4 | PASS |
| Unexpected responses | 0 | 0 | PASS |
| Sender final balance | 37655 | 37655 | PASS |
| Receiver final balance | 12345 | 12345 | PASS |
| Transfers in sender history | 1 | 1 | PASS |
| Transfers in receiver history | 1 | 1 | PASS |
| Ledger invariants | 1 (100%) | 1 | PASS |

HTTP latency (all requests): avg 17.7 ms, p95 40.8 ms, max 50.4 ms

**Overall: PASS**
