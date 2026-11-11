# OpenTofu and the Postgres state backend

How Linfra turns an approved intent into a running resource with OpenTofu, and how OpenTofu's state and locks are kept
in Postgres. For how the files are generated and protected against injection, see
[terraform-generation.md](terraform-generation.md); for the hash that binds an approval to them, see
[plan-hash.md](plan-hash.md).

The behaviour described under "The pg backend" and "Failures" was observed on OpenTofu 1.13.9 with Postgres 17,
not taken from documentation.

## Roles

| Who | Does |
|---|---|
| **Linfra** | Decides *what* to create (ranker, policy), writes the variable values, runs OpenTofu, records the result |
| **OpenTofu** | Decides *how*: compares the module with its state, calls the provider, saves the new state |
| **Provider** (`kreuzwerker/docker` 4.6.0) | Talks to the Docker daemon: pulls images, creates volumes and containers |
| **Postgres** | Holds Linfra's tables and, in a separate schema, OpenTofu's state and locks |

OpenTofu is the open-source fork of Terraform; the module and state formats are the same. Linfra never generates
OpenTofu code: the modules are checked in, and Linfra only writes `terraform.tfvars.json`.

## The module

```
src/main/resources/terraform/docker-vm/
  versions.tf               required provider (pinned), backend "pg" {}, provider "docker" {}
  variables.tf              typed inputs, each with a validation block
  main.tf                   the resources
  outputs.tf                resource_id, ipv4
  .terraform.lock.hcl       provider version and checksums
  tests/validation.tftest.hcl   module tests with a mock provider (not copied into workspaces)
```

### Inputs

| Variable | Type | Validation | Comes from |
|---|---|---|---|
| `intent_id` | string | UUID | the intent |
| `tenant_id` | string | UUID | the intent |
| `instance_type` | string | `^[a-z]+\.[a-z]+(\.[a-z]+)?$` | catalog entry |
| `region` | string | `^local-[a-z]{2}$` | catalog entry |
| `vcpu` | number | whole, 1–16 | catalog entry |
| `memory_mb` | number | whole, 512–65536 | catalog entry (GB × 1024) |
| `dedicated_cpu` | bool | | catalog CPU model is `DEDICATED` |
| `architecture` | string | `amd64` or `arm64` | catalog entry |
| `network_disk_gb` | number | 0, or whole 10–2000 | the draft's disk size when the type has no local disk, else 0 |

Validation blocks are a second guard behind Linfra's allow-list: a bad value fails at plan time even if it got past
the Kotlin side.

### Resources

```
docker_image.base       alpine:3.20 for linux/<architecture>; kept locally after destroy
docker_volume.disk      only when network_disk_gb > 0; named linfra-<intent_id>; size is a label
docker_container.vm     the "VM": named linfra-<intent_id>, runs `sleep infinity`, restart unless-stopped
                          memory   = memory_mb
                          cpu_set  = "0-<vcpu-1>"    when dedicated (pinned cores)
                          cpu_shares = vcpu × 512    when burstable
                          mounts the volume at /data when there is one
                          labels: managed_by=linfra, intent_id, tenant_id, instance_type, region
```

By convention the main resource of every module is named `vm`. The name `linfra-<intent_id>` is deterministic, and
Docker refuses a second container with the same name: that is one of the layers that keep it to one resource per
intent. The labels are how `find()` and the reconciler recognise Linfra's resources.

### Outputs

| Output | Value |
|---|---|
| `resource_id` | the container id, stored as the resource's `cloud_id` |
| `ipv4` | the container's IP on its first network, or null (`try()` keeps mocks and network-less containers working) |

## From intent to workspace

`TerraformCompiler.compile` resolves the intent's spec to a catalog entry (by provider, type and region, using the
catalog's own values), builds the variables, checks they match exactly the variables the module declares, and returns:

- the module's files (`*.tf` and the lock file; tests excluded),
- `terraform.tfvars.json` (canonical JSON, sorted keys),
- the SHA-256 over all of them: the plan hash.

Example `terraform.tfvars.json`:

```json
{
  "architecture" : "amd64",
  "dedicated_cpu" : false,
  "instance_type" : "burst.medium",
  "intent_id" : "6514e7d1-6c1b-47d4-958a-0b8c8c2cbe37",
  "memory_mb" : 4096,
  "network_disk_gb" : 20,
  "region" : "local-eu",
  "tenant_id" : "2f0c…",
  "vcpu" : 2
}
```

## Running OpenTofu: `TofuRunner`

The worker calls `Provisioner.apply`, which recompiles the workspace, checks it against the approved plan hash, and
hands it to `TofuRunner.apply`.

### Step by step

1. **Working directory.** `<work-dir>/<intent id>-<fencing token>`, where `work-dir` is `linfra.tofu.work-dir`
   (default `<java.io.tmpdir>/linfra-tofu`). The fencing token makes it unique per attempt, so a stale worker and its
   replacement never share files. Any leftover directory with that name is deleted first.
2. **Files.** The module files and `terraform.tfvars.json` are written into it.
3. **`tofu init -no-color`.** Configures the pg backend and installs the provider from the plugin cache. Runs under a
   per-process lock (see "Schema creation" below).
4. **`tofu workspace select -or-create=true intent-<intent id>`.** Selects the intent's own state, creating it on
   the first attempt.
5. **`tofu apply -auto-approve -no-color`.** Takes the state lock, compares, creates what's missing, saves the
   state, releases the lock.
6. **`tofu output -json -no-color`.** Reads `resource_id` and `ipv4`.
7. **Clean-up.** The directory is deleted, in a `finally` block, whatever happened.

`destroy` follows the same steps with `tofu destroy -auto-approve` instead of apply. It's used by the tests' clean-up;
there is no destroy flow for users yet.

### Environment

Every `tofu` process gets:

| Variable | Value | Why |
|---|---|---|
| `PG_CONN_STR` | `postgres://<user>:<password>@<host>:<port>/<db>?sslmode=disable` | Where the pg backend keeps state. Built by `PgConnString` from Spring's own datasource (`JdbcConnectionDetails`), so it's always the same database Linfra uses, including under Testcontainers. Credentials are URL-encoded |
| `TF_PLUGIN_CACHE_DIR` | `linfra.tofu.plugin-cache-dir` (`~/.cache/linfra-tofu-plugins`) | Providers are downloaded once and reused |
| `TF_IN_AUTOMATION` | `1` | Output without hints meant for humans |
| `TF_INPUT` | `0` | Never wait for interactive input |

Nothing in the module or `tfvars` contains a credential. The Docker provider uses the local daemon (`DOCKER_HOST` or
the default socket).

### Output, errors and timeouts

- Standard output and error are merged and read line by line on a separate thread, so a chatty process can't block.
- A non-zero exit raises `TofuException` with the last 500 characters of output; the full output is logged at `WARN`.
  The worker treats it as a failed attempt: retry with backoff, then `NEEDS_ATTENTION`.
- Each command has `linfra.tofu.timeout` (10 minutes). On timeout, the process **and its children** (the provider
  plugin) are killed, and the attempt fails.

### Plugin cache and lock file

The lock file pins the provider version and its checksums (`h1:` for the platforms it was created on, `zh:` for all
of them from the registry). It also matters for speed: **without a lock file, OpenTofu doesn't use the plugin cache
and downloads the provider on every `init`**. That turned a 0.3-second `init` into minutes for the large AWS
provider before lock files were committed.

## The pg backend

### Configuration

`versions.tf` declares an empty backend:

```hcl
terraform {
  backend "pg" {}   # connection string from PG_CONN_STR
}
```

Everything else is the backend's default: schema `terraform_remote_state`, schema and table created on first use.

### What it creates

On the first `init` it creates the equivalent of (reconstructed from the objects found in the database):

```sql
create schema terraform_remote_state;
create sequence public.global_states_id_seq;            -- note: in public, not in the backend's schema

create table terraform_remote_state.states (
    id   bigint primary key default nextval('global_states_id_seq'),
    name text unique,                                     -- the workspace name
    data text                                             -- the whole state, as JSON
);
create unique index states_by_name on terraform_remote_state.states (name);
```

Flyway and Hibernate never touch this schema: Flyway only runs `src/main/resources/migrations`, and Hibernate only
validates Linfra's own entities. `docker compose down -v` deletes it along with everything else.

### Workspaces are rows

Each intent has its own workspace, `intent-<intent id>`, so its own row:

```
 id | name                                          | data
----+-----------------------------------------------+---------------------------
  1 | intent-6514e7d1-6c1b-47d4-958a-0b8c8c2cbe37   | {"version":4,"serial":2,…}
```

One workspace per intent keeps states small, lets intents be applied in parallel without waiting on each other's
locks, and lets one intent be destroyed without touching any other.

### What a state row holds

`data` is OpenTofu's standard state document:

```json
{
  "version": 4,
  "terraform_version": "1.13.9",
  "serial": 2,
  "lineage": "853af3be-…",
  "outputs": { "ipv4": {…}, "resource_id": {…} },
  "resources": [
    { "type": "docker_container", "name": "vm",   "instances": [ { "attributes": { "id": "…", "name": "linfra-…", … } } ] },
    { "type": "docker_image",     "name": "base", … },
    { "type": "docker_volume",    "name": "disk", … }
  ]
}
```

- `serial` goes up with every write. Observed: created by `workspace select` (an empty state), 2 after the apply,
  3 after a destroy.
- `lineage` identifies this state's history; OpenTofu refuses to overwrite a state with a different lineage.
- The row stays after a destroy, with no resources in it.

### Locking

The lock is a Postgres **session-level advisory lock** whose key is the workspace row's `id`:

```sql
select pg_try_advisory_lock(<states.id>);   -- held by the tofu process's own connection
```

Observed during an apply: one advisory lock with `objid` = the row's `id`, held for the whole apply.

- **Held** from the start of `apply` (or `destroy`) until the new state is saved.
- **Not waited for.** A second `tofu apply` on the same workspace fails at once with
  `Error acquiring the state lock … Workspace is already locked`, because Linfra doesn't pass `-lock-timeout`.
  The worker counts that as a failed attempt and retries with backoff.
- **Released automatically when the process dies.** Advisory locks belong to the database session, so when `tofu` is
  killed its connection closes and the lock disappears. Observed: zero advisory locks immediately after `kill -9`.
  There's never a stale lock to clean up, and `force-unlock` isn't needed.
- **Per intent.** Different intents have different rows, so different lock keys: their applies never block each other.

### Schema creation

The first `init` against an empty database creates the schema, sequence and table. Two `init`s doing that at the same
moment can collide, so `TofuRunner` runs `init` under a lock inside each process. Across several instances this can
only matter on the very first run against a fresh database.

## Failures

Observed by killing `tofu` with `kill -9` while it was creating the container:

| After the kill | State |
|---|---|
| State lock | released immediately |
| State in Postgres | not updated: still the previous serial, no resources |
| Container | **exists** (Docker had already created it) |
| Volume | **exists** |
| A plain second `tofu apply` | **fails**: `Conflict. The container name "/linfra-<intent id>" is already in use` |

This is exactly why the worker calls `find(intent_id)` before applying: it finds the container by its label and
adopts it instead of running OpenTofu again. Without that, the intent would fail on the name conflict until it ended
in `NEEDS_ATTENTION`. Adopting also brings the leftovers into OpenTofu's state (next section).

## Adoption: importing into state

Whenever `find()` returns a resource, the worker adopts it in two steps:

1. **Into OpenTofu's state.** `TofuRunner.importMissing` initialises the intent's workspace, runs
   `tofu state list`, and for each of the module's resources that isn't there runs
   `tofu import -no-color <address> <id>`. The module says what to import (`VmModule.imports`):

   | Address | Id |
   |---|---|
   | `docker_container.vm` | the container id `find()` returned |
   | `docker_volume.disk[0]` | `linfra-<intent id>`, only for types with a network disk |

   The image isn't imported: it's shared and kept locally, and needs no state.
2. **Into Linfra's records.** The resource row and `SUCCEEDED`, as for a fresh create.

In the usual case, where `tofu` saved state before the worker died, `state list` already shows everything and nothing
is imported. Only the case from the table above (provider created the resources, state never saved) imports.

**Import, never apply.** `tofu import` only writes state; it doesn't change the resource. An apply would be wrong
here: observed right after importing, `tofu plan` wanted to **replace** the container and the volume, because
imported attributes never match the configuration exactly. Linfra never applies an intent again after it succeeds, so
the next OpenTofu action on the workspace is a destroy, and that removes the imported container and volume
(observed: `Destroy complete! Resources: 2 destroyed.`, nothing left in Docker).

If the import fails, for example because a surviving `tofu` still holds the lock, the attempt fails and is retried;
by then the state is usually complete and the import is skipped.

The remediation action `ADOPT` (an orphan adopted into its `NEEDS_ATTENTION` intent) imports the same way.

Other cases:

| Situation | What happens |
|---|---|
| Worker's JVM dies but its `tofu` keeps running | `tofu` finishes and saves state; meanwhile the next worker's apply fails on the lock and retries; once the lock is free, `find()` adopts |
| `tofu` exceeds the 10-minute timeout | Process and provider plugin killed; lock released with the connection; retried like any failure |
| Postgres unreachable | `init` or `apply` fails; retried with backoff |
| The plan changed since approval | Caught before `tofu` runs; nothing applied; intent `EXPIRED` ([plan-hash.md](plan-hash.md)) |

## Known gaps

- **No `-lock-timeout`.** A worker that finds the lock held fails the attempt instead of waiting; it costs retries,
  not correctness. Passing `-lock-timeout=60s` would let it wait for the previous apply.
- **Workspace rows are never removed.** Destroyed intents keep an empty state row. Harmless, but they accumulate;
  `tofu workspace delete` after a destroy would clean up.
- **State is plain text in the database.** Today it holds no secrets (no passwords are created), but a module that
  creates one would store it in `data`. OpenTofu's client-side state encryption would protect it.
- **No saved plan.** Apply recomputes the changes; see the planned saved-plan work in [plan-hash.md](plan-hash.md).

## Inspecting by hand

```sh
# All workspaces and their sizes
docker compose exec postgres psql -U linfra -d linfra -c \
  "select id, name, length(data) from terraform_remote_state.states order by id"

# Resources in one intent's state
docker compose exec postgres psql -U linfra -d linfra -At -c \
  "select data from terraform_remote_state.states where name = 'intent-<intent id>'" | jq '.resources[] | .type + "." + .name'

# Locks held right now
docker compose exec postgres psql -U linfra -d linfra -c \
  "select objid as state_row, pid, granted from pg_locks where locktype = 'advisory'"
```

To run OpenTofu by hand against an intent's state, copy the module into a directory, add its `terraform.tfvars.json`,
and use the same environment:

```sh
export PG_CONN_STR="postgres://linfra:linfra@localhost:5432/linfra?sslmode=disable"
export TF_PLUGIN_CACHE_DIR=~/.cache/linfra-tofu-plugins
tofu init && tofu workspace select intent-<intent id> && tofu state list
```

## Tests

| Test | Covers |
|---|---|
| `TerraformModuleTest` (`-Ptofu`) | `fmt`, `validate` and the module's own `tofu test` (mock provider: valid inputs, injection payloads, malformed ids, out-of-range values) |
| `DockerProvisioningTest` (`-Ptofu`) | Real applies through `TofuRunner` with the pg backend in the test Postgres; a crashed worker's container is adopted, not duplicated; leftovers OpenTofu never saved are imported into state and removed by a later destroy |
| `ProvisionerAdoptTest` | Which resources are imported, per instance type |
| `PgConnStringTest` | JDBC URL → libpq URL, credential encoding |
| `ProvisionerPlanCheckTest` | `tofu` never runs when the plan hash doesn't match |

## Code

`TofuRunner` (workspace, commands, environment, timeouts) · `PgConnString` · `TerraformCompiler` ·
`TerraformModules` (reads the module from the classpath) · `DockerVmModule` (variables) · `Provisioner`
