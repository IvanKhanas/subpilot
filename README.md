> **In progress: observability and administration**

# SubPilot

Backend for a Telegram AI chatbot with usage limits, paid subscriptions, and a cashback loyalty program. Built as five Kotlin/Spring Boot microservices communicating over gRPC and Kafka.

---

## Table of contents

- [Services](#services)
- [How the system works](#how-the-system-works)
- [gRPC call chains](#grpc-call-chains)
- [Kafka message flows](#kafka-message-flows)
- [Database schemas](#database-schemas)
- [Configuration reference](#configuration-reference)
- [How to add a subscription plan](#how-to-add-a-subscription-plan)
- [How to add a new AI model](#how-to-add-a-new-ai-model)
- [How to add a new AI provider](#how-to-add-a-new-ai-provider)
- [Local run](#local-run)
- [Build and tests](#build-and-tests)

---

## Services

| Service | HTTP port | gRPC port | Database |
|---|---|---|---|
| `tg-bot-service` | 1991 | — | Redis (navigation, chat history) |
| `chat-service` | 8082 | 9090 | Redis (chat history) |
| `subscription-service` | 8083 | 9091 | PostgreSQL `subscription` |
| `payment-service` | 8084 | 9094 | PostgreSQL `payment` |
| `loyalty-service` | 8085 | 9095 | PostgreSQL `loyalty` |

Supporting modules: `proto` (shared gRPC contracts), `migrations` (Liquibase changelogs per service), `ktlint-rules` (custom lint ruleset).

---

## How the system works

Users talk to the bot through Telegram. Each message goes through an access check: the user either has free quota left or a paid balance. When they run out, the bot prompts them to subscribe.

Subscription plans and model request costs live entirely in `subscription-service/src/main/resources/application.yml` — no database tables. Adding a plan means editing that file and redeploying.

Payment goes through YooKassa. The bot creates a checkout link; YooKassa sends a webhook when the user pays. Because a webhook call and a database write are two separate systems, the service uses a transactional outbox to guarantee nothing gets lost (details in the [Kafka section](#kafka-message-flows)).

After a successful payment, `subscription-service` activates the subscription and fires an event. `loyalty-service` listens to the same event and credits cashback points. `tg-bot-service` listens for the activation event and sends the user a Telegram message.

Users can spend cashback points on subscriptions. If they have enough to cover the full price, no YooKassa payment is created at all. If they only have part of the price, the points become a discount and YooKassa handles the rest.

---

## gRPC call chains

All gRPC calls go through `GrpcRetry`, which retries with exponential backoff when a service responds with `UNAVAILABLE`. Default: 3 attempts, starting at 200 ms, multiplier 3.0.

### Sending a chat message

The user sends a text. `tg-bot-service` forwards it to `chat-service`, which asks `subscription-service` to check and debit access atomically before calling OpenAI. If the OpenAI call fails after the debit, `chat-service` issues a refund.

```
tg-bot-service
  → chat-service.ProcessMessage
      → subscription-service.CheckAccess    (deducts requests_remaining in a single UPDATE)
      → OpenAI API
      → subscription-service.RefundAccess   (only if OpenAI fails after debit)
```

`CheckAccess` runs `UPDATE user_request_balance SET requests_remaining = requests_remaining - cost WHERE requests_remaining >= cost`. Zero rows updated means the user is over quota.

### Checking request balance

```
tg-bot-service → subscription-service.GetBalance
```

Returns free quota and paid balance broken down per AI provider. The bot formats this and sends it back to the user.

### Changing AI model

Switching provider clears chat history, because context from one model is useless for another.

```
tg-bot-service
  → subscription-service.SetModelPreference
  → chat-service.ClearHistory
```

### Viewing subscription plans

```
tg-bot-service → subscription-service.GetPlans
```

`subscription-service` reads active plans and their request allocations from the database and returns them. Plans live entirely in the `subscription-service` PostgreSQL database (`subscription_plan` + `subscription_plan_allocation`).

### Paying with card

```
tg-bot-service
  → payment-service.CreatePayment
      → YooKassa API              (creates the payment, returns a checkout URL)
  ← bot sends the URL to the user
```

From this point the flow is async. YooKassa calls the webhook after the user pays. See [Kafka message flows](#kafka-message-flows).

### Paying with bonus points, partial discount

The user has points but fewer than the plan costs. The bot applies all available points as a discount and creates a payment for the remainder.

```
tg-bot-service
  → loyalty-service.GetBalance
  → subscription-service.GetPlanInfo       (to know the price)
  → payment-service.CreatePayment(bonusPointsToApply = balance)
      → YooKassa API
  ← bot sends the checkout URL
```

After payment succeeds, `loyalty-service` earns cashback on the discounted amount only.

### Paying entirely with bonus points

The user has enough points to cover the full price. No YooKassa payment is created.

```
tg-bot-service
  → loyalty-service.SpendPoints
      → subscription-service.ActivateSubscription   (internal gRPC, no Kafka round-trip)
  ← bot confirms activation immediately
```

`loyalty-service` calls `subscription-service` directly so the user gets confirmation without waiting for an async event. The same idempotency key passes through to protect against retries.

### Checking loyalty balance

```
tg-bot-service → loyalty-service.GetBalance
```

---

## Kafka message flows

Two topics carry all async domain events.

```
Topic: payment_succeeded
  Published by:  payment-service (via transactional outbox)
  Consumed by:
    subscription-service → activates subscription, then publishes to subscription_activated
    loyalty-service      → credits cashback points (floor(amount * cashback_rate))

Topic: subscription_activated
  Published by:  subscription-service
  Consumed by:
    tg-bot-service → sends "Subscription activated" message to user in Telegram
```

Kafka runs as a 3-broker KRaft cluster. Topics are auto-created with 3 partitions and replication factor 3. A Kafka UI is available at `http://localhost:8090` when running with Docker Compose.

### Why the transactional outbox?

When YooKassa confirms a payment, `payment-service` has to do two things: update the payment status in PostgreSQL and publish an event to Kafka. Both need to succeed together. If the service updates the DB then crashes before sending to Kafka, the payment is marked as succeeded but nobody gets notified.

The outbox pattern solves this by writing both in one database transaction:

1. `handlePaymentWebhook` updates `payment.status = SUCCEEDED` and inserts a row into `outbox_payment_event` — same transaction, same connection.
2. A scheduled job (`YooKassaPaymentOutboxPublisher`) runs every 5 seconds, reads up to 100 unpublished rows, sends each to Kafka, then marks them published.

If the scheduler crashes mid-batch, unpublished rows just stay there and get picked up on the next tick. Because consumers are idempotent, duplicate delivery is safe: `UNIQUE(payment_id, provider)` on `user_subscription` and `UNIQUE(payment_id, type)` on `loyalty_transaction` make re-processing a no-op.

### Full async flow after card payment

```
YooKassa webhook → payment-service
  [DB transaction]
    UPDATE payment SET status = SUCCEEDED
    INSERT outbox_payment_event

  [Scheduler, every 5s]
    → Kafka topic: payment_succeeded

  subscription-service (consumer)
    INSERT user_subscription         (idempotent: UNIQUE payment_id + provider)
    UPDATE user_request_balance += earned_requests
    → Kafka topic: subscription_activated

  loyalty-service (consumer)
    INSERT loyalty_transaction       (idempotent: UNIQUE payment_id + type)
    UPDATE user_loyalty_balance += floor(amount * cashback_rate)

  tg-bot-service (consumer)
    → Telegram: "Subscription activated! Plan: X, N requests credited"
```

---

## Database schemas

### subscription (PostgreSQL)

```sql
subscription_user         -- registered users: user_id, blocked, role
user_subscription         -- immutable purchase log: plan_id, provider, earned_requests, payment_id
user_request_balance      -- mutable paid balance: (user_id, provider) → requests_remaining
user_free_quota           -- free-tier quota per user and provider, with reset timestamp
user_model_preference     -- preferred model per user
```

`user_subscription` is append-only. Requests are credited to `user_request_balance` at activation time. Access control reads only `requests_remaining`.

### payment (PostgreSQL)

```sql
payment                   -- UUID id, user_id, plan_id, yookassa_payment_id, amount, status
outbox_payment_event      -- event_type, payload (JSON), published_at (NULL = not yet published)
```

### loyalty (PostgreSQL)

```sql
user_loyalty_balance      -- user_id → points
loyalty_transaction       -- user_id, amount, type (EARNED/SPENT), payment_id, created_at
```

---

## Configuration reference

### Environment variables

Copy `.env.example` to `.env` and fill in the required values.

| Variable | Required | Default | Notes |
|---|---|---|---|
| `TELEGRAM_BOT_TOKEN` | yes | — | |
| `OPENAI_API_KEY` | yes | — | |
| `REDIS_PASSWORD` | yes | — | Must be non-empty for Docker Compose |
| `SUBSCRIPTION_DB_PASSWORD` | yes | — | |
| `PAYMENT_DB_PASSWORD` | yes | — | |
| `LOYALTY_DB_PASSWORD` | yes | — | |
| `YOOKASSA_SHOP_ID` | yes | — | |
| `YOOKASSA_SECRET_KEY` | yes | — | |
| `YOOKASSA_RETURN_URL` | yes | — | URL the user lands on after paying |
| `LOYALTY_CASHBACK_RATE` | no | `0.08` | Fraction of payment credited as points (0.08 = 8%) |
| `SUBSCRIPTION_FREE_QUOTA` | no | `10` | Free requests per user per reset period |
| `SUBSCRIPTION_FREE_QUOTA_RESET_PERIOD` | no | `7d` | Reset period, e.g. `7d`, `24h` |
| `OPENAI_DEFAULT_MODEL` | no | `gpt-4o-mini` | Default model for new users |

### Subscription plans (database)

Plans live in two tables in the `subscription` database: `subscription_plan` and `subscription_plan_allocation`. Each plan has a stable string ID, a display name, a price, a currency, and one or more allocations that define how many requests the user gets per provider when they buy the plan.

### Model costs and providers (application.yml)

Model costs and the mapping from model ID to provider are also in `subscription-service/src/main/resources/application.yml`.

```yaml
subscription:
  model-providers:
    gpt-4o-mini: openai
    gpt-4o: openai
    gpt-4-turbo: openai
  model-costs:
    gpt-4o-mini: 1
    gpt-4o: 2
    gpt-4-turbo: 3
```

---

## How to add a subscription plan

Plans are stored in the `subscription` database, so no redeploy is needed — add rows, and the change takes effect immediately.

Insert the plan and its allocations in one transaction:

```sql
BEGIN;

INSERT INTO subscription_plan (plan_id, provider, display_name, price, currency)
VALUES ('openai-ultra', 'openai', 'Ultra - 1000 requests for OpenAI', 999.00, 'RUB');

INSERT INTO subscription_plan_allocation (plan_id, provider, requests)
VALUES ('openai-ultra', 'openai', 1000);

COMMIT;
```

The `plan_id` (`openai-ultra`) is stored in `user_subscription.plan_id` and `payment.plan_id` for every purchase of this plan. Pick something stable — renaming it later would orphan existing records.

`payment-service` fetches the price at payment time via `subscription-service.GetPlanInfo` over gRPC, so there is nothing else to update. The plan shows up in the bot's Premium screen on the next `GetPlans` call.

To deactivate a plan without deleting it (preserving historical records):

```sql
UPDATE subscription_plan SET active = false WHERE plan_id = 'openai-ultra';
```

Deactivated plans are invisible to users but their `plan_id` stays intact in `user_subscription` and `payment` records.

### Multi-provider plans

A plan can grant requests across multiple providers at once. The `subscription_plan.provider` field is just a UI label — it controls which provider menu the plan appears under in the bot. The actual grants come from `subscription_plan_allocation`, which can have as many rows as you need.

When a user buys a combo plan, activation creates one row in `user_subscription` per allocation and credits each provider's balance separately in `user_request_balance`:

```
(user_id=1, provider=openai)     → requests_remaining += 100
(user_id=1, provider=anthropic)  → requests_remaining += 50
```

Access checks are provider-scoped. Sending a GPT-4o message debits the OpenAI balance. Sending a Claude message debits the Anthropic balance. The two are completely independent.

To add a combo plan:

```sql
BEGIN;

INSERT INTO subscription_plan (plan_id, provider, display_name, price, currency)
VALUES ('combo-basic', 'openai', 'Combo - 100 OpenAI + 50 Anthropic', 299.00, 'RUB');

INSERT INTO subscription_plan_allocation (plan_id, provider, requests)
VALUES ('combo-basic', 'openai',    100),
       ('combo-basic', 'anthropic',  50);

COMMIT;
```

The activation pipeline and access checks need no code changes — they already work per-provider. The only thing that currently limits multi-provider plans in the UI is the `PremiumProvider` enum in `tg-bot-service`, which only lists `OPENAI`. Adding Anthropic there is one line.

---

## How to add a new AI model

Adding a model means it becomes selectable in the bot's model menu and gets a request cost.

**Step 1.** Add the model to `subscription-service/src/main/resources/application.yml`:

```yaml
subscription:
  model-providers:
    gpt-4o-mini: openai
    gpt-4o: openai
    gpt-4-turbo: openai
    o1-mini: openai          # new

  model-costs:
    gpt-4o-mini: 1
    gpt-4o: 2
    gpt-4-turbo: 3
    o1-mini: 2               # new
```

**Step 2.** Add the model to the `OPENAI` entry in `tg-bot-service/src/main/kotlin/com/xeno/subpilot/tgbot/ux/AiProvider.kt`:

```kotlin
OPENAI(
    "֎ OpenAI",
    "openai",
    listOf(
        AiModel("gpt-4o", "GPT-4o"),
        AiModel("gpt-4o-mini", "GPT-4o mini"),
        AiModel("gpt-4-turbo", "GPT-4 Turbo"),
        AiModel("o1-mini", "o1 mini"),    // new
    ),
),
```

The `AiModel` constructor takes the model ID (passed to OpenAI) and the display name (shown in the bot). After restart, the model appears in the bot's model selection menu.

---

## How to add a new AI provider

Adding a provider that is separate from OpenAI — say, a self-hosted model — requires changes in several places. The pattern is consistent: add the provider key everywhere the existing `openai` key appears.

**subscription-service** — add models and their costs:

```yaml
subscription:
  model-providers:
    gpt-4o-mini: openai
    custom-7b: custom          # new model → new provider key

  model-costs:
    gpt-4o-mini: 1
    custom-7b: 1               # new
```

Add a plan that allocates requests to the new provider:

```yaml
subscription:
  plans:
    custom-basic:
      provider: custom
      display-name: "Custom Basic - 200 requests"
      price: 99.00
      currency: RUB
      allocations:
        - provider: custom
          requests: 200
```

**tg-bot-service** — register the provider in `AiProvider.kt` so users can select it and models under it:

```kotlin
enum class AiProvider(
    val displayName: String,
    val providerKey: String,
    val models: List<AiModel>,
) {
    OPENAI("֎ OpenAI", "openai", listOf(...)),
    CUSTOM("⚡ Custom", "custom", listOf(         // new
        AiModel("custom-7b", "Custom 7B"),
    )),
}
```

Also add it to `PremiumProvider.kt` so it appears in the Premium subscription menu:

```kotlin
enum class PremiumProvider(val displayName: String, val planProviderKey: String) {
    OPENAI("֎ OpenAI", "openai"),
    CUSTOM("⚡ Custom", "custom"),    // new
}
```

**chat-service** — implement the actual API call. The service currently only knows how to call OpenAI. You would add a new client class for the new provider and update the dispatch logic in `ChatServiceGrpc` to route based on the model's provider.

---

## Local run

```bash
cp .env.example .env
# fill in TELEGRAM_BOT_TOKEN, OPENAI_API_KEY, REDIS_PASSWORD,
# SUBSCRIPTION_DB_PASSWORD, PAYMENT_DB_PASSWORD, LOYALTY_DB_PASSWORD,
# YOOKASSA_SHOP_ID, YOOKASSA_SECRET_KEY, YOOKASSA_RETURN_URL
docker compose up --build
```

To run just the bot without needing an OpenAI key:

```bash
./run.sh
```

Logs are written to `<service-name>.log` files in the project root.

Kafka UI (browse topics and messages): `http://localhost:8090`

### YooKassa webhook in local development

YooKassa needs a public HTTPS URL to send webhook events. For local testing, expose `payment-service` with a tunnel (e.g. `ngrok http 8084`) and set `YOOKASSA_RETURN_URL` to the tunnel URL. Configure the webhook URL in your YooKassa dashboard to point to `https://<tunnel>/webhook/payment`.

---

## Build and tests

```bash
# Build, skip tests and lint
./gradlew build -x test -x ktlintCheck

# Run all tests
./gradlew test

# Run tests for one service
./gradlew :tg-bot-service:test

# Run a single test class
./gradlew :tg-bot-service:test --tests "com.xeno.subpilot.tgbot.SomeTest"

# Lint check
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat
```

Tests are split into three folders per service:

- `unittests/` — pure unit tests with MockK, no I/O
- `integrationtests/` — Spring context or WireMock (for external HTTP calls like Telegram, OpenAI, YooKassa)
- `testcontainers/` — tests that need real PostgreSQL, Redis, or Kafka

---

## License

Apache-2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
