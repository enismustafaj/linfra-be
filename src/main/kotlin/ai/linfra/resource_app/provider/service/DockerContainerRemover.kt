package ai.linfra.resource_app.provider.service

import ai.linfra.resource_app.ranker.model.Provider
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class DockerContainerRemover : ResourceRemover {
    override val provider = Provider.DOCKER

    // ponytail: removes the container only; a network-disk volume stays behind until a volume finder reports it
    override fun remove(cloudId: String) {
        val process = ProcessBuilder("docker", "rm", "--force", cloudId).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(60, TimeUnit.SECONDS)) { "docker rm timed out for $cloudId" }
        check(process.exitValue() == 0 || output.contains("No such container")) { "docker rm failed for $cloudId: $output" }
    }
}
