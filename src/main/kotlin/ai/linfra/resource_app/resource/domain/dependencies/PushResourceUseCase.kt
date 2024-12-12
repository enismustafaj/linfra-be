package ai.linfra.resource_app.resource.domain.dependencies

interface PushResourceUseCase {
    fun pushResource(resourcePath: String)
}