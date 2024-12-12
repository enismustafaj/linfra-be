package ai.linfra.resource_app.repository.domain.impl

import ai.linfra.resource_app.repository.configuration.ResourceRepositoryProperties
import ai.linfra.resource_app.resource.domain.dependencies.ResourceFileCreationUseCase
import org.springframework.stereotype.Component
import java.io.File

@Component
class ResourceFileCreationImpl(
    private val resourceRepositoryProperties: ResourceRepositoryProperties
): ResourceFileCreationUseCase {

    internal val fileExtension: String = ".tf"

    override fun createResourceFile(fileName: String, fileContent: String) {
        File(getFilePath(fileName)).writeText(fileContent)
    }

    private fun getFilePath(fileName: String): String {
        return resourceRepositoryProperties.localRepoPath + "/" + fileName + fileExtension
    }
}