package no.nav.sokos.pdl.proxy.api

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route

fun Route.swaggerApi() {
    swaggerUI(path = "api/pdl-proxy/v1/docs", swaggerFile = "openapi/sokos-pdl-proxy-v1-swagger2.json")
}
