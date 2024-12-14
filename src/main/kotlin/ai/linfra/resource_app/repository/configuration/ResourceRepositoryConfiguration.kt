package ai.linfra.resource_app.repository.configuration

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
@EnableConfigurationProperties(ResourceRepositoryProperties::class)
class ResourceRepositoryConfiguration(
    private val resourceRepositoryProperties: ResourceRepositoryProperties
) {

    @Bean
    fun gitConfiguration(): Git {
        return Git.cloneRepository()
            .setURI(resourceRepositoryProperties.repoUrl)
            .setDirectory(File(resourceRepositoryProperties.localRepoPath))
            .setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    resourceRepositoryProperties.repoUsername,
                    resourceRepositoryProperties.repoPassword
                )
            )
            .call()
    }
}