package no.nav.sokos.pdl.proxy

import installCommonFeatures
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import no.nav.sokos.pdl.proxy.api.pdlApi
import no.nav.sokos.pdl.proxy.pdl.PdlService

class TestServer(
    private val port: Int = 1100,
    val pdlService: PdlService,
) {
    init {
        embeddedServer(Netty, port) {
            installCommonFeatures()
            pdlApi(pdlService, false)
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
