package no.nav.sokos.pdl.proxy.pdl.security

class ApiSecurityService(
    private val preAutorizedApps: List<PreAuthorizedApp>,
) {
    fun getPreAuthorizedApp(clientId: String): PreAuthorizedApp? =
        preAutorizedApps.firstOrNull { it.clientId == clientId }
}
