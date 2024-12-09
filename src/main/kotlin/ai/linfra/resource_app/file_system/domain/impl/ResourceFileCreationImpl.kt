package ai.linfra.resource_app.file_system.domain.impl

import ai.linfra.resource_app.resource.domain.dependencies.ResourceFileCreationUseCase
import org.springframework.stereotype.Component
import java.io.File

@Component
class ResourceFileCreationImpl: ResourceFileCreationUseCase {

    internal val fileExtension: String = ".tf"

    override fun createResourceFile(fileName: String, fileContent: String) {
        File(fileName + fileExtension).writeText(fileContent)
    }
}