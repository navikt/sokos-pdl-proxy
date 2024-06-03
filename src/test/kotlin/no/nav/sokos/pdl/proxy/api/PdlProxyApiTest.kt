package no.nav.sokos.pdl.proxy.api

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.kotest.core.spec.style.FunSpec
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import io.restassured.RestAssured
import no.nav.sokos.pdl.proxy.api.model.IdentRequest
import no.nav.sokos.pdl.proxy.config.APPLICATION_JSON
import no.nav.sokos.pdl.proxy.config.EmbeddedTestServer
import no.nav.sokos.pdl.proxy.config.PDL_PROXY_API_PATH
import no.nav.sokos.pdl.proxy.config.PDL_URL
import no.nav.sokos.pdl.proxy.config.mockedHttpClientEngine
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.security.AccessTokenClient
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import java.net.URI
import kotlin.random.Random

private val validationFilter = OpenApiValidationFilter("openapi/sokos-pdl-proxy-v1-swagger.yaml")
private val accessTokenClient = mockk<AccessTokenClient>()

internal class PdlProxyApiTest : FunSpec({

    beforeEach {
        coEvery { accessTokenClient.hentAccessToken() } returns "token"
    }

    test("Klient kaller PDL med suksess") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(200)
    }

    test("Klient kaller PDL med suksess, men ingen navn på person") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_tomt_navn_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(200)
    }

    test("Finner ikke data for hverken (hentIdenter) eller (hentPerson), skal returnere 404 med feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_fant_ikke_person_response.json",
            "hentPerson_fant_ikke_person_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(404)
            .body(containsString("Fant ikke person"))
    }

    test("Finner ikke data for (hentPerson), skal returnere 404 med feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_fant_ikke_person_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(404)
            .body(containsString("Fant ikke person"))
    }

    test("Finner ikke data for (hentIdenter), skal returnere 404 med feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_fant_ikke_person_response.json",
            "hentPerson_success_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(404)
            .body(containsString("Fant ikke person"))
    }

    test("Klient ikke ikke autentisert mot tjeneste, skal returnere 500 med feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_ikke_authentisert_response.json",
            "hentPerson_success_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(500)
            .body(containsString("Ikke autentisert"))
    }

    test("Feilkoder fra PDL skal returnere 500 med en beskrivende feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_annen_feilmelding_response.json",
            "hentPerson_success_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(500)
            .body(containsString("En annen feilmelding fra PDL"))
    }

    test("Klient får ikke svar fra PDL, skal returnere 500 med en beskrivende feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "",
            "",
            HttpStatusCode.NotFound,
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(500)
            .body(containsString("En teknisk feil har oppstått. Ta kontakt med utviklerne"))
    }

    test("Klient tillater maks 3 stk kontaktadresser, og skal gi feil dersom dette overstiges") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response_med_4_kontaktadresser.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(500)
            .body("melding", equalTo("For mange kontaktadresser. Personen har 4 og overstiger grensen på 3"))
    }

    test("Klient tillater maks 2 stk oppholdsadresse, og skal gi feil dersom dette overstiges") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response_med_3_oppholdsadresser.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(500)
            .body("melding", equalTo("For mange oppholdsadresser. Personen har 3 og overstiger grensen på 2"))
    }

    test("X-Correlation-Id fra request skal følge med tilbake i respons") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(HttpHeaders.ContentType, APPLICATION_JSON)
            .header(HttpHeaders.Authorization, "Bearer dummytoken")
            .header(HttpHeaders.XCorrelationId, "enId123")
            .body(IdentRequest("123456789"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .header(HttpHeaders.XCorrelationId, "enId123")
    }
})

private fun testServerWithResponseFromPDL(
    port: Int,
    hentIdenterResponsFilnavn: String,
    hentPersonResponsFilnavn: String,
    httpStatusCode: HttpStatusCode = HttpStatusCode.OK,
) {
    val mockkGraphQlClient =
        GraphQLKtorClient(
            URI(PDL_URL).toURL(),
            mockedHttpClientEngine(
                hentIdenterResponsFilnavn,
                hentPersonResponsFilnavn,
                httpStatusCode,
            ),
        )

    EmbeddedTestServer(PdlService(pdlUrl = PDL_URL, graphQlClient = mockkGraphQlClient, accessTokenClient = accessTokenClient), port)
}

private fun randomPort() = Random.nextInt(32000, 42000)
