# AIFA Backend — Phase 1 Foundation

Personal finance tracker API for Rwanda (no AI in Phase 1).

## Stack

- Java 21, Spring Boot 3.3
- PostgreSQL 16 + Flyway
- Redis 7 (configured, minimal Phase 1 use)
- JWT authentication (access 15 min, refresh 7 days)

## Prerequisites

- Java 21+
- Maven 3.9+ (or use the Maven Wrapper once generated)
- Docker & Docker Compose

## Quick start

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts:
- PostgreSQL on `localhost:5432` (db: `aifa`, user/pass: `aifa` / `aifa_dev`)
- Redis on `localhost:6379`

### 2. Run the API

```bash
cd aifa-api
mvn spring-boot:run
```

The API listens on `http://localhost:8080` with profile `dev`.

### 3. Verify

```bash
cd aifa-api
mvn verify
```

Integration tests (require Docker):

```bash
mvn verify -Pintegration
```

## API overview (`/api/v1`)

| Area | Endpoints |
|------|-----------|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `DELETE /auth/logout` |
| Wallets | `GET /wallets`, `POST /wallets`, `PATCH /wallets/{id}` |
| Transactions | `GET /transactions`, `POST /transactions`, `DELETE /transactions/{id}`, `PATCH /transactions/{id}/category` |
| Categories | `GET /categories`, `POST /categories` |
| Budgets | `GET /budgets/current`, `POST /budgets`, `PATCH /budgets/{id}` |
| Goals | `GET /goals`, `POST /goals`, `PATCH /goals/{id}`, `POST /goals/{id}/contribute` |
| Import | `POST /import/sms`, `POST /import/sms/confirm` |
| Insights | `GET /insights/spending-analysis`, `GET /insights/health-score`, `POST /insights/affordability`, `GET /insights/recommendations` |
| Dashboard | `GET /dashboard/summary` |

All endpoints except auth register/login/refresh require `Authorization: Bearer <access_token>`.

Logout expects `X-Refresh-Token` header with the refresh token.

## Example flow

```bash
# Register
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"me@example.com","password":"password123"}'

# Create primary wallet
curl -s -X POST http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"MTN MoMo","type":"mobile_money","primary":true}'

# Add income transaction
curl -s -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"walletId":"...","amountRwf":100000,"type":"income","transactionAt":"2025-06-12T10:00:00Z"}'
```

## Configuration

| Variable | Default (dev) | Description |
|----------|---------------|-------------|
| `AIFA_JWT_SECRET` | dev placeholder in `application.yaml` | HMAC secret (min 256 bits) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/aifa` | Database URL |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |

## Project structure

```
aifa-api/src/main/java/com/aifa/
  shared/          # security, money, exceptions, config
  modules/
    iam/           # auth
    ledger/        # wallets, transactions, categories
    planning/      # budgets, goals
    importing/     # MoMo SMS import
    dashboard/     # summary
    insights/      # health score, spending analysis, affordability, recommendations
```

## Phase 1 notes

- Money stored as `BIGINT` RWF integers (no decimals)
- Wallet balance is materialized from the transaction ledger atomically
- System categories seeded: food, transport, rent, utilities, entertainment, health, savings, other
- MoMo SMS parser version: `mtn_momo_v1`
- Errors return RFC 7807 `ProblemDetail`

## Follow-up (Phase 2+)

- Financial Health Score
- Affordability engine
- AI orchestrator (Python)
- Redis session context & rate limiting
- Merchant lookup enrichment
- PATCH wallet/budget endpoints
