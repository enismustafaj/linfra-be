package ai.linfra.resource_app.conversation.service

import ai.linfra.resource_app.conversation.model.AgentAction
import ai.linfra.resource_app.conversation.model.AgentReply
import ai.linfra.resource_app.conversation.model.AssistantReply
import ai.linfra.resource_app.conversation.model.Draft
import ai.linfra.resource_app.ranker.service.Ranker
import org.springframework.stereotype.Service

private const val REPHRASE = "Sorry, I couldn't turn that into a request. Could you rephrase it?"

/** Turns untrusted LLM output into what the user sees. Anything short of a valid, rankable PROPOSE has no draft. */
@Service
class DraftBuilder(
    private val ranker: Ranker,
    private val agent: AgentClient,
) {
    fun build(reply: AgentReply?): AssistantReply {
        if (reply == null) return AssistantReply(REPHRASE)
        if (reply.action != AgentAction.PROPOSE) return AssistantReply(reply.message)
        val requirements = reply.requirements ?: return AssistantReply(REPHRASE)

        val errors = requirements.validationErrors()
        if (errors.isNotEmpty()) return AssistantReply("I can't offer that: ${errors.joinToString("; ")}.")

        val pick = ranker.pick(requirements)
            ?: return AssistantReply("No VM in the catalog fits those requirements and budget. Try a smaller size, another area or a higher budget.")

        // The explanation is cosmetic: if that call fails, the draft is still valid.
        val explanation = runCatching { agent.explain(requirements, pick) }
            .getOrElse { "Best match for your requirements: ${pick.entry.instanceType} on ${pick.entry.provider}, €${pick.cost.total}/month in total." }

        return AssistantReply(reply.message, Draft(requirements, pick, explanation))
    }
}
