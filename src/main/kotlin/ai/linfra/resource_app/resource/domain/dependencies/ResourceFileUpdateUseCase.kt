package ai.linfra.resource_app.resource.domain.dependencies

interface ResourceFileUpdateUseCase {
    fun updateResourceFile(fileName: String, content: String)
}