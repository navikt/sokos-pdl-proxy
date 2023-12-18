package no.nav.sokos.pdl.proxy.api

import EmbeddedTestServer
import com.atlassian.oai.validator.restassured.OpenApiValidationFilter
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.http.HttpStatusCode
import io.restassured.RestAssured
import io.restassured.http.Header
import java.net.URI
import kotlin.random.Random
import no.nav.sokos.pdl.proxy.ApplicationState
import no.nav.sokos.pdl.proxy.api.model.PersonIdent
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.setupMockEngine
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.containsStringIgnoringCase
import org.junit.jupiter.api.Test
import toJson

internal class PdlProxyApiTest {
    private val validationFilter = OpenApiValidationFilter("openapi/sokos-pdl-proxy-v1-swagger2.json")
    private val pdlUrl = "http://0.0.0.0"

    @Test
    fun `Klient kaller tjeneste med suksess som validerer ok mot swagger-kontrakten`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(200)
    }

    @Test
    fun `Klient kaller begge tjenester med suksess, men ingen navn på person, skal også validerer ok mot swagger-kontrakten`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_tomt_navn_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(200)
    }

    @Test
    fun `Finner ikke data for hverken hentIdenter eller hentPerson, skal returnere 404 med feilmelding`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_fant_ikke_person_response.json",
            "hentPerson_fant_ikke_person_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(404)
            .body(
                containsString("Fant ikke person")
            )
    }

    @Test
    fun `Finner ikke data for hentPerson, skal returnere 404 med feilmelding`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_fant_ikke_person_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(404)
            .body(
                containsString("Fant ikke person")
            )
    }

    @Test
    fun `Finner ikke data for hentIdenter, skal returnere 404 med feilmelding`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_fant_ikke_person_response.json",
            "hentPerson_success_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(404)
            .body(
                containsString("Fant ikke person")
            )
    }

    @Test
    fun `ikke autentisert, skal returnere 500 med feilmelding`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_ikke_authentisert_response.json",
            "hentPerson_success_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(500)
            .body(
                containsString("Ikke autentisert")
            )

    }

    @Test
    fun `Andre feilkoder fra PDL skal returnere 500 med en beskrivende feilmelding`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_annen_feilmelding_response.json",
            "hentPerson_success_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(500)
            .body(containsString("En annen feilmelding fra PDL"))
    }

    @Test
    fun `Teste når vi ikke får svar fra PDL, så skal det returneres 500 med en beskrivende feilmelding`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            null,
            null,
            HttpStatusCode.NotFound
        )

        RestAssured.given()
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(500)
            .body(containsString("En teknisk feil har oppstått. Ta kontakt med utviklerne"))
    }

    @Test
    internal fun `For å begrense datamengde til stormaskin så tillattes maks 3 stk kontaktadresser, og api skal gi feil dersom dette overstiges`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response_med_4_kontaktadresser.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(500)
            .body(
                containsStringIgnoringCase("For mange kontaktadresser"),
                containsStringIgnoringCase("Personen har 4"),
                containsStringIgnoringCase("overstiger grensen på 3")
            )

    }

    @Test
    internal fun `For å begrense datamengde til stormaskin så tillattes maks 2 stk oppholdsadresse, og api skal gi feil dersom dette overstiges`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response_med_3_oppholdsadresser.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(500)
            .body(
                containsStringIgnoringCase("For mange oppholdsadresser"),
                containsStringIgnoringCase("Personen har 3"),
                containsStringIgnoringCase("overstiger grensen på 2")
            )

    }

    @Test
    fun `x-correlation-id fra request skal følge med tilbake i repons`() {
        val port = randomPort()

        testServerWithResponseFromPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .header(Header("x-correlation-id", "enId123"))
            .body(PersonIdent("ikke interessant").toJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .header("x-correlation-id", "enId123")
    }

    private fun testServerWithResponseFromPDL(
        port: Int,
        hentIdenterResponsFilnavn: String?,
        hentPersonResponsFilnavn: String?,
        httpStatusCode: HttpStatusCode = HttpStatusCode.OK
    ) {
        val mockkGraphQlClient = GraphQLKtorClient(
            URI(pdlUrl).toURL(),
            setupMockEngine(
                hentIdenterResponsFilnavn,
                hentPersonResponsFilnavn,
                httpStatusCode
            )
        )

        EmbeddedTestServer(port, PdlService(mockkGraphQlClient, pdlUrl, accessTokenClient = null), ApplicationState())
    }

    private fun randomPort() = Random.nextInt(32000, 42000)
}