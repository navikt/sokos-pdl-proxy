package no.nav.sokos.pdl.proxy.api

import io.ktor.server.application.Application
import io.ktor.server.http.content.defaultResource
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.routing.routing

fun Application.swaggerApi() {
    routing {
        static("/person-proxy/api/v1/docs/") {
            resources("api")
            defaultResource("api/index.html")
        }
    }
}