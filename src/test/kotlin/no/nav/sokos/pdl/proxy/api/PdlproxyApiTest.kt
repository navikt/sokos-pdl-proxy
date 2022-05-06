package no.nav.sokos.pdl.proxy.api

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.http.HttpStatusCode
import io.restassured.RestAssured
import io.restassured.http.Header
import java.net.URL
import kotlin.random.Random
import no.nav.sokos.pdl.proxy.TestServer
import no.nav.sokos.pdl.proxy.api.model.PersonIdent
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.setupMockEngine
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test

internal class PdlproxyApiTest {
    private val validationFilter = OpenApiValidationFilter("spec/sokos-pdl-proxy-v1-swagger2.json")
    private val pdlUrl = "http://0.0.0.0"

    @Test
    fun `Solskinnshistorie - klient kaller tjeneste med suksess som validerer ok mot swagger-kontrakten`() {
        val port = enTilfleldigPort()

        enTestserverMedResponsFraPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_success_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").tilJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(200)
    }

    @Test
    fun `Finner ikke data for hverken hentIdenter eller hentPerson, skal returnere 404 med feilmelding`() {
        val port = enTilfleldigPort()

        enTestserverMedResponsFraPDL(
            port,
            "hentIdenter_fant_ikke_person_response.json",
            "hentPerson_fant_ikke_person_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").tilJson())
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
        val port = enTilfleldigPort()

        enTestserverMedResponsFraPDL(
            port,
            "hentIdenter_success_response.json",
            "hentPerson_fant_ikke_person_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").tilJson())
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
        val port = enTilfleldigPort()

        enTestserverMedResponsFraPDL(
            port,
            "hentIdenter_fant_ikke_person_response.json",
            "hentPerson_success_response.json"
        )

        RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("ikke interessant").tilJson())
            .port(port)
            .post("/hent-person")
            .then()
            .assertThat()
            .statusCode(404)
            .body(
                containsString("Fant ikke person")
            )
    }


    private fun enTestserverMedResponsFraPDL(
        port: Int,
        hentIdenterResponsFilnavn: String,
        hentPersonResponsFilnavn: String,
    ) {
        val mockkGraphQlClient = GraphQLKtorClient(
            URL(pdlUrl),
            setupMockEngine(
                hentIdenterResponsFilnavn,
                hentPersonResponsFilnavn,
                HttpStatusCode.OK)
        )
        val pdlService = PdlService(mockkGraphQlClient, pdlUrl, accessTokenClient = null)
        TestServer(port, pdlService)
    }

    private fun enTilfleldigPort() = Random.nextInt(32000, 42000)
}