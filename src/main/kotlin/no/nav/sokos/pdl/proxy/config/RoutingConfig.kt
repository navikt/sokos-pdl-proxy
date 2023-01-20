package no.nav.sokos.pdl.proxy.config

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import no.nav.sokos.pdl.proxy.api.metricsApi
import no.nav.sokos.pdl.proxy.api.naisApi
import no.nav.sokos.pdl.proxy.api.pdlProxyV1Api
import no.nav.sokos.pdl.proxy.api.swaggerApi
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.util.ApplicationState

fun Application.routingConfig(
    applicationState: ApplicationState,
    pdlService: PdlService,
    useAuthentication: Boolean
) {
    routing {
        naisApi({ applicationState.alive }, { applicationState.ready })
        metricsApi()
        swaggerApi()
        pdlProxyV1Api(pdlService, useAuthentication)
    }
}

fun Route.autentiser(brukAutentisering: Boolean, authenticationProviderId: String? = null, block: Route.() -> Unit) {
    if (brukAutentisering) authenticate(authenticationProviderId) { block() } else block()
}