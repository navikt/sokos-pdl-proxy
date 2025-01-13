package no.nav.sokos.pdl.proxy.config

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing

import no.nav.sokos.pdl.proxy.api.pdlProxyApi
import no.nav.sokos.pdl.proxy.api.swaggerApi

fun Application.routingConfig(
    useAuthentication: Boolean,
    applicationState: ApplicationState,
) {
    routing {
        internalNaisRoutes(applicationState)
        swaggerApi()
        authenticate(useAuthentication, AUTHENTICATION_NAME) {
            pdlProxyApi()
        }
    }
}

fun Route.authenticate(
    useAuthentication: Boolean,
    authenticationProviderId: String? = null,
    block: Route.() -> Unit,
) {
    if (useAuthentication) authenticate(authenticationProviderId) { block() } else block()
}
