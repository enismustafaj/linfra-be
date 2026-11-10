# Remediation

The reconciler finds problems; a user decides what to do about each one. Every finding offers the actions that
make sense for it right now, and the user picks one through the API.

## Findings

Each reconciliation run updates the `findings` table:

| Status | Meaning |
|---|---|
| `OPEN` | Seen in the latest run, waiting for a user. At most one open finding per subject |
| `CLEARED` | No longer observed (e.g. fixed by hand); closed by the reconciler |
| `RESOLVED` | A user took an action; the action, its outcome and the time are kept |

A subject the user dismissed or accepted is not reported again. For drift, the subject includes the actual values,
so accepting "memory 4 GB → 8 GB" doesn't hide a later "→ 16 GB".

## Actions

| Finding | Actions |
|---|---|
| Orphan | `DESTROY` remove the container · `ADOPT` record it as the resource of its intent (only when that intent is `NEEDS_ATTENTION`) · `DISMISS` |
| Duplicate | `KEEP` with a `cloudId`: keep that copy, remove the others, point the record at it |
| Missing | `MARK_DESTROYED` accept it's gone · `DISMISS` |
| Drift | `ACCEPT` the change was intended |
| Stuck | `RETRY_NOW` make the job claimable now · `MARK_NEEDS_ATTENTION` stop retrying (only while `APPLYING`) |

Planned: `RECREATE` for missing resources (a new intent through approval) and `REVERT` for drift (re-apply through
the worker).

## API

| Method | Path | Result |
|---|---|---|
| `GET` | `/api/v1/findings?status=OPEN` | The tenant's findings, each with `availableActions` |
| `GET` | `/api/v1/findings/{id}` | One finding; another tenant's is `404` |
| `POST` | `/api/v1/findings/{id}/actions` `{"action": "KEEP", "cloudId": "..."}` | The resolved finding; an action not on offer is `409` |

Findings about resources without a tenant label belong to nobody, so no tenant sees them; they appear only in the
reconciler metrics.

## How an action runs

Synchronously, in three steps:

1. Check that the finding is `OPEN` and offers the action, computed fresh rather than taken from the list the user saw.
2. Talk to the provider (remove containers, or look the resource up to adopt it) **outside** any transaction.
   Removals are idempotent.
3. Record the outcome in one short transaction that fails if the finding changed meanwhile (`@Version`).

If the app dies between 2 and 3, the finding stays `OPEN`; the next run clears it if the problem is gone.

`NEEDS_ATTENTION` intents can move to `SUCCEEDED` (adopt) or `FAILED` only through these actions: that state means
"a human decides".

## Classes

`FindingSync` (report → findings) · `RemediationOptions` (actions on offer) · `RemediationService` (runs an action) ·
`ResourceRemover` / `DockerContainerRemover` (provider removal)
