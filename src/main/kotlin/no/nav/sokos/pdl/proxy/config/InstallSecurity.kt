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
import no.nav.sokos.pdl.proxy.pdl.security.Api
import no.nav.sokos.pdl.proxy.pdl.security.ApiSecurityService
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger("no.nav.kontoregister.person.api")

fun Application.installSecurity(
    apiSecurityService: ApiSecurityService,
    applicationConfiguration: ApplicationConfiguration,
    useAuthentication: Boolean = true,
) {
    if (useAuthentication) {
        LOGGER.info("Running with authentication")
        install(Authentication) {
            apiJwt(apiSecurityService, applicationConfiguration)
            jwt { azureAuth(applicationConfiguration, apiSecurityService) }
        }
    } else LOGGER.warn("Running WITHOUT authentication!")
}

fun AuthenticationConfig.apiJwt(apiSecurityService: ApiSecurityService, applicationConfiguration: ApplicationConfiguration) =
    Api.values().forEach { api -> jwt(api.name) { azureAuth(applicationConfiguration, apiSecurityService, api) } }

fun Route.autentiser(brukAutentisering: Boolean, authenticationProviderId: String? = null, block: Route.() -> Unit) {
    if (brukAutentisering) authenticate(authenticationProviderId) { block() } else block()
}

private fun JWTAuthenticationProvider.Config.azureAuth(
    applicationConfiguration: ApplicationConfiguration,
    apiSecurityService: ApiSecurityService,
    api: Api? = null,
) {
    verifier(applicationConfiguration.azureAdServer.jwkProvider, applicationConfiguration.azureAdServer.openIdConfiguration.issuer)
    realm = applicationConfiguration.appName
    validate { credentials ->
        try {
            requireNotNull(credentials.payload.audience) {
                LOGGER.info("Auth: Missing audience in token")
                "Auth: Missing audience in token"
            }
            require(credentials.payload.audience.contains(applicationConfiguration.azureAdServer.clientId)) {
                LOGGER.info("Auth: Valid audience not found in claims")
                "Auth: Valid audience not found in claims"
            }
            //TODO Vi trenger ikke allow list for denne applikasjonen, siden den bare har ett api med ett rest-kall.
            // (så enten har man tilgang eller ikke)
            if (api != null) {
                val azp = credentials.payload.getClaim("azp").asString()
                check(apiSecurityService.verifyAccessToApi(azp, api)) {
                    val client = apiSecurityService.getPreAuthorizedApp(azp)
                    LOGGER.warn("${client?.appName} har forsøkt å nå $api API. Tilgang nektet")
                    "Auth: Client does not have access to API"
                }
            }
            JWTPrincipal(credentials.payload)
        } catch (e: Throwable) {
            null
        }
    }
}
