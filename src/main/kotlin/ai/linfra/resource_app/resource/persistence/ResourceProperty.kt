package ai.linfra.resource_app.resource.persistence

import kotlinx.serialization.Serializable

@Serializable
data class ResourceProperty(
    val propertyName: String,
    val propertyValue: String
)
