package ai.linfra.resource_app.intent.exception

/** The intent isn't in a state that allows this (e.g. approving one that isn't waiting for approval). */
class IntentNotActionableException(reason: String) : RuntimeException(reason)
