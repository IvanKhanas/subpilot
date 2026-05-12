# SubPilot

Telegram AI bot backend as a microservice system: paid subscriptions, free quota, cashback loyalty points, admin API, and production-style observability/security stack.

The project is designed as a strong portfolio case for a junior/stage backend role in fintech teams (T-Банк / Альфа): reliability, idempotency, security boundaries, and test coverage are first-class concerns.

## Architecture

### Services

| Service | HTTP | gRPC | Main responsibility |
|---|---:|---:|---|
| `tg-bot-service` | 1991 | 9081 | Telegram UX, command handling, async notifications |
| `chat-service` | 8082 | 9090 | Chat orchestration, access check + refund on AI failure |
| `subscription-service` | 8083 | 9091 | Users, plans, balances, model preferences, activation |
| `payment-service` | 8084 | 9094 | YooKassa payments, webhook handling, outbox publishing |
| `loyalty-service` | 8085 | 9095 | Cashback accrual/spend, points-based activation |
| `admin-service` | 8086 | — | Admin REST API, audit log, auth endpoints |
| `gateway-service` | 8088 | — | Public API auth + proxy to admin-service |

### Shared modules

- `proto` — gRPC contracts (contract-first API).
- `migrations` — Liquibase changelogs for all service DBs.
- `ktlint-rules` — custom lint rules.
- `buildSrc` — shared build conventions (`license`, `jacoco`).

### Infra stack (`infra/`)

- PostgreSQL
- Redis
- Kafka (KRaft, 3 brokers)
- Nginx (TLS termination + routing)
- Prometheus + Grafana

---

## Engineering practices implemented

### 1) Contract-first integration

All gRPC APIs are defined in `proto/` and generated for consumers/providers. This makes service boundaries explicit and reviewable.

### 2) Reliability in inter-service calls

Outbound gRPC wrappers use retry only for `UNAVAILABLE` with exponential backoff:

- `GRPC_RETRY_MAX_ATTEMPTS` (default `3`)
- `GRPC_RETRY_INITIAL_BACKOFF_MS` (default `200`)
- `GRPC_RETRY_BACKOFF_MULTIPLIER` (default `3.0`)

### 3) Transactional outbox for payment events

Card payment success flow in `payment-service`:

1. webhook marks payment `SUCCEEDED`
2. same DB transaction inserts row into `outbox_payment_event`
3. scheduler publishes unpublished rows to Kafka topic `payment_succeeded`
4. rows are marked as published

This prevents "DB committed but event lost" scenarios.

### 4) Idempotency as a business invariant

Duplicate deliveries are safe by design.

Examples in DB schema:

- `subscription`: `UNIQUE(payment_id, provider)`
- `loyalty`: `UNIQUE(payment_id, type)`

Admin and loyalty operations use idempotency keys where needed.

### 5) Security boundaries and scoped authorization

- Nginx is the only public entry point.
- `/payment/webhook` is IP-restricted to YooKassa ranges.
- Public admin API goes through `gateway-service`.
- Access policy:
  - `GET /api/v1/admin/**` -> `admin.read` or `admin.write`
  - `POST|PUT|PATCH|DELETE /api/v1/admin/**` -> `admin.write`
- Gateway issues internal JWT for hop to `admin-service`.

### 6) Observability and actionable alerts

Micrometer metrics are exposed via `/actuator/prometheus` from services.

Business metrics included:

- `payments_succeeded_total`
- `webhook_failures_total`
- `outbox_backlog_size`
- `subscription_activations_total`
- `user_registrations_total`
- `prompts_total`

Prometheus alert rules include:

- high HTTP 5xx rate
- high p99 API latency
- outbox backlog growth/critical
- webhook failures
- Kafka consumer lag high/critical

### 7) Quality gates in build + CI

- `ktlintCheck` + custom rules from `ktlint-rules`
- `spotlessCheck` for license headers
- JaCoCo convention plugin with line coverage threshold (`>= 80%`)
- GitHub Actions pipeline (`.github/workflows/github_ci.yml`): build, test + JaCoCo reports, lint, optional SonarQube

### 8) Test pyramid in practice

Per service test layout:

- `unittests/` — business logic with mocks
- `integrationtests/` — Spring context / WireMock
- `testcontainers/` — real PostgreSQL / Kafka / Redis dependent scenarios

---

## Core business flows

### Chat request flow

```text
tg-bot-service
  -> chat-service.ProcessMessage
      -> subscription-service.CheckAccess (atomic debit)
      -> OpenAI call
      -> subscription-service.RefundAccess (on AI failure)
```

### Card payment activation flow

```text
YooKassa webhook -> payment-service
  [TX] update payment status + insert outbox row
  [Scheduler] publish to Kafka payment_succeeded

payment_succeeded consumed by:
  - subscription-service (activate + publish subscription_activated)
  - loyalty-service (cashback accrual)

subscription_activated consumed by:
  - tg-bot-service (notify user)
```

### Full bonus payment flow (no card)

```text
tg-bot-service -> loyalty-service.SpendPoints
  -> subscription-service.ActivateSubscription (direct gRPC)
```

---

## Repository structure

```text
admin-service/
chat-service/
gateway-service/
loyalty-service/
payment-service/
subscription-service/
tg-bot-service/
proto/
migrations/
infra/
ktlint-rules/
buildSrc/
```

---

## Quick start (Docker)

### 1) Prerequisites

- Docker + Docker Compose
- JDK 21 (for local Gradle commands)

### 2) Configure env

```bash
cp .env.example .env
```

Fill at least required secrets:

- `TELEGRAM_BOT_TOKEN`
- `OPENAI_API_KEY`
- `YOOKASSA_SHOP_ID`
- `YOOKASSA_SECRET_KEY`
- `YOOKASSA_RETURN_URL`
- `ADMIN_JWT_SECRET`
- `ADMIN_AUTH_JWT_SECRET`
- DB passwords (`SUBSCRIPTION_DB_PASSWORD`, `PAYMENT_DB_PASSWORD`, `LOYALTY_DB_PASSWORD`, `ADMIN_DB_PASSWORD`)

### 3) Start stack

```bash
docker compose up --build
```

### 4) Useful URLs

- Kafka UI: `http://localhost:8090`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Public API entrypoint: `https://<NGINX_SERVER_NAME>/api/v1/`

---

## Admin API usage (through gateway)

Optional bootstrap admin in `.env`:

- `ADMIN_BOOTSTRAP_USERNAME`
- `ADMIN_BOOTSTRAP_PASSWORD`

Login:

```bash
curl -s 'https://<host>/api/v1/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"change-me"}'
```

Use `accessToken` for admin endpoints:

```bash
curl 'https://<host>/api/v1/admin/audit?page=0&size=20' \
  -H 'Authorization: Bearer <accessToken>'
```

---

## Environment reference

`/.env.example` is the source of truth. Highlights:

### Required for normal run

- Telegram/OpenAI: `TELEGRAM_BOT_TOKEN`, `OPENAI_API_KEY`
- Payment: `YOOKASSA_SHOP_ID`, `YOOKASSA_SECRET_KEY`, `YOOKASSA_RETURN_URL`
- Security: `ADMIN_JWT_SECRET`, `ADMIN_AUTH_JWT_SECRET`
- Persistence: all `*_DB_PASSWORD`

### Important optional tuning

- gRPC retries: `GRPC_RETRY_*`
- Free tier behavior: `SUBSCRIPTION_FREE_QUOTA`, `SUBSCRIPTION_FREE_QUOTA_RESET_PERIOD`
- JWT TTL: `ADMIN_AUTH_ACCESS_TTL_SECONDS`, `ADMIN_AUTH_REFRESH_TTL_SECONDS`, `GATEWAY_INTERNAL_JWT_TTL_SECONDS`
- Bot long-polling: `TELEGRAM_POLLING_TIMEOUT`, `TELEGRAM_POLLING_RETRY_DELAY_MS`
- Observability/UI: `GRAFANA_ADMIN_PASSWORD`

---

## Build, test, lint

```bash
# Full test run
./gradlew test

# One service tests
./gradlew :payment-service:test

# Lint
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat

# License headers
./gradlew spotlessCheck

# Coverage reports
./gradlew jacocoTestReport
```

---

## How to present this on an interview

Good narrative for a junior fintech interview:

1. Explain one reliability problem and your solution (outbox + idempotency).
2. Show one security boundary decision (public edge in Nginx + scoped JWT in gateway).
3. Show one operability decision (metrics/alerts you would watch in production).
4. Show one quality decision (CI gates + coverage threshold + testcontainers).
5. Discuss one tradeoff and next step (for example: add distributed tracing, add contract tests, move secrets to Vault).

---

## License

Apache-2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
