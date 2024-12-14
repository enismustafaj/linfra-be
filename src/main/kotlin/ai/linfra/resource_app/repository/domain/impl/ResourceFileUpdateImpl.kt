package ai.linfra.resource_app.repository.domain.impl

import ai.linfra.resource_app.repository.configuration.ResourceRepositoryProperties
import ai.linfra.resource_app.resource.domain.dependencies.ResourceFileUpdateUseCase
import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Component
class ResourceFileUpdateImpl(
    private val resourceRepositoryProperties: ResourceRepositoryProperties,
    private val gitConfiguration: Git
): ResourceFileUpdateUseCase {

    internal val log = LoggerFactory.getLogger(ResourceFileDeletionImpl::class.java)

    override fun updateResourceFile(fileName: String, content: String) {
        val file = File(getFilePath(fileName))

        if (file.exists()) {
            file.writeText(content)
            gitConfiguration.add().addFilepattern("$fileName.tf").call()
            gitConfiguration.commit().setMessage("Updated $fileName").call()
            gitConfiguration.push().call()
        } else {
            log.error("File $fileName does not exist")
        }
    }


    private fun getFilePath(fileName: String): String {
        return resourceRepositoryProperties.localRepoPath + "/" + fileName + ".tf"
    }
}