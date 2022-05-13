package no.nav.kontoregister.person.api

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTAuthenticationProvider
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.routing.Route
import no.nav.sokos.pdl.proxy.Configuration
import no.nav.sokos.pdl.proxy.pdl.security.Api
import no.nav.sokos.pdl.proxy.pdl.security.ApiSecurityService
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger("no.nav.kontoregister.person.api")

fun Application.installSecurity(
    apiSecurityService: ApiSecurityService,
    appConfig: Configuration,
    useAuthentication: Boolean = true,
) {
    if (useAuthentication) {
        LOGGER.info("Running with authentication")
        install(Authentication) {
            apiJwt(apiSecurityService, appConfig)
            jwt { azureAuth(appConfig, apiSecurityService) }
        }
    } else LOGGER.warn("Running WITHOUT authentication!")
}

fun Authentication.Configuration.apiJwt(apiSecurityService: ApiSecurityService, appConfig: Configuration) =
    Api.values().forEach { api -> jwt(api.name) { azureAuth(appConfig, apiSecurityService, api) } }

private fun JWTAuthenticationProvider.Configuration.azureAuth(
    appConfig: Configuration,
    apiSecurityService: ApiSecurityService,
    api: Api? = null,
) {
    verifier(appConfig.azureAdServer.jwkProvider, appConfig.azureAdServer.openIdConfiguration.issuer)
    realm = appConfig.appName
    validate { credentials ->
        try {
            requireNotNull(credentials.payload.audience) {
                LOGGER.info("Auth: Missing audience in token")
                "Auth: Missing audience in token"
            }
            require(credentials.payload.audience.contains(appConfig.azureAdServer.clientId)) {
                LOGGER.info("Auth: Valid audience not found in claims")
                "Auth: Valid audience not found in claims"
            }
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

fun Route.authenticate(useAuthentication: Boolean, config: String? = null, block: Route.() -> Unit) {
    if (useAuthentication) authenticate(config) { block() } else block()
}
