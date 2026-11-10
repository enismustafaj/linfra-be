# Runs with a mock provider: no Docker daemon needed. "$${" is a literal "${" in HCL.

mock_provider "docker" {}

variables {
  intent_id       = "11111111-1111-4111-8111-111111111111"
  tenant_id       = "22222222-2222-4222-8222-222222222222"
  instance_type   = "burst.medium"
  region          = "local-eu"
  vcpu            = 2
  memory_mb       = 4096
  dedicated_cpu   = false
  architecture    = "amd64"
  network_disk_gb = 20
}

run "burstable_with_network_disk" {
  command = plan

  assert {
    condition     = docker_container.vm.name == "linfra-11111111-1111-4111-8111-111111111111"
    error_message = "name must derive from intent_id"
  }
  assert {
    condition     = contains([for l in docker_container.vm.labels : l.value if l.label == "intent_id"], var.intent_id)
    error_message = "intent_id label is required for find()"
  }
  assert {
    condition     = docker_container.vm.memory == 4096 && docker_container.vm.cpu_shares == 1024 && docker_container.vm.cpu_set == null
    error_message = "burstable gets memory limit and CPU shares, no pinned cores"
  }
  assert {
    condition     = length(docker_volume.disk) == 1 && docker_image.base.platform == "linux/amd64"
    error_message = "network disk volume and image platform expected"
  }
}

run "dedicated_with_local_disk" {
  command = plan
  variables {
    instance_type   = "storage.medium"
    dedicated_cpu   = true
    network_disk_gb = 0
  }

  assert {
    condition     = docker_container.vm.cpu_set == "0-1" && length(docker_volume.disk) == 0
    error_message = "dedicated pins cores; local disk needs no volume"
  }
}

run "template_expression_in_instance_type_is_rejected" {
  command = plan
  variables {
    instance_type = "$${file(\"/etc/passwd\")}"
  }
  expect_failures = [var.instance_type]
}

run "non_uuid_intent_id_is_rejected" {
  command = plan
  variables {
    intent_id = "x\"; rm -rf /"
  }
  expect_failures = [var.intent_id]
}

run "unknown_architecture_is_rejected" {
  command = plan
  variables {
    architecture = "amd64\"]"
  }
  expect_failures = [var.architecture]
}

run "oversized_disk_is_rejected" {
  command = plan
  variables {
    network_disk_gb = 100000
  }
  expect_failures = [var.network_disk_gb]
}
