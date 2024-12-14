package ai.linfra.resource_app.resource.domain.handler

import ai.linfra.resource_app.resource.domain.dependencies.ResourceFileCreationUseCase
import ai.linfra.resource_app.resource.domain.model.event.DatabaseResourceCreatedEvent
import ai.linfra.resource_app.resource.domain.model.templates.DatabaseTemplate
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ResourceCreationHandler(
    private val templateEngine: TemplateEngine,
    private val resourceFileCreationUseCase: ResourceFileCreationUseCase
) {

    @EventListener
    fun handleDatabaseResourceCreationEvent(event: DatabaseResourceCreatedEvent) {
        val output = StringOutput()
        templateEngine.render("database.kte", mapOf("database" to createDatabaseTemplateData(event)), output)
        resourceFileCreationUseCase.createResourceFile(event.resourceId, output.toString())
    }

    private fun createDatabaseTemplateData(event: DatabaseResourceCreatedEvent): DatabaseTemplate {
        return DatabaseTemplate(
            event.databaseName,
            event.databaseEngine,
            event.databaseInstanceClass
        )
    }
}