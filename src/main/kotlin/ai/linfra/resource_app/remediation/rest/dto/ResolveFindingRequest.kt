package ai.linfra.resource_app.remediation.rest.dto

import ai.linfra.resource_app.remediation.model.RemediationAction

/** cloudId is only used by KEEP: the resource to keep. */
data class ResolveFindingRequest(val action: RemediationAction, val cloudId: String? = null)
