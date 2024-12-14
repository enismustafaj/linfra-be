package ai.linfra.resource_app.resource.domain.dependencies

interface ResourceFileDeletionUseCase {
    fun deleteResourceFile(fileName: String)
}