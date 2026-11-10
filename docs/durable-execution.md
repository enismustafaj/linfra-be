# Durable execution

One approved intent creates at most one resource, and every intent ends in a named state, even if a worker dies mid-apply.

## How "at most one" holds

The database and the cloud can't share a transaction, so the guarantee comes from layers, each covering a gap the
previous one leaves:

| # | Layer | Stops |
|---|---|---|
| 1 | `outbox_jobs.intent_id` unique, inserted with the approval | Two jobs for one intent |
| 2 | `SKIP LOCKED` claim, lease, fencing token | Two workers on one job; a replaced worker recording anything. Protects the database only |
| 3 | `find(intent_id)` by label before every apply | A second create after a worker died between apply and recording |
| 4 | One pg-backend state workspace per intent, locked during apply | Two concurrent applies for one intent |
| 5 | Name `linfra-<intent_id>`; Docker rejects duplicate names | A second create even if state was lost and `find()` ran too early |
| 6 | `resources.intent_id` and `(provider, cloud_id)` unique | Two records for one intent; one container linked to two intents |
| 7 | `find()` refuses when more than one resource carries the label | Adopting anything once the guarantee is broken; intent → `NEEDS_ATTENTION` |

Layer 5 depends on the provider: Hetzner also rejects duplicate names, AWS doesn't (an AWS adapter would add EC2's
`ClientToken`).

## Intent states

```
PROPOSED → PLANNED → APPROVED → APPLYING → SUCCEEDED
```

The exits are `REJECTED`, `EXPIRED` (re-plan and re-approve), `FAILED` (nothing created) and `NEEDS_ATTENTION`
(outcome uncertain; a human decides).

- `Intent.moveTo` rejects illegal transitions.
- `@Version` stops two concurrent transitions on the same intent from both committing.

## Approval and outbox

`IntentService.approve` checks the plan hash, moves the intent to `APPROVED` and inserts the outbox job in one
transaction. `outbox_jobs.intent_id` is unique, so an intent can never be enqueued twice.

## Workers

`WorkerPool` runs `linfra.worker.threads` threads (default 4) per process. Each thread has its own worker id
(`<process id>-<n>`) and handles one job at a time. Threads and processes coordinate only through the database.
No in-memory locking is involved.

1. **Claim:** select due `PENDING` jobs, or `LEASED` jobs whose lease ran out, with `FOR UPDATE SKIP LOCKED`.
   In the same transaction: `fencing_token += 1`, the intent moves to `APPLYING`, and a `runs` row is written.
   The record exists before any cloud call.
2. **Apply:** `find(intent) ?: apply(intent)`. `LeaseHeartbeat` extends the lease while the call runs.
3. **Record:** `succeed` and `fail` lock the job row and check the fencing token. A worker whose lease was taken over
   gets `LostLeaseException` and changes nothing. `succeed` inserts the `resources` row and moves the intent to
   `SUCCEEDED` in one transaction.

**Shutdown:** stop claiming, then wait up to one lease for jobs in flight. Threads still busy after that are abandoned;
their leases expire and other workers recover the jobs, the same path as after a crash.

## Resources

A resource outlives its intent: it gets reconciled, can drift, and gets destroyed. So it has its own row.

- `intent_id` is unique, so one intent creates at most one resource.
- `(provider, cloud_id)` is unique. If `find()` ever returned a server that already belongs to another intent, the
  insert fails, the job goes down the failure path, and the intent ends in `NEEDS_ATTENTION` instead of being linked
  to someone else's server.
- `provider`, `region` and `instance_type` record what was applied: the reconciler compares the live cloud against them.

## Recovery

| Situation | What happens |
|---|---|
| Process dies | Every thread's lease expires; workers in other processes take over |
| Worker dies after claiming | Lease expires; another worker claims with a higher token |
| Worker dies mid-apply | The new worker waits on the state lock or adopts via `find()`; a duplicate create fails on the name |
| Worker dies after apply, before recording | The new worker's `find()` adopts the container (tested on real Docker) |
| Apply throws | Retry with exponential backoff (5 min cap); after `max-attempts` → `NEEDS_ATTENTION` |
| Approval expired before start | Intent → `EXPIRED`, nothing applied |

## Classes

`OutboxStore` (all job state changes) · `OutboxWorker` (one job) · `WorkerPool` (threads) · `LeaseHeartbeat` ·
`Provisioner` (`find()` and `apply()`)

## Not proven yet

The layers are tested individually (including adopting a crashed worker's container on real Docker), not under
real process kills. A killed worker's `tofu` child process can outlive it; layers 4 and 5 are meant to cover that.
Anything that slips through shows up as a [reconciliation](reconciliation.md) finding a user can resolve.
