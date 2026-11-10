package ai.linfra.resource_app.common.rest

import ai.linfra.resource_app.conversation.exception.ConversationNotFoundException
import ai.linfra.resource_app.conversation.exception.DraftNotConfirmableException
import ai.linfra.resource_app.conversation.exception.InvalidMessageException
import ai.linfra.resource_app.intent.exception.IntentNotFoundException
import ai.linfra.resource_app.resource.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.ai.retry.NonTransientAiException
import org.springframework.ai.retry.TransientAiException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException

@RestControllerAdvice
class ApiExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ConversationNotFoundException::class, IntentNotFoundException::class, ResourceNotFoundException::class)
    fun notFound() = ResponseEntity.notFound().build<Unit>()

    @ExceptionHandler(DraftNotConfirmableException::class)
    fun conflict(e: RuntimeException) = error(HttpStatus.CONFLICT, e.message)

    @ExceptionHandler(InvalidMessageException::class)
    fun badRequest(e: RuntimeException) = error(HttpStatus.BAD_REQUEST, e.message)

    @ExceptionHandler(TransientAiException::class, NonTransientAiException::class, RestClientException::class)
    fun llmUnavailable(e: Exception): ResponseEntity<Map<String, String?>> {
        log.warn("LLM call failed: {}", e.message)
        return error(HttpStatus.SERVICE_UNAVAILABLE, "assistant unavailable, try again")
    }

    private fun error(status: HttpStatus, message: String?) = ResponseEntity.status(status).body(mapOf("error" to message))
}
