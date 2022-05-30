package no.nav.sokos.pdl.proxy

import installCommonFeatures
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import java.util.concurrent.TimeUnit
import no.nav.kontoregister.person.api.installSecurity
import no.nav.sokos.ereg.proxy.api.naisApi
import no.nav.sokos.ereg.proxy.api.swaggerApi
import no.nav.sokos.pdl.proxy.api.PdlproxyApi.pdlproxyV1Api
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.metrics.installMetrics
import no.nav.sokos.pdl.proxy.pdl.metrics.metricsApi
import no.nav.sokos.pdl.proxy.pdl.security.ApiSecurityService


class HttpServer(
    appState: ApplicationState,
    appConfig: Configuration,
    pdlService: PdlService,
    apiSecurityService: ApiSecurityService,
    port: Int = 8080,
) {
    private val embeddedServer = embeddedServer(Netty, port) {
        installSecurity(apiSecurityService, appConfig, appConfig.useAuthentication)
        pdlproxyV1Api(pdlService, appConfig.useAuthentication)
        installCommonFeatures()
        installMetrics()
        metricsApi()
        swaggerApi()
        naisApi({ appState.alive }, { appState.ready })
    }

    fun start() = embeddedServer.start(wait = true)
    fun stop() = embeddedServer.stop(5, 5, TimeUnit.SECONDS)
}
