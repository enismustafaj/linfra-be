package ai.linfra.resource_app.terraform.service

import ai.linfra.resource_app.terraform.exception.UnsafeTerraformValueException

/**
 * Backstop for strings taken from an intent. tfvars values are data and never evaluated, so this is defence in depth:
 * allowing only [A-Za-z0-9._-] rules out template syntax, quotes and newlines even if a value ends up somewhere else.
 */
object TerraformValues {
    private val SAFE = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,62}$")

    fun requireSafe(value: String): String {
        if (!SAFE.matches(value)) throw UnsafeTerraformValueException(value)
        return value
    }
}
