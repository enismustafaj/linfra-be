package ai.linfra.resource_app.repository.domain.impl

import ai.linfra.resource_app.common.security.api.GetAuthenticatedUserUseCase
import ai.linfra.resource_app.common.security.model.AuthenticatedUser
import ai.linfra.resource_app.repository.configuration.ResourceRepositoryProperties
import ai.linfra.resource_app.resource.domain.dependencies.ResourceFileCreationUseCase
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.stereotype.Component
import java.io.File

@Component
class ResourceFileCreationImpl(
    private val resourceRepositoryProperties: ResourceRepositoryProperties,
    private val gitConfiguration: Git,
    private val getAuthenticatedUserUseCase: GetAuthenticatedUserUseCase
): ResourceFileCreationUseCase {

    internal val fileExtension: String = ".tf"

    override fun createResourceFile(fileName: String, fileContent: String) {
        File(getFilePath(fileName)).writeText(fileContent)
        val authenticatedUser: AuthenticatedUser = getAuthenticatedUserUseCase.getAuthenticatedUserUseCase();

        gitConfiguration.add().addFilepattern("$fileName.tf").call()
        gitConfiguration.commit().setMessage(buildGitCommitMessage(authenticatedUser.userId, fileName)).call()
        gitConfiguration.push().setCredentialsProvider(getCredentialProvider()).call()
    }

    private fun getFilePath(fileName: String): String {
        return resourceRepositoryProperties.localRepoPath + "/" + fileName + fileExtension
    }

    private fun getCredentialProvider(): UsernamePasswordCredentialsProvider {
        return UsernamePasswordCredentialsProvider(
            resourceRepositoryProperties.repoUsername,
            resourceRepositoryProperties.repoPassword
        )
    }

    private fun buildGitCommitMessage(userId: String, resourceId: String): String {
        return "action:CREATED,user:$userId,resource:$resourceId"
    }

}