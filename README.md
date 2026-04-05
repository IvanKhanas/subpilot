# SubPilot

Kotlin/Spring Boot microservice backend for a Telegram AI chatbot with usage limits, paid subscriptions, and payment processing.

## What works right now

### Telegram bot (`tg-bot-service`)

- `/start` — welcome screen with navigation menu
- `/help` — help screen
- `/menu` — main menu
- AI chat — send any text message, get a response from the selected model
- **Provider & model selection** — inline menu to pick provider (OpenAI) and model (GPT-4o, GPT-4o mini, GPT-4 Turbo)
- Navigation stack with **Back** button (session stored in Redis, 20-min TTL)
- Waiting indicator while AI is processing
- OpenAI Markdown → Telegram HTML conversion

### AI chat (`chat-service`)

- gRPC server (`ProcessMessage`, `SetModel`)
- OpenAI integration
- Per-user model preference persisted in PostgreSQL
- Conversation history in Redis — last 20 messages, 20-min sliding TTL

### Not yet implemented

- `subscription-service` — free quota (10 req/user), subscription plans (scaffold only)
- `payment-service` — YooKassa payments (scaffold only)
- `notification-service` — payment receipts via Kafka (scaffold only)

## Architecture

```
Telegram ──► tg-bot-service (8081)
                  │  gRPC
                  ▼
             chat-service (8082 / gRPC 9090)
                  │  PostgreSQL  │  Redis
```

Inter-service: gRPC for synchronous calls, Kafka planned for async payment events.

## Tech stack

- Kotlin 2.2, Spring Boot 4.0, Java 21
- gRPC (grpc-spring-boot-starter)
- PostgreSQL + Liquibase (chat-service)
- Redis (chat history, navigation state)
- Gradle multi-module with convention plugins

## Local development

```bash
cp .env.example .env
# fill in TELEGRAM_BOT_TOKEN and OPENAI_API_KEY

docker compose up --build
```

## Build & test

```bash
# Build (skip tests)
./gradlew build -x test -x ktlintCheck

# Run tests
./gradlew test

# Lint / auto-format
./gradlew ktlintCheck
./gradlew ktlintFormat
```

## License

Apache-2.0 — see `LICENSE` and `NOTICE`.
