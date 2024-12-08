package ai.linfra.resource_app.resource.domain.api

interface DeleteResourceUseCase {
    fun deleteResource(chatId: String, resourceId: String)
}