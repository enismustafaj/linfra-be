package ai.linfra.resource_app.terraform.model

/**
 * What one intent's OpenTofu workspace contains: a checked-in module's files plus the generated terraform.tfvars.json.
 * sha256 covers both, so a changed module or changed values both change the hash.
 */
data class CompiledWorkspace(
    val module: String,
    val moduleFiles: Map<String, String>,
    val tfvarsJson: String,
    val sha256: String,
)
