package ai.linfra.resource_app.repository.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resource-repository")
data class ResourceRepositoryProperties(
    val repoUrl: String,
    val localRepoPath: String,
    val repoUsername: String,
    val repoPassword: String
)
