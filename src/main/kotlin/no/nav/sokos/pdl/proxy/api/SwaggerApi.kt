package no.nav.sokos.pdl.proxy.api

import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing

fun Application.swaggerApi() {
    routing {
        swaggerUI(path = "api/v1/docs", swaggerFile = "openapi/sokos-skattekort-person-v1-swagger.yaml")
    }
}