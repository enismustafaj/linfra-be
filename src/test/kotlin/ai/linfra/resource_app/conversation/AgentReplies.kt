package ai.linfra.resource_app.conversation

import ai.linfra.resource_app.conversation.model.AgentAction
import ai.linfra.resource_app.conversation.model.AgentReply
import ai.linfra.resource_app.conversation.model.Environment
import ai.linfra.resource_app.conversation.model.VmRequirements
import ai.linfra.resource_app.ranker.model.Area

/** Canned LLM outputs for tests. */
object AgentReplies {
    val smallEuStaging = VmRequirements(
        vcpuMin = 2, memoryGbMin = 4, area = Area.EU, environment = Environment.STAGING, maxMonthlyEur = null,
        estimatedEgressGbPerMonth = 10, cpuProfile = null, architecture = null, storage = null, disk = null, purpose = "staging API",
    )

    fun propose(requirements: VmRequirements? = smallEuStaging) = AgentReply(AgentAction.PROPOSE, "A small EU VM for staging.", requirements)

    fun ask(question: String = "Which environment is this for?") = AgentReply(AgentAction.ASK, question, null)
}
