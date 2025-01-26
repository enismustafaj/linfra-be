package ai.linfra.resource_app.resource.domain.model

data class ResourceDescription(
    val resourceId: String? = null,
    val userId: String? = null,
    val chatThreadId: String,
    val resourceName: String,
    val resourceType: ResourceTypeEnum,
    val resourceStatus: ResourceStatusEnum? = null,
    val resourceProperties: List<ResourceProperty>
)
