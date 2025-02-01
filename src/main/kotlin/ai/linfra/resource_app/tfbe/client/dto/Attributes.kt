package ai.linfra.resource_app.tfbe.client.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Attributes(
    val name: String,
    val description: String,
    @SerialName("auto-apply") val autoApply: Boolean,
    @SerialName("execution-mode") val executionMode: String
)