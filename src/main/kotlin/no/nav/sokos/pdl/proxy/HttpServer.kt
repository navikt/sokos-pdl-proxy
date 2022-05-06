package no.nav.sokos.pdl.proxy

import installCommonFeatures
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.ContentType
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.prometheus.client.exporter.common.TextFormat
import java.util.concurrent.TimeUnit
import no.nav.kontoregister.person.api.installSecurity
import no.nav.sokos.ereg.proxy.api.naisApi
import no.nav.sokos.ereg.proxy.api.swaggerApi
import no.nav.sokos.pdl.proxy.api.pdlApi
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.metrics.Metrics
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
        pdlApi(pdlService, appConfig.useAuthentication)
        installCommonFeatures()
        installMetrics()
        swaggerApi()
        naisApi({ appState.initialized }, { appState.running })
    }

    fun Application.installMetrics() {
        install(MicrometerMetrics) {
            registry = Metrics.prometheusRegistry
            meterBinders = listOf(
                UptimeMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                JvmThreadMetrics(),
                ProcessorMetrics()
            )
        }
        routing {
            route("metrics") {
                get {
                    call.respondText(ContentType.parse(TextFormat.CONTENT_TYPE_004)) { Metrics.prometheusRegistry.scrape() }
                }
            }
        }
    }
    fun start() = embeddedServer.start()
    fun stop() = embeddedServer.stop(5, 5, TimeUnit.SECONDS)
}
