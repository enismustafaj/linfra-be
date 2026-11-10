# AI provisioning assistant

A user describes what they need in chat. The LLM turns that into typed requirements, the system picks the VM, and
the user confirms. Only a confirmed draft becomes an intent.

## API

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/v1/conversations` | `{id}` |
| `POST` | `/api/v1/conversations/{id}/messages` `{"text": "..."}` | The assistant's reply, with `draft` if it proposed one |
| `POST` | `/api/v1/conversations/{id}/drafts/{messageId}/confirm` | The `PROPOSED` intent |
| `GET` | `/api/v1/conversations/{id}` | Messages plus linked intents and their states |

## The LLM is untrusted

- It replies with one JSON object: `action` (`ASK`, `PROPOSE` or `REJECT`), `message`, and `requirements`.
  OpenAI strict structured output enforces the schema.
- It never sets provider, instance type, name, tags, credentials, tenant or message roles.
- Malformed output, out-of-range values or an unrankable request produce a plain reply with no draft.
- The worst a prompt injection can do is produce a schema-valid draft. The ranker, policy and the user's confirmation
  still sit between it and the cloud.
- A second call explains the ranker's pick and cannot change it. If that call fails, the draft keeps a fixed explanation.

## Guarantees

- The request body has text only; the server sets roles.
- Messages are 1–2000 characters, checked before any LLM call.
- No DB transaction is open during an LLM call (30s timeout, 2 attempts). If the call fails, the API returns `503` and
  the user's message is kept.
- Only the latest draft can be confirmed. Confirming twice returns the same intent; `source_message_id` is unique.
- Every intent links back to its conversation and the exact draft the user confirmed.

## Classes

`ConversationService` (create, own, read) · `ChatService` (one exchange) · `DraftBuilder` (LLM reply → draft) ·
`DraftConfirmationService` (draft → intent) · `AgentClient` (the only LLM caller) · `AgentPrompts`
