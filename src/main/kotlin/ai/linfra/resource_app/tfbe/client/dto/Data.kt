package ai.linfra.resource_app.tfbe.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class Data(
    val type: String,
    val attributes: Attributes
)