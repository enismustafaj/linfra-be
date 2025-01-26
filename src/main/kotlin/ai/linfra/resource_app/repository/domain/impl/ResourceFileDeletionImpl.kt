package ai.linfra.resource_app.repository.domain.impl

import ai.linfra.resource_app.common.security.api.GetAuthenticatedUserUseCase
import ai.linfra.resource_app.common.security.model.AuthenticatedUser
import ai.linfra.resource_app.repository.configuration.ResourceRepositoryProperties
import ai.linfra.resource_app.resource.domain.dependencies.ResourceFileDeletionUseCase
import lombok.extern.slf4j.Slf4j
import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Slf4j
@Component
class ResourceFileDeletionImpl(
    private val resourceRepositoryProperties: ResourceRepositoryProperties,
    private val gitConfiguration: Git,
    private val getAuthenticatedUserUseCase: GetAuthenticatedUserUseCase
): ResourceFileDeletionUseCase {

    internal val log = LoggerFactory.getLogger(ResourceFileDeletionImpl::class.java)

    override fun deleteResourceFile(fileName: String) {
        val file = File(getFilePath(fileName))
        val authenticatedUser: AuthenticatedUser = getAuthenticatedUserUseCase.getAuthenticatedUserUseCase()

        if (file.exists()) {
            gitConfiguration.rm().addFilepattern("$fileName.tf").call()
            gitConfiguration.commit().setMessage(buildGitCommitMessage(authenticatedUser.userId, fileName)).call()
            gitConfiguration.push().call()
        } else {
            log.error("File $fileName does not exist")
        }
    }

    private fun getFilePath(fileName: String): String {
        return resourceRepositoryProperties.localRepoPath + "/" + fileName + ".tf"
    }

    private fun buildGitCommitMessage(userId: String, resourceId: String): String {
        return "action:DELETED,user:$userId,resource:$resourceId"
    }
}