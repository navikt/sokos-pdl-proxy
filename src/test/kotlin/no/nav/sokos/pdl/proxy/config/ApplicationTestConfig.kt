package no.nav.sokos.pdl.proxy.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import no.nav.sokos.pdl.proxy.TestHelper.readFromResource
import no.nav.sokos.pdl.proxy.api.pdlProxyApi
import no.nav.sokos.pdl.proxy.pdl.PdlService

class EmbeddedTestServer(
    private val pdlService: PdlService,
    private val port: Int = 1100,
) {
    init {
        embeddedServer(Netty, port, module = {
            serverModule()
        }).start()
    }

    private fun Application.serverModule() {
        commonConfig()
        routing {
            authenticate(false) {
                pdlProxyApi(pdlService = pdlService)
            }
        }
        RestAssured.baseURI = "http://localhost"
        RestAssured.basePath = "/api/pdl-proxy/v1"
        RestAssured.port = port
        RestAssured.config =
            RestAssuredConfig.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory { _, _ -> jsonMapper },
            )
    }
}

fun setupMockEngine(
    hentIdenterResponseFilNavn: String?,
    hentPersonResponseFilNavn: String?,
    statusCode: HttpStatusCode = HttpStatusCode.OK,
): HttpClient {
    return HttpClient(
        MockEngine { request ->
            val body = request.body as TextContent
            val content =
                when {
                    body.text.contains("hentIdenter") -> hentIdenterResponseFilNavn
                    else -> hentPersonResponseFilNavn
                }?.readFromResource().orEmpty()

            respond(
                content = content,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                status = statusCode,
            )
        },
    ) {
        expectSuccess = false
    }
}
