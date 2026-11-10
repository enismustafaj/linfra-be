# Reconciliation

The provider is the source of truth. Every minute the reconciler compares what Docker has against what Linfra
recorded and records the differences as [findings](remediation.md). It never acts on the provider itself: users
resolve findings with the actions each one offers.

## Findings

| Finding | Meaning | Action |
|---|---|---|
| Seen | Recorded and present | `last_seen_at` updated; a `MISSING` resource goes back to `ACTIVE` |
| Missing | Recorded `ACTIVE`, but the provider no longer has it | Marked `MISSING` |
| Drifted | Present but differs from what was applied (instance type, region, vCPU, memory, dedicated CPU) | Reported with expected and actual values |
| Orphan | Linfra-labeled, no record, and no in-flight intent | Reported |
| Duplicate | More than one resource with the same `intent_id` | Reported |
| Stuck | `APPROVED` or `APPLYING` with no live lease for longer than `stuck-after` | Reported |

**Why the reconciler never acts on its own:**
- **No adopting:** the worker's `find()` already adopts. One writer for outcomes means no race between two
  recovery paths.
- **No deleting:** an unexpected resource may need a human's attention; deleting it automatically is the kind of
  action Linfra exists to prevent.
- **No reverting:** a revert is an apply, and applies go through intents and policy.

## Avoiding false positives

Linfra's records are read **before** the provider is listed. A resource a worker creates in between belongs to an
intent that is still in flight in that snapshot, so it can't be reported as an orphan.

## Stuck vs in progress

An `APPLYING` intent looks the same whether a worker is in a long apply or died. The difference is the outbox job's
lease: heartbeats extend the lease, never the intent. `InFlightIntentRepository` joins each in-flight intent with
its job in one query and returns whether a live lease exists and since when a worker should have been on it.

## Output

- Findings, with actions to resolve them: see [remediation.md](remediation.md).
- Metrics at `/actuator/metrics` (operator view, all tenants): `linfra.reconciler.orphans`, `.missing`, `.drifted`,
  `.duplicates`, `.stuck.intents`, `.oldest.unfinished.intent.seconds`, `.last.run.epoch.seconds`.

## Config

`linfra.reconciler.*`: `enabled`, `interval` (1 min), `stuck-after` (2 min, two leases).

## Classes

`Reconciler` (snapshot, compare, record) · `InFlightIntentRepository` (intents joined with leases) · `InventoryComparer` (pure comparison) · `ReconcilerMetrics` · `ResourceFinder.listManaged()` (normalises provider data into catalog terms)
