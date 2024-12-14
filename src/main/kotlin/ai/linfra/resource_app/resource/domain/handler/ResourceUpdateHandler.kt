package ai.linfra.resource_app.resource.domain.handler

import ai.linfra.resource_app.resource.domain.dependencies.ResourceFileUpdateUseCase
import ai.linfra.resource_app.resource.domain.model.event.DatabaseResourceUpdatedEvent
import ai.linfra.resource_app.resource.domain.model.templates.DatabaseTemplate
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ResourceUpdateHandler(
    private val resourceFileUpdateUseCase: ResourceFileUpdateUseCase,
    private val templateEngine: TemplateEngine
) {

    @EventListener
    fun handleDatabaseResourceUpdateEvent(event: DatabaseResourceUpdatedEvent) {
        val output = StringOutput()
        templateEngine.render("database.kte", mapOf("database" to createDatabaseTemplateData(event)), output)

        resourceFileUpdateUseCase.updateResourceFile(event.resourceId, output.toString())
    }

    private fun createDatabaseTemplateData(event: DatabaseResourceUpdatedEvent): DatabaseTemplate {
        return DatabaseTemplate(
            event.resourceNewName,
            event.resourceNewEngine,
            event.resourceNewInstanceClass
        )
    }
}