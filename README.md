# SubPilot

Backend for a Telegram AI bot, built as Kotlin/Spring Boot microservices.

## Current modules

- `tg-bot-service` — Telegram transport layer, commands, text-button UX, and gRPC clients.
- `chat-service` — OpenAI chat orchestration + Redis chat history.
- `subscription-service` — user registration, model preference, free quota and paid-balance access checks.
- `payment-service` — Spring Boot skeleton (payment domain is not implemented yet).
- `proto` — shared gRPC contracts.
- `migrations` — Liquibase changelogs (currently used by `subscription-service`).
- `ktlint-rules` — custom ktlint ruleset module used by the build.

## Implemented behavior

### `tg-bot-service`

- Commands: `/start`, `/help`, `/menu`, `/model <model_id>`, `/support`.
- Main-menu and model-selection UX via Telegram reply keyboards.
- Registers user in `subscription-service` on `/start`.
- Sends chat requests to `chat-service`.
- Handles structured denial reasons from chat flow:
  - `QUOTA_EXHAUSTED`
  - `NO_SUBSCRIPTION`
  - `BLOCKED`
- Clears chat context when model provider changes.

### `chat-service`

- gRPC API:
  - `ProcessMessage`
  - `ClearHistory`
- Resolves user model from `subscription-service`.
- Runs access check in `subscription-service` before OpenAI call.
- Stores recent conversation in Redis (`chat:history:{chatId}`), bounded and TTL-based.
- Refunds consumed quota through `subscription-service` when processing fails after access was consumed.

### `subscription-service`

- gRPC API:
  - `CheckAccess`
  - `RefundAccess`
  - `RegisterUser`
  - `GetModelPreference`
  - `SetModelPreference`
- PostgreSQL + Liquibase persistence.
- Keeps free quota per provider with reset period.
- Splits consumption between free and paid balance.
- Returns metadata for UI/UX (`available_requests`, `model_cost`, `reset_at_epoch`, consumed counters).

## Runtime topology

```
Telegram
  -> tg-bot-service
      -> (gRPC) chat-service
      -> (gRPC) subscription-service

chat-service
  -> (gRPC) subscription-service
  -> Redis

subscription-service
  -> PostgreSQL (Liquibase)
```

## Environment configuration

Repository uses `.env.example` as the canonical env template (if you were using `.env.sample`, switch to `.env.example`).

Minimum values you should set:

- `TELEGRAM_BOT_TOKEN`
- `OPENAI_API_KEY`
- `REDIS_PASSWORD` (must be non-empty for docker compose setup)
- `SUBSCRIPTION_DB_PASSWORD`

Most other keys in `.env.example` are optional overrides with service defaults.

## Local run (Docker Compose)

```bash
cp .env.example .env
# fill required secrets
docker compose up --build
```

Services and ports:

- `tg-bot-service`: `1991`
- `chat-service`: `8082` (HTTP), `9090` (gRPC)
- `subscription-service`: `8083` (HTTP), `9091` (gRPC)
- `postgres`: `5432`
- `redis`: `6379`

## Build and tests

```bash
./gradlew build
./gradlew test

# module-level examples
./gradlew :tg-bot-service:test
./gradlew :chat-service:test
./gradlew :subscription-service:test
```

## Status notes

- Payment flow, YooKassa integration, Kafka outbox/events, and notification service are not implemented in this repository state.

## License

Apache-2.0. See `LICENSE` and `NOTICE`.
