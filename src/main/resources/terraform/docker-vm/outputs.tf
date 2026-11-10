output "resource_id" {
  value = docker_container.vm.id
}

output "ipv4" {
  value = try(docker_container.vm.network_data[0].ip_address, null)
}
