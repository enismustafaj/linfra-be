package ai.linfra.resource_app.remediation.exception

/** The finding is no longer open, doesn't offer the action, or its context changed since it was listed. */
class ActionNotAllowedException(reason: String) : RuntimeException(reason)
