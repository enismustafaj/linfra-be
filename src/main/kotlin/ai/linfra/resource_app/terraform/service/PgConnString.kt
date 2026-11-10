package ai.linfra.resource_app.terraform.service

import java.net.URI
import java.net.URLEncoder

/** The OpenTofu pg backend wants a libpq URL; Spring has a JDBC URL. Same database: Terraform state lives next to Linfra's tables. */
object PgConnString {
    fun fromJdbc(jdbcUrl: String, username: String, password: String): String {
        val uri = URI(jdbcUrl.removePrefix("jdbc:"))
        val credentials = "${enc(username)}:${enc(password)}"
        return "postgres://$credentials@${uri.host}:${if (uri.port == -1) 5432 else uri.port}${uri.path}?sslmode=disable"
    }

    private fun enc(s: String) = URLEncoder.encode(s, Charsets.UTF_8)
}
