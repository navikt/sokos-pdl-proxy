package no.nav.sokos.pdl.proxy.pdl.security

class ApiSecurityService(
    private val apiAllowLists: Map<Api, List<String>>,
    private val preAutorizedApps: List<PreAuthorizedApp>,
) {
    fun verifyAccessToApi(clientId: String, api: Api): Boolean =
        getPreAuthorizedApp(clientId)?.appName in api.allowList()

    fun getPreAuthorizedApp(clientId: String): PreAuthorizedApp? =
        preAutorizedApps.firstOrNull { it.clientId == clientId }

    private fun Api.allowList() = apiAllowLists[this] ?: emptyList()
}
