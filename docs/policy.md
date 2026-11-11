# Planning and policy

Between "the user confirmed a draft" and "a worker creates it", the intent is planned and checked against its
tenant's policy. Within policy nothing waits for a human; above it, a human approves in the UI.

## Flow

```
confirm draft ──▶ PROPOSED ──plan──▶ PLANNED ──policy──┬─▶ APPROVED + outbox job ──▶ worker ──▶ container
                                                       ├─▶ PLANNED, waiting for a human ──approve──▶ APPROVED + job
                                                       │                                └─reject──▶ REJECTED
                                                       └─▶ REJECTED (hard rule broken)
```

Confirming a draft, planning, the policy decision and (if approved) the outbox job commit in **one transaction**.

**Approving is one step.** Approve works on any intent still waiting for a decision. One that isn't planned yet
(`PROPOSED`, or `EXPIRED` and needing a new plan) is planned and checked first: within policy it's auto-approved, a
hard-rule breach is rejected, and otherwise the click itself is the human approval. All in one transaction.

## Plan

`IntentPlanner` compiles the intent (module plus allow-listed tfvars) and stores the workspace's SHA-256 as
`plan_hash`. The approval is bound to that hash: before applying, the worker recompiles and compares. If the module,
catalog or values changed since approval, nothing is applied and the intent goes to `EXPIRED`, to be re-planned.

## Policy

One row per tenant in `policies`; tenants without one get `linfra.policy.*` defaults.

| Rule | Default | If broken |
|---|---|---|
| Allowed instance types | any | `REJECTED` |
| Allowed regions | any | `REJECTED` |
| Monthly budget: running resources + approved-but-not-created + this one | €200 | `REJECTED` |
| Production needs a human | yes | waits for a human |
| Auto-approve up to, per request | €50/month | waits for a human |

Hard rules (type, region, budget) can't be overridden; a human approval re-checks them, since spend may have changed.
Approved-but-not-created intents count against the budget, and the tenant row is locked during every decision, so
two approvals can't both fit under the same remaining budget.

An approval is valid for `approval-ttl` (1 hour): if no worker started by then, the intent expires.

## API

| Method | Path | For |
|---|---|---|
| `GET` | `/api/v1/policy` | The tenant's effective policy, committed spend, and the catalog's types and regions |
| `PUT` | `/api/v1/policy` | Save it. Types and regions must exist in the catalog; budget > 0; auto-approve limit between 0 and the budget |
| `POST` | `/api/v1/intents/{id}/approve` | `PROPOSED`, `PLANNED` or `EXPIRED` intents: plans first if needed, then auto-approves within policy or records the human approval |
| `POST` | `/api/v1/intents/{id}/reject` | `PROPOSED` or `PLANNED` intents |

`decisionReason` on each intent says why it was rejected, why it waits, or who approved it.

In the web UI, the **Policy** page edits the policy (with committed spend against the budget), and the **Intents**
page has Approve and Reject buttons. A saved policy applies to decisions made from then on; intents already
waiting are checked against it when approved.

## Limitations

- Any API key of a tenant can change its policy; there are no roles yet.
- The plan is the hash of the compiled workspace, not a saved `tofu plan` file. A saved plan would let a reviewer see
  the exact changes; the hash already guarantees that what is applied is what was approved.

## Classes

`IntentPlanner` (plan and decide; one-step approve) · `PolicyService` (policy, committed spend) · `PolicyEvaluator` (pure rules) ·
`IntentService` (approve, human approve/reject)
