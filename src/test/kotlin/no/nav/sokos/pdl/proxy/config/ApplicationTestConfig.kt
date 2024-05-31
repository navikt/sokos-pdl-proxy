package no.nav.sokos.pdl.proxy.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.sokos.pdl.proxy.TestHelper.readFromResource
import no.nav.sokos.pdl.proxy.api.pdlProxyApi
import no.nav.sokos.pdl.proxy.pdl.PdlService

const val APPLICATION_JSON = "application/json"
const val PDL_PROXY_API_PATH = "/api/pdl-proxy/v1/hent-person"

class EmbeddedTestServer(
    private val pdlService: PdlService,
    port: Int = 1100,
) {
    init {
        embeddedServer(Netty, port, module = {
            commonConfig()
            routing {
                authenticate(false) {
                    pdlProxyApi(pdlService = pdlService)
                }
            }
        }).start()
    }
}

fun setupMockEngine(
    hentIdenterResponseFilNavn: String,
    hentPersonResponseFilNavn: String,
    statusCode: HttpStatusCode = HttpStatusCode.OK,
): HttpClient {
    return HttpClient(
        MockEngine { request ->
            val body = request.body as TextContent
            val content =
                when {
                    body.text.contains("hentIdenter") -> hentIdenterResponseFilNavn
                    else -> hentPersonResponseFilNavn
                }.readFromResource()

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

fun ApplicationTestBuilder.configureTestApplication() {
    val mapApplicationConfig = MapApplicationConfig()
    environment {
        config = mapApplicationConfig
    }

    application {
        commonConfig()
    }
}