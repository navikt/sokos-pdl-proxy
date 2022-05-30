package no.nav.sokos.pdl.proxy.pdl.metrics

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.prometheus.client.exporter.common.TextFormat

fun Application.metricsApi() {
    routing {
        route("metrics") {
            get {
                call.respondText(ContentType.parse(TextFormat.CONTENT_TYPE_004)) { Metrics.prometheusRegistry.scrape() }
            }
        }
    }
}