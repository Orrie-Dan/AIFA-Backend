# AIFA Chat API

Natural-language interface over the AIFA Financial Engine (Phase 3).

## Base URL

- Local orchestrator: `http://localhost:8000`
- API prefix: `/api/v1`

The Java API (`http://localhost:8080`) remains the source of truth for data. The orchestrator forwards your JWT to it.

## Authentication

Use the same access token as the main API:

```
Authorization: Bearer <accessToken>
```

Obtain tokens via `POST /api/v1/auth/register` or `POST /api/v1/auth/login` on the Java API.

## Chat

### `POST /api/v1/chat`

**Request**

```json
{
  "message": "Can I afford a 1,200,000 RWF laptop by December?"
}
```

**Response**

```json
{
  "reply": "Based on your current savings pattern...",
  "intent": "affordability",
  "engineData": {
    "affordable": true,
    "monthsNeededMinimum": 4,
    "projectedSavingsAtTargetDateRwf": 1500000,
    "confidence": "high"
  },
  "source": "llm"
}
```

`source` values:

| Value | Meaning |
|-------|---------|
| `llm` | OpenAI response (Smart mode), validated |
| `llm_private` | Ollama/Mistral response (Private mode), validated |
| `rule_based` | Template response (LLM unavailable) |
| `fallback` | Template response (LLM failed validation) |
| `clarification` | Missing parameters; no engine call yet |

### Supported intents (v1)

| User asks about… | `intent` value |
|------------------|----------------|
| Affording a purchase | `affordability` |
| Spending patterns | `spending` |
| Financial health score | `health_score` |
| Budget usage | `budget_status` |
| Savings goals | `goals` |
| Recommendations | `recommendations` |
| General / unknown | `general` |

Affordability questions need a **price** and **future target date**. If missing, the orchestrator asks a clarifying question (`source: clarification`) without counting against the daily limit.

## AI mode (Smart / Private)

Read and update on the Java API:

```
GET  /api/v1/users/me
PATCH /api/v1/users/me
```

```json
{ "aiMode": "smart" }
```

```json
{ "aiMode": "private_mode" }
```

| Mode | LLM routing |
|------|-------------|
| `smart` | OpenAI GPT-4o-mini |
| `private_mode` | Ollama Mistral (falls back to rule-based if Ollama unavailable) |

## Rate limiting

- **20 AI queries per user per day** (Redis-backed)
- Returns `429` with RFC 7807-style `ProblemDetail` when exceeded
- Clarifying questions (missing affordability params) are exempt

## Errors

```json
{
  "type": "https://aifa.rw/problems/rate-limit",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Daily AI query limit reached (20/day).",
  "instance": "/api/v1/chat"
}
```

Handle `building_profile` engine states in `engineData.status` — show a “collecting data” message in the UI.

## Health check

```
GET /health
```

## Local development

```bash
# Terminal 1 — infrastructure
docker compose up -d postgres redis ollama

# Terminal 2 — Java API
cd aifa-api
mvn spring-boot:run

# Terminal 3 — orchestrator
cd aifa-orchestrator
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Optional: pull a local model for Private mode:

```bash
docker exec -it aifa-ollama ollama pull mistral
```

## E2E manual test checklist

1. Register and login on Java API; save `accessToken`
2. Create wallet and add 3+ months of income/expense transactions
3. `GET /api/v1/users/me` — confirm `aiMode: smart`
4. `POST /api/v1/chat` with affordability question — verify `engineData.affordable` matches direct `POST /insights/affordability`
5. Ask spending / health score / budget / goals / recommendations questions
6. `PATCH /api/v1/users/me` with `private_mode`; retry chat
7. Send 21+ chat messages — expect `429` on the 21st
8. Ask "Can I afford a laptop?" without price — expect clarification, not 429

## Mobile integration notes

- Store tokens in Keychain/Keystore
- On `401`, refresh token once via Java API, then retry chat
- Display `reply` as chat bubble; use `engineData` for structured cards below the message
- Settings screen: toggle `aiMode` via `PATCH /users/me`
