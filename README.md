# wallet-service

REST API for a simple digital wallet: users register, log in, and move money in and out of their own wallet.

- Deposit, withdraw, transfer between wallets, balance and history
- A balance can never go negative, even under heavy concurrent load
- A repeated request with the same `Idempotency-Key` is applied only once
- Every completed transaction is published to RabbitMQ (transactional outbox)
- Structured JSON logs with one trace id per request, searchable in Kibana

Tech: Java 21, Spring Boot 4, PostgreSQL, RabbitMQ, Elasticsearch/Kibana/Filebeat, k6.

The events are consumed by a separate service, `wallet-event-consumer` (its own repository).

## Running

You only need Docker:

```bash
docker compose up -d --build
```

This starts PostgreSQL, RabbitMQ, Elasticsearch, Kibana, Filebeat and the service itself.
Start this stack before `wallet-event-consumer`. The consumer joins this stack's Docker network
(`digital-wallet`) and uses the same PostgreSQL server, but with its own database (`consumer`).

| Service | Address |
|---|---|
| API | http://localhost:8090/api/v1 |
| Swagger UI | http://localhost:8090/swagger-ui.html |
| Kibana | http://localhost:5602 |
| RabbitMQ UI | http://localhost:15672 (wallet / wallet) |
| PostgreSQL | localhost:5440 (wallet / wallet) |

To stop: `docker compose down`. To also delete all data: `docker compose down -v`.

## API

Register, log in, then send the token as `Authorization: Bearer <token>`. You can only access your own
wallets. Amounts are whole numbers in the smallest currency unit (IRR).

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/auth/register` | creates the user and a wallet |
| POST | `/api/v1/auth/login` | returns `accessToken` |
| GET | `/api/v1/wallets` | your wallets |
| GET | `/api/v1/wallets/{id}` | balance |
| POST | `/api/v1/wallets/{id}/deposits` | `{ "amount": 1000 }` |
| POST | `/api/v1/wallets/{id}/withdrawals` | `422 INSUFFICIENT_FUNDS` if the balance is too low |
| POST | `/api/v1/wallets/{id}/transfers` | `{ "toWalletId", "amount" }`, `Idempotency-Key` header required |
| GET | `/api/v1/wallets/{id}/transactions` | history, newest first |

Errors use the RFC 9457 problem format and include a `code` and the `traceId`.
Every response has an `X-Trace-Id` header.

## Concurrency scenarios

Run these with the stack up. k6 runs in Docker, so you don't need to install anything:

```bash
scripts/concurrency-scenarios.sh
```

1. **50 parallel withdrawals** of 3,000 from a balance of 100,000: exactly 33 succeed, 17 get
   `INSUFFICIENT_FUNDS`, the final balance is 1,000, and the history matches the balance.
2. **The same transfer sent 5 times at once** with one `Idempotency-Key`: it is applied once, and the other
   four get the stored response back (`Idempotent-Replayed: true`).

If any check fails, the script exits with an error. Reports are written to `reports/` as Markdown and JSON.
`RUNS=5 scripts/concurrency-scenarios.sh` repeats both scenarios to show the result is always the same.

## Logs and tracing

Each service writes one JSON object per log line to stdout. Filebeat ships those lines to Elasticsearch.
In Kibana, go to **Discover → Wallet trace** and search:

```
traceId : "<id from the X-Trace-Id header>"
```

You get the whole path of one request in order: request received, database commit, event published to
RabbitMQ, event processed by the consumer.

From the terminal:

```bash
scripts/trace-demo.sh          # makes a transfer and prints all of its log lines
scripts/trace.sh <traceId>     # same, for any trace id
```

## Tests

```bash
./mvnw test
```

Unit tests cover the wallet balance rules and the request fingerprint used for idempotency. The
concurrency and duplicate-request behaviour is tested end to end with the k6 scenarios above.

## How it works

- **Concurrency:** each balance change locks the wallet row (`SELECT ... FOR NO KEY UPDATE`) inside one
  database transaction. A transfer locks both wallets ordered by id, so two opposite transfers can't deadlock.
  The database also enforces `CHECK (balance >= 0)`.
- **Ledger:** every operation stores a transaction and ledger entries with the balance after it.
  Rejected operations are stored as `FAILED`.
- **Idempotency:** the key and a hash of the request are inserted in the same transaction as the money
  movement (`INSERT ... ON CONFLICT DO NOTHING`). A concurrent duplicate waits for the first request and
  then gets its result. The same key with a different body returns `422 IDEMPOTENCY_KEY_REUSED`.
- **Outbox:** the event row is written in the same transaction as the change. A relay publishes pending rows
  every 100 ms and marks them sent only after RabbitMQ confirms. If the service crashes in between, the event
  is published again, and the consumer drops the duplicate by event id.
- **Schema:** there is no migration tool. Hibernate creates the tables from the entities (`ddl-auto: update`).

## Configuration

| Variable | Default |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `localhost`, `5440`, `wallet`, `wallet`, `wallet` |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | `localhost`, `5672`, `wallet`, `wallet` |
| `JWT_SECRET` | a development value; set your own (32+ characters) |
