package ai.linfra.resource_app.repository.configuration

import org.eclipse.jgit.api.Git
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class ResourceRepositoryConfiguration(
    private val resourceRepositoryProperties: ResourceRepositoryProperties
) {

    @Bean
    fun gitConfiguration(): Git {
        return Git.cloneRepository()
            .setURI(resourceRepositoryProperties.repoUrl)
            .setDirectory(File(resourceRepositoryProperties.localRepoPath))
            .call()
    }
}