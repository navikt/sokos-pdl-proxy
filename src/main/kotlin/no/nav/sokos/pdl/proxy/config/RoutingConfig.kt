package no.nav.sokos.pdl.proxy.config

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.sokos.pdl.proxy.ApplicationState
import no.nav.sokos.pdl.proxy.api.metricsApi
import no.nav.sokos.pdl.proxy.api.naisApi
import no.nav.sokos.pdl.proxy.api.pdlProxyApi
import no.nav.sokos.pdl.proxy.api.swaggerApi
import no.nav.sokos.pdl.proxy.pdl.PdlService

fun Application.routingConfig(
    applicationState: ApplicationState,
    pdlService: PdlService,
    useAuthentication: Boolean
) {
    routing {
        naisApi({ applicationState.initialized }, { applicationState.running })
        metricsApi()
        swaggerApi()
        pdlProxyApi(pdlService, useAuthentication)
    }
}