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

    val pdlProxyApiCallCounter: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("api_pdl_proxy_call_counter")
        .help("Antall kall til pdl proxy api")
        .register(prometheusRegistry.prometheusRegistry)

    val pdlProxyApiCallExceptionCounter: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("api_pdl_proxy_call_exception_counter")
        .help("Antall teknisk feil oppstått")
        .register(prometheusRegistry.prometheusRegistry)

}