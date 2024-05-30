package no.nav.sokos.pdl.proxy.api

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.kotest.core.spec.style.FunSpec
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import io.restassured.RestAssured
import io.restassured.http.Header
import no.nav.sokos.pdl.proxy.api.model.PersonIdent
import no.nav.sokos.pdl.proxy.config.EmbeddedTestServer
import no.nav.sokos.pdl.proxy.config.PDL_PROXY_API_PATH
import no.nav.sokos.pdl.proxy.config.setupMockEngine
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import java.net.URI
import kotlin.random.Random

private val validationFilter = OpenApiValidationFilter("openapi/sokos-pdl-proxy-v1-swagger2.json")
private val accessTokenClient = mockk<AccessTokenClient>()

internal class PdlProxyApiTest : FunSpec({

    beforeEach {
        coEvery { accessTokenClient.hentAccessToken() } returns "token"
    }

    test("klient kaller tjeneste med suksess som validerer ok mot swagger-kontrakten") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(200)
    }

    test("klient kaller begge tjenester med suksess, men ingen navn på person, skal også validerer ok mot swagger-kontrakten") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_tomt_navn_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(200)
    }

    test("finner ikke data for hverken hentIdenter eller hentPerson, skal returnere 404 med feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_fant_ikke_person_response.json",
            "hentPerson_fant_ikke_person_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(404)
            .body(
                containsString("Fant ikke person"),
            )
    }

    test("finner ikke data for hentPerson, skal returnere 404 med feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_fant_ikke_person_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(404)
            .body(
                containsString("Fant ikke person"),
            )
    }

    test("finner ikke data for hentIdenter, skal returnere 404 med feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_fant_ikke_person_response.json",
            "hentPerson_success_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(404)
            .body(
                containsString("Fant ikke person"),
            )
    }

    test("ikke autentisert, skal returnere 500 med feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_ikke_authentisert_response.json",
            "hentPerson_success_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(500)
            .body(
                containsString("Ikke autentisert"),
            )
    }

    test("andre feilkoder fra PDL skal returnere 500 med en beskrivende feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_annen_feilmelding_response.json",
            "hentPerson_success_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(500)
            .body(containsString("En annen feilmelding fra PDL"))
    }

    test("Teste når vi ikke får svar fra PDL, så skal det returneres 500 med en beskrivende feilmelding") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "",
            "",
            HttpStatusCode.NotFound,
        )

        RestAssured.given()
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(500)
            .body(containsString("En teknisk feil har oppstått. Ta kontakt med utviklerne"))
    }

    test("For å begrense datamengde til stormaskin så tillattes maks 3 stk kontaktadresser, og api skal gi feil dersom dette overstiges") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response_med_4_kontaktadresser.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(500)
            .body("melding", equalTo("For mange kontaktadresser. Personen har 4 og overstiger grensen på 3"))
    }

    test("For å begrense datamengde til stormaskin så tillattes maks 2 stk oppholdsadresse, og api skal gi feil dersom dette overstiges") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response_med_3_oppholdsadresser.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .statusCode(500)
            .body("melding", equalTo("For mange oppholdsadresser. Personen har 3 og overstiger grensen på 2"))
    }

    test("x-correlation-id fra request skal følge med tilbake i repons") {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response.json",
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .header(Header("x-correlation-id", "enId123"))
            .body(PersonIdent("ikke interessant"))
            .port(port)
            .post(PDL_PROXY_API_PATH)
            .then()
            .assertThat()
            .header("x-correlation-id", "enId123")
    }
})

private fun testServerWithResponseFromPDL(
    port: Int,
    hentIdenterResponsFilnavn: String,
    hentPersonResponsFilnavn: String,
    httpStatusCode: HttpStatusCode = HttpStatusCode.OK,
) {
    val pdlUrl = "http://0.0.0.0"

    val mockkGraphQlClient =
        GraphQLKtorClient(
            URI(pdlUrl).toURL(),
            setupMockEngine(
                hentIdenterResponsFilnavn,
                hentPersonResponsFilnavn,
                httpStatusCode,
            ),
        )

    EmbeddedTestServer(PdlService(pdlUrl = pdlUrl, graphQlClient = mockkGraphQlClient, accessTokenClient = accessTokenClient), port)
}

private fun randomPort() = Random.nextInt(32000, 42000)
