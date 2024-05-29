package no.nav.sokos.pdl.proxy.metrics

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter

private const val NAMESPACE = "sokos_pdl_proxy"

private const val MULTIPLE_AKTIVE_NAVN_COUNTER = "${NAMESPACE}_count_flere_aktive_navn_personer"
private const val NO_AKTIVT_NAVN_COUNTER = "${NAMESPACE}_count_kun_historiske_navn_personer"

object Metrics {
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    var multipleAktiveNavnCounter: Counter =
        Counter.builder()
            .name(MULTIPLE_AKTIVE_NAVN_COUNTER)
            .help("Antall personer som har flere aktive navn")
            .withoutExemplars()
            .register(prometheusRegistry.prometheusRegistry)

    var noAktivtNavnCounter: Counter =
        Counter.builder()
            .name(NO_AKTIVT_NAVN_COUNTER)
            .help("Antall personer som har kun historiske navn")
            .withoutExemplars()
            .register(prometheusRegistry.prometheusRegistry)
}
