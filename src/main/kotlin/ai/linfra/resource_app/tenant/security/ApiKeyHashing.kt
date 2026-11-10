package ai.linfra.resource_app.tenant.security

import java.security.MessageDigest
import java.util.HexFormat

fun hashApiKey(key: String): String =
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.toByteArray()))
