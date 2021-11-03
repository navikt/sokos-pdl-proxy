package no.nav.sokos.pdl.proxy.person.metrics

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.ContentType
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.Counter
import io.prometheus.client.exporter.common.TextFormat

private const val NAMESPACE = "sokos_kontooppslag_person"

object Metrics {
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val appStateRunningFalse: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("app_state_running_false")
        .help("app state running changed to false")
        .register(prometheusRegistry.prometheusRegistry)

    val appStateReadyFalse: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("app_state_ready_false")
        .help("app state ready changed to false")
        .register(prometheusRegistry.prometheusRegistry)

    val eregCallCounter: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("ereg_call_counter")
        .labelNames("responseCode")
        .help("Counts calls to ereg with response status code")
        .register(prometheusRegistry.prometheusRegistry)

    val eregValidationErrorCounter: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("ereg_validation_error_counter")
        .help("Counts validating errors in ereg response")
        .register(prometheusRegistry.prometheusRegistry)


    val databaseFailureCounter: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("database_failure_counter")
        .labelNames("errorCode", "sqlState")
        .help("Count database errors")
        .register(prometheusRegistry.prometheusRegistry)
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