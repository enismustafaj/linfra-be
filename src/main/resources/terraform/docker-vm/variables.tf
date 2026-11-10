# Values arrive in terraform.tfvars.json as plain data: they are never evaluated as templates.
# The validation blocks are a second guard behind Linfra's catalog allow-list.

variable "intent_id" {
  type = string
  validation {
    condition     = can(regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", var.intent_id))
    error_message = "intent_id must be a UUID."
  }
}

variable "tenant_id" {
  type = string
  validation {
    condition     = can(regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", var.tenant_id))
    error_message = "tenant_id must be a UUID."
  }
}

variable "instance_type" {
  type = string
  validation {
    condition     = can(regex("^[a-z]+\\.[a-z]+(\\.[a-z]+)?$", var.instance_type))
    error_message = "instance_type must be a catalog instance type."
  }
}

variable "region" {
  type = string
  validation {
    condition     = can(regex("^local-[a-z]{2}$", var.region))
    error_message = "region must be a local region."
  }
}

variable "vcpu" {
  type = number
  validation {
    condition     = var.vcpu >= 1 && var.vcpu <= 16 && floor(var.vcpu) == var.vcpu
    error_message = "vcpu must be a whole number between 1 and 16."
  }
}

variable "memory_mb" {
  type = number
  validation {
    condition     = var.memory_mb >= 512 && var.memory_mb <= 65536 && floor(var.memory_mb) == var.memory_mb
    error_message = "memory_mb must be a whole number between 512 and 65536."
  }
}

variable "dedicated_cpu" {
  type = bool
}

variable "architecture" {
  type = string
  validation {
    condition     = contains(["amd64", "arm64"], var.architecture)
    error_message = "architecture must be amd64 or arm64."
  }
}

variable "network_disk_gb" {
  type        = number
  description = "Size of the attached volume; 0 when the instance type has its own local disk."
  validation {
    condition     = var.network_disk_gb == 0 || (var.network_disk_gb >= 10 && var.network_disk_gb <= 2000 && floor(var.network_disk_gb) == var.network_disk_gb)
    error_message = "network_disk_gb must be 0 or a whole number between 10 and 2000."
  }
}
