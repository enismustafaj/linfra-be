package ai.linfra.resource_app.terraform

import ai.linfra.resource_app.terraform.service.PgConnString
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PgConnStringTest {

    @Test
    fun `converts a JDBC URL to a libpq URL, encoding credentials`() {
        assertEquals(
            "postgres://linfra:p%40ss%3Aword@localhost:5432/linfra?sslmode=disable",
            PgConnString.fromJdbc("jdbc:postgresql://localhost:5432/linfra", "linfra", "p@ss:word"),
        )
    }

    @Test
    fun `defaults the port and drops JDBC parameters`() {
        assertEquals(
            "postgres://u:p@db/test?sslmode=disable",
            PgConnString.fromJdbc("jdbc:postgresql://db/test?loggerLevel=OFF", "u", "p").replace(":5432", ""),
        )
    }
}
