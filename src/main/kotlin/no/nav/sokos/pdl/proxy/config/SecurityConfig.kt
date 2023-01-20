package no.nav.sokos.pdl.proxy.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.routing.Route
import mu.KotlinLogging
import no.nav.sokos.pdl.proxy.pdl.security.Api
import no.nav.sokos.pdl.proxy.pdl.security.ApiSecurityService

private val logger = KotlinLogging.logger {}

fun Application.securityConfig(
    apiSecurityService: ApiSecurityService,
    propertiesConfig: PropertiesConfig,
    useAuthentication: Boolean = true,
) {
    if (useAuthentication) {
        logger.info("Running with authentication")
        install(Authentication) {
            apiJwt(apiSecurityService, propertiesConfig)
            jwt { azureAuth(propertiesConfig, apiSecurityService) }
        }
    } else logger.warn("Running WITHOUT authentication!")
}

fun AuthenticationConfig.apiJwt(apiSecurityService: ApiSecurityService, propertiesConfig: PropertiesConfig) =
    Api.values().forEach { api -> jwt(api.name) { azureAuth(propertiesConfig, apiSecurityService, api) } }

fun Route.autentiser(brukAutentisering: Boolean, authenticationProviderId: String? = null, block: Route.() -> Unit) {
    if (brukAutentisering) authenticate(authenticationProviderId) { block() } else block()
}

private fun JWTAuthenticationProvider.Config.azureAuth(
    propertiesConfig: PropertiesConfig,
    apiSecurityService: ApiSecurityService,
    api: Api? = null,
) {
    verifier(propertiesConfig.azureAdServer.jwkProvider, propertiesConfig.azureAdServer.openIdConfiguration.issuer)
    realm = propertiesConfig.appName
    validate { credentials ->
        try {
            requireNotNull(credentials.payload.audience) {
                logger.info("Auth: Missing audience in token")
                "Auth: Missing audience in token"
            }
            require(credentials.payload.audience.contains(propertiesConfig.azureAdServer.clientId)) {
                logger.info("Auth: Valid audience not found in claims")
                "Auth: Valid audience not found in claims"
            }
            if (api != null) {
                val azp = credentials.payload.getClaim("azp").asString()
                check(apiSecurityService.verifyAccessToApi(azp, api)) {
                    val client = apiSecurityService.getPreAuthorizedApp(azp)
                    logger.warn("${client?.appName} har forsøkt å nå $api API. Tilgang nektet")
                    "Auth: Client does not have access to API"
                }
            }
            JWTPrincipal(credentials.payload)
        } catch (e: Throwable) {
            null
        }
    }
}
