terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "4.6.0"
    }
  }
  # Connection string from PG_CONN_STR; one workspace per intent.
  backend "pg" {}
}

# Talks to the local Docker daemon (DOCKER_HOST or the default socket).
provider "docker" {}
