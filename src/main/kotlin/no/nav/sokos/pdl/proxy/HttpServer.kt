package no.nav.sokos.pdl.proxy

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.metrics.micrometer.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.prometheus.client.exporter.common.TextFormat
import no.nav.sokos.ereg.proxy.api.installCommonFeatures
import no.nav.sokos.ereg.proxy.api.naisApi
import no.nav.sokos.ereg.proxy.api.swaggerApi
import no.nav.sokos.pdl.proxy.api.installSecurity
import no.nav.sokos.pdl.proxy.api.pdlApi
import no.nav.sokos.pdl.proxy.person.metrics.Metrics
import no.nav.sokos.pdl.proxy.person.pdl.PdlService
import no.nav.sokos.pdl.proxy.person.pdl.PdlServiceImpl
import no.nav.sokos.pdl.proxy.person.security.ApiSecurityService
import java.util.concurrent.TimeUnit


class HttpServer(
    appState: ApplicationState,
    appConfig: Configuration,
    pdlService: PdlService,
    apiSecurityService: ApiSecurityService,
    port: Int = 8080,
) {
    private val embeddedServer = embeddedServer(Netty, port) {
        installSecurity(apiSecurityService, appConfig, appConfig.useAuthentication)
        pdlApi(pdlService as PdlServiceImpl)
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
