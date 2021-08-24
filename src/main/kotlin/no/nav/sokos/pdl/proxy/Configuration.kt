package no.nav.sokos.pdl.proxy

data class Configuration (
        var pdlHost: String = ""
)

fun readProperty(key: String): String {
    return System.getProperty(key) ?: System.getenv(key) ?: throw RuntimeException("Property $key not found")
}
