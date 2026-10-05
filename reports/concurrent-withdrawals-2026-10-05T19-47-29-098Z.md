# Concurrent withdrawals scenario

Run at: 2026-10-05T19:47:29.098Z

Wallet funded with 100000; 50 withdrawals of 3000 sent in parallel (one per VU, same start instant).
A separate VU sampled the balance while the requests were in flight.

| Check | Expected | Actual | Result |
|---|---|---|---|
| Successful withdrawals (HTTP 201) | 33 | 33 | PASS |
| Rejected with INSUFFICIENT_FUNDS (HTTP 422) | 17 | 17 | PASS |
| Unexpected responses | 0 | 0 | PASS |
| Final balance | 1000 | 1000 | PASS |
| Negative balance observations | 0 | 0 | PASS |
| Sum of COMPLETED withdrawals in history | 99000 | 99000 | PASS |
| COMPLETED withdrawals in history | 33 | 33 | PASS |
| FAILED withdrawals in history | 17 | 17 | PASS |
| Ledger/state invariants | 1 (100%) | 1 | PASS |

HTTP latency (all requests): avg 65.9 ms, p95 293.6 ms, max 332.8 ms
Balance samples taken during the run: min 1000, max 100000.

**Overall: PASS**
