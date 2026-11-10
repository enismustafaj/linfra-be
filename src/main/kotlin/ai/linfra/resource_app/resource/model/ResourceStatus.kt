package ai.linfra.resource_app.resource.model

enum class ResourceStatus {
    ACTIVE,
    DESTROYED,
    /** Our record says it exists, the cloud says it doesn't (set by the reconciler). */
    MISSING,
}
