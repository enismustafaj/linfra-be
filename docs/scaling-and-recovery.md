# Scaling and recovery

How Linfra runs as several instances, and what happens to work in progress when an instance crashes, a new one
starts, or a deployment replaces them mid-run.

## The model: stateless instances, all coordination in Postgres

Every instance is identical: the HTTP API, a pool of worker threads, and the reconciler. No instance holds state the
others depend on. Requests, jobs, leases and OpenTofu state all live in Postgres, so any instance can serve any
request and any worker can pick up any job.

```
            load balancer
          ┌──────┼──────┐
     instance A  B  C        each: HTTP API + WorkerPool (N threads) + Reconciler
          └──────┼──────┘
              Postgres        intents, outbox_jobs (leases, fencing tokens), runs, resources, findings,
                              policies, OpenTofu state (one workspace per intent, with its lock)
```

There's no leader, no service discovery and no in-memory coordination between instances or threads.

## Scaling

| To scale | Do | Why it's safe |
|---|---|---|
| API traffic | Add instances behind the load balancer | Requests carry their tenant (API key) and read everything from Postgres |
| Provisioning throughput | Add instances, or raise `WORKER_THREADS` (default 4) | Workers claim jobs with `FOR UPDATE SKIP LOCKED`: two workers never get the same job, and none waits on a row another is claiming |

Each worker thread handles one job at a time, so in-flight applies = instances × threads.

**Ceilings to watch:**

- **Postgres connections.** Each instance has a pool of 10 (HikariCP default). Workers only hold a connection for
  short transactions (claim, heartbeat, record), never during an apply. But every running `tofu apply` opens its
  own Postgres connection for state and locking. Size Postgres `max_connections` for
  `instances × (pool + threads)`.
- **The provider.** With the local Docker provider, every instance must reach the same Docker daemon
  (`DOCKER_HOST`). A real cloud has no such constraint.
- **The reconciler runs on every instance.** Each lists the provider and writes findings every minute. That's
  correct: one open finding per subject is enforced by a unique index, and findings are versioned, so concurrent runs
  can't create duplicates; a conflicting run fails and the next one catches up. But it's repeated work. With many
  instances, add a leader lock (`pg_try_advisory_lock`) so only one instance reconciles per interval.

## Leases, heartbeats and fencing

| Setting | Value | Role |
|---|---|---|
| `linfra.worker.lease` | 30 s | How long a claim is valid without a heartbeat |
| Heartbeat | every 10 s (lease / 3) | Extends the lease while a long apply runs |
| `linfra.worker.poll-interval` | 1 s | How often an idle thread looks for work |
| `linfra.worker.max-attempts` | 5 | Failed attempts before `NEEDS_ATTENTION` |
| Retry backoff | 2, 4, 8, 16 s … capped at 5 min | Between failed attempts |
| `linfra.policy.approval-ttl` | 1 h | An approved intent no worker started in time expires |
| `linfra.reconciler.stuck-after` | 2 min | An in-flight intent with no live lease this long is reported stuck |

A **lease** says "this worker is on this job until T". A worker that stops heartbeating (crashed, frozen,
partitioned) loses it after 30 s, and the job becomes claimable again.

A **fencing token** is incremented on every claim. Every write a worker makes (heartbeat, success, failure) checks that
its token is still the current one. A worker that was replaced, for example one that froze and came back, gets
`LostLeaseException` and can't change anything.

Fencing protects the database only; the provider never sees the token. What keeps the provider side to at most one
resource is: `find(intent_id)` before every apply, the per-intent OpenTofu state lock, and the deterministic name
`linfra-<intent_id>`. See [durable-execution.md](durable-execution.md).

## An instance crashes

Killed, out of memory, or its machine is gone.

| What it was doing | What happens |
|---|---|
| Serving an API call | The call is lost; the client retries. Confirming a draft and approving are single transactions: either all of it happened or none. Confirming twice returns the same intent; approving twice is a `409` |
| Chat: waiting for the LLM | The user's message was saved before the call; the reply is lost. The user resends; LLM calls have no side effects |
| A claimed job, before the apply | Heartbeats stop; after 30 s another instance claims it with a higher fencing token, finds nothing, and applies |
| A job mid-apply | Same takeover. The new worker calls `find()` first: if the container already exists, it's adopted, not created again |
| A job whose apply finished but wasn't recorded | The new worker's `find()` adopts the container. Tested on real Docker |
| Recording the result | Recording is one transaction (resource row + `SUCCEEDED`); a crash means it didn't happen, and the next worker adopts |

**If only the JVM dies and its `tofu` process survives** (e.g. the out-of-memory killer took the JVM only), that
apply keeps running and holds the intent's state lock. The next worker's `tofu apply` fails on the lock at once
(no `-lock-timeout` is set) and the attempt is retried with backoff. Once the orphaned apply finishes, the retry's
`find()` adopts its container. This costs retry attempts, not correctness; setting `-lock-timeout` would make the new
worker wait instead.

## A new instance starts

Nothing to register. It:

1. Runs Flyway, which takes a database lock, so instances starting together don't migrate twice.
2. Starts its worker threads, each with its own id (`<random process id>-<n>`).
3. Claims due `PENDING` jobs and `LEASED` jobs whose lease ran out: work from crashed instances is picked up here.
4. Starts reconciling a minute later.

## A deployment in the middle of a run

A rolling deployment stops old instances and starts new ones while jobs are running.

**Stopping an old instance** (`SIGTERM`):

1. The HTTP server stops accepting requests and finishes the ones in flight (Spring Boot graceful shutdown).
2. The worker pool stops claiming and gives in-flight jobs up to one lease (30 s) to finish and record.
3. Jobs still running after that are left behind. Their leases expire and a new instance takes them over: the same
   path as a crash, with `find()` first.

The orchestrator's grace period should be longer than the lease, so jobs that can finish do: Kubernetes'
default of 30 s is borderline, so 45 s or more is better. If it's shorter, the instance is killed, and the crash path
applies.

**Old and new versions running side by side:**

- **Changed module, catalog or provider version.** An intent approved under the old version is checked by the new
  worker: it recompiles the workspace, the hash no longer matches the approved plan, nothing is applied, and the
  intent becomes `EXPIRED`, to be re-approved under the new version. See [plan-hash.md](plan-hash.md).
- **Database migrations** must work for both versions at once while old instances still run: add columns and tables
  first, remove old ones in a later release.

**Every instance down for a while:** jobs wait in the outbox. Approvals older than an hour expire when finally claimed
instead of applying something approved long ago.

## Postgres unavailable

Worker threads catch the error, wait and retry; they don't die. Heartbeats fail, so leases expire. When Postgres comes
back, jobs are claimed again, and fencing stops any worker that lost its lease from writing. OpenTofu state is in the
same database, so applies fail during the outage and are retried.

## What's tested and what isn't

**Tested:** lease takeover and fencing, concurrent claims (`SKIP LOCKED`), several threads draining the outbox with
exactly one apply per intent, adopting a crashed worker's container on real Docker, the plan-hash check, and graceful
recovery paths in isolation.

**Not tested:** real process kills, real rolling deployments, and several instances running side by side. The
mechanisms above are designed for them; anything that slips through shows up as a reconciliation finding (stuck,
orphan or duplicate) that a user can resolve.
