package no.nav.sokos.pdl.proxy.config

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.call.body
import io.ktor.client.request.get
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.sokos.pdl.proxy.pdl.security.Api
import no.nav.sokos.pdl.proxy.pdl.security.PreAuthorizedApp
import no.nav.sokos.pdl.proxy.util.httpClient
import no.nav.sokos.pdl.proxy.util.jsonMapper

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
        val preAutorizedApps: List<PreAuthorizedApp> =
            readProperty("AZURE_APP_PRE_AUTHORIZED_APPS", "[]").let { jsonMapper.readValue(it) },
        val apiAllowLists: Map<Api, List<String>> = mapOf(
            Api.PDLPROXY to readProperty("ALLOW_LIST_PDLPROXY", "").split(","),
        ),
    ) {
        val openIdConfiguration: AzureAdOpenIdConfiguration by lazy {
            runBlocking { httpClient.get(authorityEndpoint).body() }
        }
        val jwkProvider: JwkProvider by lazy {
            JwkProviderBuilder(URL(openIdConfiguration.jwksUri))
                .cached(10, 24, TimeUnit.HOURS)       // cache up to 10 JWKs for 24 hours
                .rateLimited(
                    10,
                    1,
                    TimeUnit.MINUTES
                ) // if not cached, only allow max 10 different keys per minute to be
                .build()                              // fetched from external provider
        }
    }

    data class AzureAdOpenIdConfiguration(
        @JsonProperty("jwks_uri")
        val jwksUri: String,
        @JsonProperty("issuer")
        val issuer: String,
        @JsonProperty("token_endpoint")
        val tokenEndpoint: String,
        @JsonProperty("authorization_endpoint")
        val authorizationEndpoint: String
    )
}

private fun readProperty(name: String, default: String? = null) =
    System.getenv(name)
        ?: System.getProperty(name)
        ?: default.takeIf { it != null }?.also { logger.info("Bruker default verdi for property $name") }
        ?: throw RuntimeException("Mandatory property '$name' was not found")
