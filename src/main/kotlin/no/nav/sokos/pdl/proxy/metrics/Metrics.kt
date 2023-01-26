package no.nav.sokos.pdl.proxy.metrics

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

    var allNamesCounter: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("count_to_aktivt_navn_personer")
        .help("The number of persons having two active names")
        .register(prometheusRegistry.prometheusRegistry)

    var fregNamesCounter: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("count_to_aktivt_navn_personer_freg_ibruk")
        .help("The number of persons using FREG name and having two active names\"")
        .register(prometheusRegistry.prometheusRegistry)

    var pdlNamesCounter: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("count_to_aktivt_navn_personer_pdl_ibruk")
        .help("The number of persons using PDL name and having two active names")
        .register(prometheusRegistry.prometheusRegistry)

}