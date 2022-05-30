package no.nav.sokos.pdl.proxy.pdl.metrics

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.Counter

private const val NAMESPACE = "sokos_pdl_proxy"

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