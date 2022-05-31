package no.nav.sokos.pdl.proxy

import kotlin.properties.Delegates
import no.nav.sokos.pdl.proxy.metrics.Metrics

class ApplicationState(
    alive: Boolean = true, ready: Boolean = false
) {
    var alive: Boolean by Delegates.observable(alive) { _, _, newValue ->
        if (!newValue) Metrics.appStateReadyFalse.inc()
    }
    var ready: Boolean by Delegates.observable(ready) { _, _, newValue ->
        if (!newValue) Metrics.appStateRunningFalse.inc()
    }
}