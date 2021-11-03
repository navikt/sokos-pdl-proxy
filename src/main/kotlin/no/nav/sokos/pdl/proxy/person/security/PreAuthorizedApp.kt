package no.nav.sokos.pdl.proxy.person.security

class PreAuthorizedApp(
    val name: String,
    val clientId: String,
) {
    val cluster: String
    val namespace: String
    val appName: String

    init {
        val nameParts = name.split(":")
        cluster = nameParts[0]
        namespace = nameParts[1]
        appName = nameParts[2]
    }
}
