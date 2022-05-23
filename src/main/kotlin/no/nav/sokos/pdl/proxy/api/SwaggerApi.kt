package no.nav.sokos.ereg.proxy.api

import io.ktor.application.Application
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.route
import io.ktor.routing.routing

fun Application.swaggerApi() {
    routing {
        static("/person-proxy/api/v1/docs/") {
            resources("api")
            defaultResource("api/index.html")
        }
    }
}