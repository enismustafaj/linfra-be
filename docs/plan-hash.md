# The plan hash

Every intent is approved for one exact piece of infrastructure code. The plan hash is how Linfra makes sure the worker
applies exactly that, and nothing else.

## What is hashed

When an intent is planned, `TerraformCompiler` builds its workspace: the files OpenTofu will run.

| File | Comes from |
|---|---|
| `versions.tf`, `variables.tf`, `main.tf`, `outputs.tf` | The checked-in module (`docker-vm`) |
| `.terraform.lock.hcl` | The module's pinned provider versions and checksums |
| `terraform.tfvars.json` | The intent's values, all allow-listed: ids, instance type, region, vCPU, memory, CPU model, architecture, disk |

The plan hash is SHA-256 over all of them, in a fixed order:

```
for each file, sorted by name:
    sha256.update("<file name>\n<file content>\n")
plan_hash = hex(sha256)
```

`terraform.tfvars.json` is canonical JSON with sorted keys, and the files are sorted by name, so the same intent
always gives the same hash. Without that, an identical workspace could hash differently and expire for no reason.

## When it's used

```
plan        IntentPlanner compiles the workspace and stores its hash as intents.plan_hash
approve     the approval (by policy or by a human) is recorded for that plan_hash
claim       the worker's claim carries the approved plan_hash
apply       Provisioner recompiles the workspace from the intent and compares hashes, before running tofu
              equal     → tofu apply
              different → nothing applied; intent → EXPIRED with the reason; re-approve
```

The check happens after `find()`. If the resource already exists (a previous worker created it and crashed), it's
adopted without the check: it was created under the earlier, approved apply.

## What it ensures

**What is applied is exactly what was approved.** The approval is for these bytes of OpenTofu code and these
values, not for "the intent" in general. The worker can't apply anything different, whatever changed in between.

It catches any change between approval and apply:

| Change after approval | Why the hash changes |
|---|---|
| A deployment changes the module (e.g. a new label, a different image) | Module file contents differ |
| A deployment bumps a provider version | `versions.tf` and the lock file differ |
| The catalog changes an instance type's size | The compiler reads sizes from the catalog, so `terraform.tfvars.json` differs |
| Someone edits the intent's spec in the database | The values recompiled from the spec differ |

In each case the intent goes to `EXPIRED` instead of applying something nobody approved, and the next approval goes
through policy again with the current code and prices.

This matters most during a rolling deployment: an intent approved under the old version and picked up by a new
instance is expired rather than silently applied with the new module. See
[scaling-and-recovery.md](scaling-and-recovery.md).

## What it doesn't ensure

- **It's not a `tofu plan`.** It fixes Linfra's input to OpenTofu, not OpenTofu's result. It doesn't capture what a
  data source would resolve to at apply time (with a real cloud, e.g. "the latest Ubuntu image") or what the provider
  would decide to change. A saved plan file would; that's the next step if plans need human review.
- **It doesn't cover price.** Cost isn't part of the workspace, so a catalog price change after approval doesn't
  expire the intent. The policy decided on the price at approval time.
- **It's not a signature.** Someone who can write to the database could change the spec and `plan_hash` together.
  The hash protects against changes in code and catalog, not against an attacker with database access; an HMAC with
  a key kept outside the database would.
- **It's not the approval's expiry.** Approvals also expire after an hour (`approval-ttl`), independently of the hash.

## Planned improvements

Not built yet. In order of priority.

### 1. Harden the hash (small)

| Change | Why |
|---|---|
| Store `v1:sha256:<hex>` and hash each file as length + name + length + content | Today only a newline separates a file's name from its content, so two different workspaces could in theory produce the same byte stream. Length prefixes make it unambiguous; the version prefix lets the format change later without expiring every approved intent by surprise |
| Include the OpenTofu version in the hash | The lock file pins providers but not OpenTofu itself; a different `tofu` binary on a new instance can behave differently with the same files |
| Record the applied hash on `runs` and `resources` | Answers "which exact code created this resource?" afterwards, and lets the reconciler flag resources created by code that has since changed |

### 2. Sign the approval (small)

Today an approval is a state plus a `plan_hash` column, so someone who can write to the database can change both
together. A signed approval record closes that:

```
approval = HMAC(key, intent_id | tenant_id | plan_hash | approved_by | reason | expires_at)
```

The key lives outside the database (environment or secret manager), with a key id stored next to the signature for
rotation. The worker verifies the signature before applying, so database access alone can't forge or widen an
approval. This binds the approval to plan, tenant and intent literally.

### 3. A real saved plan with a verifier (larger)

Today the hash fixes Linfra's input to OpenTofu, not OpenTofu's decision. Planning would run `tofu plan`:

```
PROPOSED ──tofu plan -out=plan.bin (a worker job)──▶ PLANNED
           plan_hash = sha256(plan.bin)
           verifier reads `tofu show -json plan.bin`: only allowed resource types, no deletes or replacements,
           intent_id / tenant_id labels present, sizes and region match the intent
APPROVED ──worker: tofu apply plan.bin──▶ resource
```

| Gains | Costs |
|---|---|
| OpenTofu refuses a stale plan itself ("saved plan is stale") if the state changed since planning | Planning becomes asynchronous (seconds of `tofu plan`), so confirming a draft can't auto-approve in the same transaction: a `PLANNING` state and a second job type |
| Data sources resolve at planning time (with a real cloud, e.g. "the latest Ubuntu image") | Plan files are stored (a `bytea` column is enough here) and can contain sensitive values |
| The verifier checks what OpenTofu will actually do, e.g. rejects a module change that would replace a VM instead of updating it | Plan files are tied to the OpenTofu and provider versions, so deployments that change them still expire approvals, as today |
| Humans approve a real diff, shown on the intent card | |

### Not planned: price in the hash

Cost isn't part of the workspace, and putting it in would expire intents over small price changes. Instead, the
policy's budget check should run again when the worker claims the job: one call to the existing check.

### Decision: recompile, don't replay the approved artifact

The worker recompiles and compares, so a deployment that changes the module expires intents approved but not yet
applied. The alternative, storing the approved workspace or plan file and applying it as-is, lets approvals survive
deployments but runs old module code under a new version of the app, which is wrong when the deployment fixed the
module. Linfra keeps expiring on change: safer, at the cost of some re-approvals during deployments.

## Where it's tested

| Test | Checks |
|---|---|
| `TerraformCompilerTest` | The same intent always compiles to the same workspace and hash; different intents differ |
| `IntentPlannerTest` | The stored `plan_hash` equals the compiled workspace's hash |
| `ProvisionerPlanCheckTest` | A stale hash is refused before OpenTofu runs; a matching one applies |
| `PlanChangeTest` | A refused plan expires the intent and closes the job, with no retries |
| `DockerProvisioningTest` | The full path on real Docker: plan, policy, approval, hash check, container |

## Code

`TerraformCompiler.compile` (builds and hashes the workspace) · `IntentPlanner` (stores the hash) ·
`IntentService.approve` (approves for that hash) · `Provisioner.apply` (checks it) · `OutboxStore.expire`
