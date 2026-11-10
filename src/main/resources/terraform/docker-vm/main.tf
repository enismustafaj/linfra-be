locals {
  labels = {
    managed_by    = "linfra"
    intent_id     = var.intent_id
    tenant_id     = var.tenant_id
    instance_type = var.instance_type
    region        = var.region
  }
}

resource "docker_image" "base" {
  name         = "alpine:3.20"
  platform     = "linux/${var.architecture}"
  keep_locally = true
}

# Network disk: a separate volume, like EBS. The local driver cannot cap its size, so the size is a label.
resource "docker_volume" "disk" {
  count = var.network_disk_gb > 0 ? 1 : 0
  name  = "linfra-${var.intent_id}"

  dynamic "labels" {
    for_each = merge(local.labels, { size_gb = tostring(var.network_disk_gb) })
    content {
      label = labels.key
      value = labels.value
    }
  }
}

# A long-running container standing in for a VM. Docker rejects a second container with the same name, so the
# intent-derived name alone blocks a duplicate create.
resource "docker_container" "vm" {
  name    = "linfra-${var.intent_id}"
  image   = docker_image.base.image_id
  command = ["sleep", "infinity"]
  restart = "unless-stopped"
  memory  = var.memory_mb

  # Dedicated: pinned cores. Burstable: CPU shares, so it gets starved when the host is busy.
  cpu_set    = var.dedicated_cpu ? "0-${var.vcpu - 1}" : null
  cpu_shares = var.dedicated_cpu ? null : var.vcpu * 512

  dynamic "mounts" {
    for_each = docker_volume.disk
    content {
      type   = "volume"
      source = mounts.value.name
      target = "/data"
    }
  }

  dynamic "labels" {
    for_each = local.labels
    content {
      label = labels.key
      value = labels.value
    }
  }
}
