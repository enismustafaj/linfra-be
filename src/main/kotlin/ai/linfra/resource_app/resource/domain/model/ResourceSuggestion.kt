package ai.linfra.resource_app.resource.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ResourceSuggestion(
        val resourceName: String?,
        val resourceType: String?,
        val resourceSize: String?
) {
    fun isValidSuggestion(): Boolean {
        return resourceName != null && resourceType != null && resourceSize != null
    }
}
