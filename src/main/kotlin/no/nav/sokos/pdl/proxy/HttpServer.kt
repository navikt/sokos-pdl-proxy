package no.nav.sokos.pdl.proxy

import installCommonFeatures
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
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
import no.nav.sokos.pdl.proxy.api.PdlproxyApi.pdlproxyV1Api
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
        pdlproxyV1Api(pdlService, appConfig.useAuthentication)
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
    fun start() = embeddedServer.start(wait = true)
    fun stop() = embeddedServer.stop(5, 5, TimeUnit.SECONDS)
}
