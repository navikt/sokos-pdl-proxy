package no.nav.sokos.pdl.proxy

import no.nav.sokos.pdl.proxy.config.installCommonFeatures
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import no.nav.sokos.pdl.proxy.config.jsonMapper
import no.nav.sokos.pdl.proxy.config.routingConfig
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.util.ApplicationState

class TestServer(
    private val port: Int = 1100,
    private val pdlService: PdlService,
    applicationState: ApplicationState,
) {
    init {
        embeddedServer(Netty, port) {
            installCommonFeatures()
            routingConfig(applicationState, pdlService, false)
            RestAssured.baseURI = "http://localhost"
            RestAssured.basePath = "/api/pdl-proxy/v1"
            RestAssured.port = port
            RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory { _, _ -> jsonMapper }
            )
        }.start()
    }

}
