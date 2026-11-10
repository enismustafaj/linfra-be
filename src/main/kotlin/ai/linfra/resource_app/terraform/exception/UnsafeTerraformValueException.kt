package ai.linfra.resource_app.terraform.exception

class UnsafeTerraformValueException(value: String) :
    RuntimeException("Refusing to emit value into Terraform: \"${value.take(80)}\"")
