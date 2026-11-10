package ai.linfra.resource_app.ranker.model

/**
 * Providers Linfra can provision on. DOCKER is a local, simulated cloud: containers stand in for VMs.
 * Adding a real cloud (AWS, Hetzner) = catalog entries, a Terraform module, a VmModule and a ResourceFinder.
 */
enum class Provider { DOCKER }
