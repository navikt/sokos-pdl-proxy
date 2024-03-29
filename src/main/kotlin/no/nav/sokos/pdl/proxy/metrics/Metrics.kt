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

    var multipleAktiveNavnCounter: Counter = Counter.build()
            .namespace(NAMESPACE)
            .name("count_flere_aktive_navn_personer")
            .help("Antall personer som har flere aktive navn")
            .register(prometheusRegistry.prometheusRegistry)

    var noAktivtNavnCounter: Counter = Counter.build()
            .namespace(NAMESPACE)
            .name("count_kun_historiske_navn_personer")
            .help("Antall personer som har kun historiske navn")
            .register(prometheusRegistry.prometheusRegistry)

}