# Local provider

Linfra provisions on a local, simulated cloud: each "VM" is a Docker container created by OpenTofu through the
`docker-vm` module. No cloud account is needed, and everything Linfra does (state, locking, `find()`, recovery) runs
for real.

## What is real and what is simulated

| Real (container settings) | Simulated |
|---|---|
| Memory limit from the instance type | Prices |
| Dedicated CPU → pinned cores (`cpu_set`); burstable → CPU shares | Regions (`local-eu`, `local-us` both run on the local daemon) |
| Architecture → image platform (`linux/amd64`, `linux/arm64`) | Public IPv4 (the container's bridge IP is used) |
| Network disk → a Docker volume; local disk types get none | Volume size (the local driver can't cap it; it's a label) |

## How a VM is created

1. `TerraformCompiler` produces the `docker-vm` files and `terraform.tfvars.json`.
2. `TofuRunner` writes them to a directory unique to the attempt (intent and fencing token), runs `init`, selects the
   intent's state workspace `intent-<id>` in the pg backend, and runs `apply`.
3. The outputs (`resource_id` = container id, `ipv4`) become the `resources` row.

## Finding a VM

`DockerContainerFinder` asks Docker directly (`docker ps --all --filter label=intent_id=<id>`), not Linfra's records
or Terraform state, so it finds containers whose creator crashed before recording them. Stopped containers count.

## Extending to real clouds

AWS or Hetzner plug into the same seams: catalog entries, a module, a `VmModule` and a `ResourceFinder`. The worker,
outbox, compiler and recovery don't change.

## Tests

`mvn verify -Ptofu` (needs `tofu` and Docker), all against real containers and destroying everything afterwards:

- `docker-vm` module tests with a mock provider.
- `DockerProvisioningTest`: the full path from plan and policy to a container; a crashed worker's container is adopted
  rather than duplicated.
- `DockerReconcilerTest`: drift (`docker update`), missing (`docker rm`) and orphans are found, and destroying an
  orphan through a finding removes it.
