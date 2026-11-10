# Resources

A resource is what an intent created. It has its own row because it outlives the intent: it gets reconciled, can
drift and gets destroyed.

## API

| Method | Path | Result |
|---|---|---|
| `GET` | `/api/v1/resources` | The tenant's resources, newest first |
| `GET` | `/api/v1/resources/{id}` | One resource; another tenant's is `404` |

Each resource has: `intentId`, `provider`, `region`, `instanceType`, `cloudId` (the container id), `outputs` (e.g.
`ipv4`), `status` (`ACTIVE`, `DESTROYED`, `MISSING`), `createdAt` and `lastSeenAt` (set by the [reconciler](reconciliation.md)).

## Guarantees

- Written in the same transaction that marks the intent `SUCCEEDED`, so the two never disagree.
- `intent_id` is unique: one intent, at most one resource.
- `(provider, cloud_id)` is unique: a container is never linked to two intents.
- `provider`, `region` and `instance_type` record what was applied, so the reconciler can compare the live provider
  against them.
