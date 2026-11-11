# Terraform generation

Each resource type is a normal OpenTofu module checked into `src/main/resources/terraform/<module>`. Per intent,
Linfra generates only one file, `terraform.tfvars.json`, with the module's input values. Linfra never generates
Terraform code. How OpenTofu then runs it, and how its state and locks live in Postgres: [opentofu.md](opentofu.md).

| Module | Resource |
|---|---|
| `docker-vm` | `docker_container` (plus a `docker_volume` for network disk) |

## No injection

Variable values are data: OpenTofu never evaluates them as templates, so `"${file("/etc/passwd")}"` stays a literal
string. On top of that, three layers stop anything unexpected from reaching the workspace:

1. **Allow-list (Kotlin):** provider, instance type and region are looked up in `PriceCatalog` by key, and the
   catalog's own values are used. Ids are UUIDs, disk size is range-checked, and free text such as `purpose` is
   never passed. Anything else → `NotInAllowListException`.
2. **Backstop (Kotlin):** `TerraformValues.requireSafe` only lets strings matching `[A-Za-z0-9._-]` through.
3. **Module validation (HCL):** every variable has a `validation` block (UUID format, instance type format,
   `amd64`/`arm64` only, disk range). A bad value fails at plan time even if layers 1 and 2 are bypassed.

The compiler also checks that the generated keys match exactly the variables the module declares, so the Kotlin and
HCL sides can't drift apart silently.

## Workspace and hash

`TerraformCompiler` returns the module's `.tf` files plus `terraform.tfvars.json`, and a SHA-256 over all of them.
The same intent always gives the same hash; a change to either the module or the values changes it.

## Other properties

- **No credentials in config:** providers read their credentials from the environment. The `pg` backend reads
  `PG_CONN_STR`, which `TofuRunner` derives from Linfra's own datasource: state lives in the same Postgres, in
  OpenTofu's `terraform_remote_state` schema.
- **Pinned providers and lock files:** each module commits `.terraform.lock.hcl`. Besides pinning checksums, this lets
  OpenTofu reuse the plugin cache; without it every `init` downloads providers again.
- **Findable:** every resource is named `linfra-<intent_id>` and labeled with `intent_id`, `tenant_id` and
  `managed_by`, so the worker can `find()` it before retrying.

## Adding a resource type

Add a module folder (`versions.tf`, `variables.tf` with validation, `main.tf`, `outputs.tf`,
`tests/validation.tftest.hcl`, lock file) and a `VmModule` that maps a target to its variables. A new provider
(AWS, Hetzner) also needs catalog entries and a `ResourceFinder`.

## Tests

- **Kotlin** (`mvn verify`): allow-list, injection attempts, determinism, the variables each module receives.
- **Modules** (`mvn verify -Ptofu`, or `tofu test` in the module folder): `fmt`, `validate`, and plan-time checks with
  mock providers (injection payloads, malformed ids, out-of-range values). No credentials needed. Excluded from the
  default build because it downloads providers. CI runs it as a separate job with a plugin cache.

## Classes

`TerraformCompiler` (allow-list, tfvars, hash) · `TerraformModules` (reads module files) · `DockerVmModule` ·
`TerraformValues` · `TofuRunner` (runs OpenTofu) · `PgConnString`
