package no.nav.sokos.pdl.proxy.config

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.request.get
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
const val AUTHENTICATION_NAME = "azureAd"

fun Application.installSecurity(
    configuration: Configuration,
    useAuthentication: Boolean = true,
) {
    if (useAuthentication) {
        logger.info("Running with authentication")
        val openIdMetadata: OpenIdMetadata = wellKnowConfig(configuration.azureAdServer.authorityEndpoint)
        val jwkProvider = cachedJwkProvider(openIdMetadata.jwksUri)
        authentication {
            jwt(AUTHENTICATION_NAME) {
                realm = configuration.appName
                verifier(
                    jwkProvider = jwkProvider,
                    issuer = openIdMetadata.issuer
                )
                validate { credential ->
                    try {
                        requireNotNull(credential.payload.audience) {
                            logger.info("Auth: Missing audience in token")
                            "Auth: Missing audience in token"
                        }
                        require(credential.payload.audience.contains(configuration.azureAdServer.clientId)) {
                            logger.info("Auth: Valid audience not found in claims")
                            "Auth: Valid audience not found in claims"
                        }
                        JWTPrincipal(credential.payload)
                    } catch (e: Exception) {
                        logger.warn(e) { "Client authentication failed" }
                        null
                    }
                }
            }
        }


    } else logger.warn("Running WITHOUT authentication!")
}

private fun cachedJwkProvider(jwksUri: String): JwkProvider {
    val jwkProviderBuilder = JwkProviderBuilder(URL(jwksUri))
    System.getenv("HTTP_PROXY")?.let {
        jwkProviderBuilder.proxied(ProxyBuilder.http(it))
    }

    return jwkProviderBuilder
        .cached(10, 24, TimeUnit.HOURS) // cache up to 10 JWKs for 24 hours
        .rateLimited(
            10,
            1,
            TimeUnit.MINUTES
        ) // if not cached, only allow max 10 different keys per minute to be fetched from external provider
        .build()
}

data class OpenIdMetadata(
    @JsonProperty("jwks_uri") val jwksUri: String,
    @JsonProperty("issuer") val issuer: String,
    @JsonProperty("token_endpoint") val tokenEndpoint: String,
)

private fun wellKnowConfig(wellKnownUrl: String): OpenIdMetadata {
    val openIdMetadata: OpenIdMetadata by lazy {
        runBlocking { httpClient.get(wellKnownUrl).body() }
    }
    return openIdMetadata
}
