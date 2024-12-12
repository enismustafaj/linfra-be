package ai.linfra.resource_app.repository.domain.impl

import ai.linfra.resource_app.repository.configuration.ResourceRepositoryProperties
import ai.linfra.resource_app.resource.domain.dependencies.PushResourceUseCase
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.util.concurrent.locks.ReentrantLock

@Component
@EnableConfigurationProperties(ResourceRepositoryProperties::class)
class PushResourceImpl(
    private val resourceRepositoryProperties: ResourceRepositoryProperties,
    private val gitConfiguration: Git
): PushResourceUseCase {

    internal val operationLock: ReentrantLock = ReentrantLock()

    override fun pushResource(resourcePath: String) {
        try {
            operationLock.lock()
            gitConfiguration.add().addFilepattern(resourcePath).call()
            gitConfiguration.commit().setMessage("Added $resourcePath").call()
            gitConfiguration.push().setCredentialsProvider(getCredentialProvider()).call()
        } finally {
            operationLock.unlock()
        }
    }

    private fun getCredentialProvider(): UsernamePasswordCredentialsProvider {
        return UsernamePasswordCredentialsProvider(
            resourceRepositoryProperties.repoUsername,
            resourceRepositoryProperties.repoPassword
        )
    }
}