import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import no.nav.sokos.pdl.proxy.ApplicationState
import no.nav.sokos.pdl.proxy.config.commonConfig
import no.nav.sokos.pdl.proxy.config.routingConfig
import no.nav.sokos.pdl.proxy.pdl.PdlService

private fun String.readFromResource() = {}::class.java.classLoader.getResource(this)!!.readText()

fun Any.toJson() = jsonMapper().writeValueAsString(this)!!

private fun jsonMapper(): ObjectMapper =
    jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        findAndRegisterModules()
    }

class EmbeddedTestServer(
    private val port: Int = 1100,
    private val pdlService: PdlService,
    private val applicationState: ApplicationState,
) {
    init {
        embeddedServer(Netty, port, module = {
            applicationModule(pdlService, applicationState)
        }).start()
    }

    private fun Application.applicationModule(
        pdlService: PdlService,
        applicationState: ApplicationState,
    ) {
        commonConfig()
        routingConfig(applicationState, pdlService, false)
        RestAssured.baseURI = "http://localhost"
        RestAssured.basePath = "/api/pdl-proxy/v1"
        RestAssured.port = port
        RestAssured.config =
            RestAssuredConfig.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory { _, _ -> no.nav.sokos.pdl.proxy.util.jsonMapper },
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
