package ai.linfra.resource_app.resource.domain.dependencies

interface ResourceFileCreationUseCase {
    fun createResourceFile(fileName: String, fileContent: String)
}