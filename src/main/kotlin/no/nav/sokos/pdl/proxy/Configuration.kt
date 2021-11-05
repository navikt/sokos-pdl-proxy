package no.nav.sokos.pdl.proxy

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.sokos.pdl.proxy.person.security.Api
import no.nav.sokos.pdl.proxy.person.security.PreAuthorizedApp
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.TimeUnit

val LOGGER = LoggerFactory.getLogger("no.nav.sokos.pdl.proxy.Configuration")

data class Configuration (
    val useAuthentication: Boolean = readProperty("USE_AUTHENTICATION", default = "true") != "false",
    //val azureAdServer: Configuration.AzureAdServer = Configuration.AzureAdServer(),
    val azureAdClint: Configuration.AzureAdClient = Configuration.AzureAdClient(),
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
            Api.KONTOREGISTER to readProperty("ALLOW_LIST_KONTOREG", "").split(","),
            Api.FRONTEND to readProperty("ALLOW_LIST_OKONOMIPORTAL", "").split(","),
        ),
    ) {
        val openIdConfiguration: AzureAdOpenIdConfiguration by lazy {
            runBlocking { defaultHttpClient.get(authorityEndpoint) }
        }
        val jwkProvider: JwkProvider by lazy {
            JwkProviderBuilder(URL(openIdConfiguration.jwksUri))
                .cached(10, 24, TimeUnit.HOURS)       // cache up to 10 JWKs for 24 hours
                .rateLimited(10, 1, TimeUnit.MINUTES) // if not cached, only allow max 10 different keys per minute to be
                .build()                              // fetched from external provider
        }
    }

    /*data class DatabaseConfig(
        val host: String = readProperty("NAIS_DATABASE_SOKOS_KONTOREGISTER_PERSON_DB_HOST"),
        val port: String = readProperty("NAIS_DATABASE_SOKOS_KONTOREGISTER_PERSON_DB_PORT"),
        val name: String = readProperty("NAIS_DATABASE_SOKOS_KONTOREGISTER_PERSON_DB_DATABASE"),
        val username: String = readProperty("NAIS_DATABASE_SOKOS_KONTOREGISTER_PERSON_DB_USERNAME"),
        val password: String = readProperty("NAIS_DATABASE_SOKOS_KONTOREGISTER_PERSON_DB_PASSWORD")
    ) {
        val jdbcUrl: String = "jdbc:postgresql://$host:$port/$name"
    }*/

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
        ?: default.takeIf { it != null }?.also { LOGGER.info("Bruker default verdi for property $name") }
        ?: throw RuntimeException("Mandatory property '$name' was not found")
