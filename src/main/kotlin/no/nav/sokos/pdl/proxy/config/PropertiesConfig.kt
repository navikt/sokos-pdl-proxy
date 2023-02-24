package no.nav.sokos.pdl.proxy.config

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

data class PropertiesConfig(
    val useAuthentication: Boolean = readProperty("USE_AUTHENTICATION", default = "true") != "false",
    val azureAdServer: AzureAdServer = AzureAdServer(),
    val azureAdClint: AzureAdClient = AzureAdClient(),
    val appName: String = readProperty("NAIS_APP_NAME"),
    val pdlUrl: String = readProperty("PDL_URL"),
) {

    data class AzureAdClient(
        val clientId: String = readProperty("AZURE_APP_CLIENT_ID", ""),
        val authorityEndpoint: String = readProperty("AZURE_APP_WELL_KNOWN_URL", ""),
        val tenant: String = readProperty("AZURE_APP_TENANT_ID", ""),
        val clientSecret: String = readProperty("AZURE_APP_CLIENT_SECRET", ""),
        val pdlClientId: String = readProperty("PDL_CLIENT_ID", "")
    )

    data class AzureAdServer(
        val clientId: String = readProperty("AZURE_APP_CLIENT_ID", ""),
        val authorityEndpoint: String = readProperty("AZURE_APP_WELL_KNOWN_URL", ""),
    )

}

private fun readProperty(name: String, default: String? = null) =
    System.getenv(name)
        ?: System.getProperty(name)
        ?: default.takeIf { it != null }?.also { logger.info("Bruker default verdi for property $name") }
        ?: throw RuntimeException("Mandatory property '$name' was not found")
