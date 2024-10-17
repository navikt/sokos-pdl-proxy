package no.nav.sokos.pdl.proxy.api

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
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
import no.nav.sokos.pdl.proxy.APPLICATION_JSON
import no.nav.sokos.pdl.proxy.PDL_PROXY_API_PATH
import no.nav.sokos.pdl.proxy.TestData.mockPersonDetaljer
import no.nav.sokos.pdl.proxy.api.model.IdentRequest
import no.nav.sokos.pdl.proxy.config.AUTHENTICATION_NAME
import no.nav.sokos.pdl.proxy.config.PdlApiException
import no.nav.sokos.pdl.proxy.config.authenticate
import no.nav.sokos.pdl.proxy.config.commonConfig
import no.nav.sokos.pdl.proxy.pdl.PdlService
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers

private const val PORT = 9090

private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

private val validationFilter = OpenApiValidationFilter("openapi/sokos-pdl-proxy-v1-swagger.yaml")
private val pdlService = mockk<PdlService>(relaxed = true)

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

        response.shouldNotBeNull()
    }

    test("Klient kaller PDL, ingen person finnes, skal returnere 404 med feilmelding") {

        every { pdlService.hentPersonDetaljer(any()) } throws PdlApiException(404, "Fant ikke person")

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
            .body("melding", Matchers.equalTo("Fant ikke person"))
    }

    test("Klient ikke ikke autentisert mot PDL, skal returnere 500 med feilmelding") {

        every { pdlService.hentPersonDetaljer(any()) } throws PdlApiException(500, "Ikke autentisert")

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
    }

    test("Feilkoder fra PDL skal returnere 500 med en beskrivende feilmelding") {

        every { pdlService.hentPersonDetaljer(any()) } throws PdlApiException(500, "En annen feilmelding fra PDL")

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
            .body(containsString("En annen feilmelding fra PDL"))
    }

    test("Klient får ikke svar fra PDL, skal returnere 500 med en beskrivende feilmelding") {

        every { pdlService.hentPersonDetaljer(any()) } throws PdlApiException(500, "En teknisk feil har oppstått. Ta kontakt med utviklerne")

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
            .body(containsString("En teknisk feil har oppstått. Ta kontakt med utviklerne"))
    }

    test("Klient tillater maks 3 stk kontaktadresser, og skal gi feil dersom dette overstiges") {

        every { pdlService.hentPersonDetaljer(any()) } throws PdlApiException(500, "For mange kontaktadresser. Personen har 4 og overstiger grensen på 3")

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
            .body("melding", equalTo("For mange kontaktadresser. Personen har 4 og overstiger grensen på 3"))
    }

    test("Klient tillater maks 2 stk oppholdsadresse, og skal gi feil dersom dette overstiges") {

        every { pdlService.hentPersonDetaljer(any()) } throws PdlApiException(500, "For mange oppholdsadresser. Personen har 3 og overstiger grensen på 2")

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
            .body("melding", equalTo("For mange oppholdsadresser. Personen har 3 og overstiger grensen på 2"))
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
