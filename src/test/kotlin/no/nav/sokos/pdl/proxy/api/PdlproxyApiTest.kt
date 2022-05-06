package no.nav.sokos.pdl.proxy.api

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.http.HttpStatusCode
import io.restassured.RestAssured
import io.restassured.http.Header
import java.net.URL
import no.nav.sokos.pdl.proxy.TestServer
import no.nav.sokos.pdl.proxy.api.model.PersonIdent
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.setupMockEngine
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class PdlproxyApiTest {
    private val validationFilter = OpenApiValidationFilter("spec/sokos-pdl-proxy-v1-swagger2.json")
    private val pdlUrl = "http://0.0.0.0"

    @Test
    fun `Solskinnshistorie - klient kaller tjenest og ingenting skal feile`() {
        val mockkGraphQlClient = GraphQLKtorClient(
            URL(pdlUrl),
            setupMockEngine(
                "hentIdenter_Success_Response.json",
                "hentPerson_Success_Response.json",
                HttpStatusCode.OK)
        )
        val pdlService = PdlService(mockkGraphQlClient, pdlUrl, accessTokenClient = null)

        val server: TestServer = TestServer(8080, pdlService)

        val apiResponse = RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("12345678901").tilJson())
            .port(8080)
            .post("/hent-person")

    }

    @Disabled
    @Test
    fun `Finner ikke person ved søk etter identer i PDL`() {
        val mockkGraphQlClient = GraphQLKtorClient(URL(pdlUrl),
            setupMockEngine(
                "hentIdenter_fant_ikke_person_response.json",
                "hentPerson_FantIkkePerson_Response.json",
                HttpStatusCode.OK)
        )
        val pdlService = PdlService(mockkGraphQlClient, pdlUrl, accessTokenClient = null)

        val server: TestServer = TestServer(8080, pdlService)

        val apiResponse = RestAssured.given()
            .filter(validationFilter)
            .header(Header("Content-Type", "application/json"))
            .header(Header("Authorization", "Bearer dummytoken"))
            .body(PersonIdent("12345678901").tilJson())
            //.port(8081)
            .post("/hent-person")

//        apiResponse
//            .prettyPrint()
//            .then()
//            .statusCode(404)
            //.body("feilmelding", containsString(feilmelding))

    }
}