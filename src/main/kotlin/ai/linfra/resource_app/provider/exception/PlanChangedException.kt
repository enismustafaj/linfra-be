package ai.linfra.resource_app.provider.exception

import java.util.UUID

class PlanChangedException(intentId: UUID) :
    RuntimeException("Plan for intent $intentId changed since it was approved; nothing applied, re-plan and re-approve")
