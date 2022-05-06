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
    fun `Solskinnshistorie - klient kaller tjeneste og ingenting skal feile`() {
        val port = enTilfleldigPort()

        enTestserverMedResponsFraPDL(
            port,
            "hentIdenter_Success_Response.json",
            "hentPerson_Success_Response.json"
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
    fun `Finner ikke person ved søk etter identer i PDL`() {
        val port = enTilfleldigPort()

        enTestserverMedResponsFraPDL(
            port,
            "hentIdenter_fant_ikke_person_response.json",
            "hentPerson_FantIkkePerson_Response.json"
        )

        RestAssured
            .given()
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