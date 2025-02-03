package ai.linfra.resource_app.resource.domain.configuration

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.ResourceLoader
import java.nio.file.Path


@Configuration
class JteConfiguration {

    @Bean
    fun templateEngine(resourceLoader: ResourceLoader): TemplateEngine {
        return TemplateEngine.create(DirectoryCodeResolver(Path.of(resourceLoader.getResource("classpath:templates/").file.path)), ContentType.Plain)
    }
}