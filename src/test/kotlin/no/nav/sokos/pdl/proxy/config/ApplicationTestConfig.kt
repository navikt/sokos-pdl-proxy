package no.nav.sokos.pdl.proxy.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
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
const val PDL_URL = "http://0.0.0.0"

fun ApplicationTestBuilder.configureTestApplication() {
    val mapApplicationConfig = MapApplicationConfig()
    environment {
        config = mapApplicationConfig
    }

    application {
        commonConfig()
    }
}

class EmbeddedTestServer(
    private val pdlService: PdlService,
    port: Int = 9100,
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

fun mockedHttpClientEngine(
    hentIdenterResponseFilNavn: String,
    hentPersonResponseFilNavn: String,
    statusCode: HttpStatusCode = HttpStatusCode.OK,
): HttpClient {
    return HttpClient(MockEngine) {
        expectSuccess = false
        engine {
            addHandler { request ->
                val body = request.body as TextContent
                val responseFile = if (body.text.contains("hentIdenter")) hentIdenterResponseFilNavn else hentPersonResponseFilNavn
                val content = responseFile.readFromResource()

                respond(
                    content = content,
                    status = statusCode,
                    headers = headersOf(HttpHeaders.ContentType, APPLICATION_JSON),
                )
            }
        }
    }
}
