package no.nav.sokos.pdl.proxy.api

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.routing.routing
import io.mockk.every
import io.mockk.mockk
import io.restassured.RestAssured
import kotlinx.serialization.json.Json
import no.nav.sokos.pdl.proxy.APPLICATION_JSON
import no.nav.sokos.pdl.proxy.PDL_PROXY_API_PATH
import no.nav.sokos.pdl.proxy.TestData.mockPersonDetaljer
import no.nav.sokos.pdl.proxy.api.model.IdentRequest
import no.nav.sokos.pdl.proxy.config.AUTHENTICATION_NAME
import no.nav.sokos.pdl.proxy.config.PdlApiException
import no.nav.sokos.pdl.proxy.config.authenticate
import no.nav.sokos.pdl.proxy.config.commonConfig
import no.nav.sokos.pdl.proxy.domain.PersonDetaljer
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.util.TjenestefeilResponse
import org.hamcrest.CoreMatchers.containsString

private const val PORT = 9090

private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

private val validationFilter = OpenApiValidationFilter("openapi/sokos-pdl-proxy-v1-swagger.yaml")
private val pdlService = mockk<PdlService>()

internal class PdlProxyApiTest : FunSpec({

    beforeTest {
        server = embeddedServer(Netty, PORT, module = Application::applicationTestModule).start()
    }

    afterTest {
        server.stop(5, 5)
    }

    test("Klient kaller PDL med suksess") {

        every { pdlService.hentPersonDetaljer(any()) } returns mockPersonDetaljer()

        val response =
            RestAssured.given()
                .filter(validationFilter)
                .header(HttpHeaders.ContentType, APPLICATION_JSON)
                .header(HttpHeaders.Authorization, "Bearer dummytoken")
                .body(IdentRequest("123456789"))
                .port(PORT)
                .post(PDL_PROXY_API_PATH)
                .then().assertThat()
                .statusCode(HttpStatusCode.OK.value)
                .extract()
                .response()

        Json.decodeFromString<PersonDetaljer>(response.asString()) shouldBe mockPersonDetaljer()
    }

    test("Klient kaller PDL, ingen person finnes, skal returnere 404 med feilmelding") {

        every { pdlService.hentPersonDetaljer(any()) } throws PdlApiException(404, "Fant ikke person")

        val response =
            RestAssured.given()
                .filter(validationFilter)
                .header(HttpHeaders.ContentType, APPLICATION_JSON)
                .header(HttpHeaders.Authorization, "Bearer dummytoken")
                .body(IdentRequest("123456789"))
                .port(PORT)
                .post(PDL_PROXY_API_PATH)
                .then()
                .assertThat()
                .statusCode(404)
                .extract()
                .response()

        Json.decodeFromString<TjenestefeilResponse>(response.asString()) shouldBe
            TjenestefeilResponse(
                "Fant ikke person",
            )
    }

    test("Klient ikke ikke autentisert mot PDL, skal returnere 500 med feilmelding") {

        every { pdlService.hentPersonDetaljer(any()) } throws PdlApiException(500, "Ikke autentisert")

        val response =
            RestAssured.given()
                .filter(validationFilter)
                .header(HttpHeaders.ContentType, APPLICATION_JSON)
                .header(HttpHeaders.Authorization, "Bearer dummytoken")
                .body(IdentRequest("123456789"))
                .port(PORT)
                .post(PDL_PROXY_API_PATH)
                .then()
                .assertThat()
                .statusCode(HttpStatusCode.InternalServerError.value)
                .body(containsString("Ikke autentisert"))
                .extract()
                .response()

        Json.decodeFromString<TjenestefeilResponse>(response.asString()) shouldBe
            TjenestefeilResponse(
                "Ikke autentisert",
            )
    }
})

private fun Application.applicationTestModule() {
    commonConfig()
    routing {
        authenticate(false, AUTHENTICATION_NAME) {
            pdlProxyApi(pdlService)
        }
    }
}
