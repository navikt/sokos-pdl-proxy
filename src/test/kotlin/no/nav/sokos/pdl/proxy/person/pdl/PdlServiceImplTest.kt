package no.nav.sokos.pdl.proxy.person.pdl

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isNotNull
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import java.net.URL
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import no.nav.sokos.pdl.proxy.exception.PdlApiException
import no.nav.sokos.pdl.proxy.jsonClientConfiguration
import no.nav.sokos.pdl.proxy.pdl.entities.Ident
import no.nav.sokos.pdl.proxy.pdl.entities.PersonDetaljer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import resourceToString

private const val pdlUrl = "http://0.0.0.0"

internal class PdlServiceImplTest {
    @Test
    fun `Vellykket hent av en persons identer, navn og adresser fra Pdl`() {

        val mockkGraphQlClient = GraphQLKtorClient(URL(pdlUrl),
            setupMockEngine(
                "hentIdenter_Success_Response.json",
                "hentPerson_Success_Response.json",
                HttpStatusCode.OK)
        )
        val pdlUrl = pdlUrl
        val pdlService = PdlServiceImpl(mockkGraphQlClient, pdlUrl, accessTokenClient = null)

        val personDetaljer: PersonDetaljer? = pdlService.hentPersonDetaljer("22334455667")


        assertThat(personDetaljer)
            .isNotNull()
            .transform { it.identer.map(Ident::ident) }
            .containsExactlyInAnyOrder("2804958208728", "24117920441")




    }

    @Test
    fun `Finnes ikke person identer fra Pdl`() {
        val mockkGraphQlClient = GraphQLKtorClient(URL(pdlUrl),
            setupMockEngine(
                "hentIdenter_fant_ikke_person_response.json",
                "hentPerson_FantIkkePerson_Response.json",
                HttpStatusCode.OK)
        )
        val pdlUrl = pdlUrl
        val pdlService = PdlServiceImpl(mockkGraphQlClient, pdlUrl, accessTokenClient = null)
        assertFailsWith<PdlApiException>(
            message = "Fant ikke person",
            block = {
                pdlService.hentPersonDetaljer("22334455667")
            }
        )
    }

    @Test
    @Disabled
    fun `Ikke authentisert å hente person identer fra Pdl`() {
        val mockkGraphQlClient = GraphQLKtorClient(URL(pdlUrl),
            setupMockEngine(
                "hentIdenter_IkkeAuthentisert_Response.json",
                "hentPerson_IkkeAuthentisert_Response.json",
                HttpStatusCode.OK)
        )
        val pdlUrl = pdlUrl
        val pdlService = PdlServiceImpl(mockkGraphQlClient, pdlUrl, accessTokenClient = null)

        val personDetaljer: PersonDetaljer? = pdlService.hentPersonDetaljer("22334455667")

        assertNotNull(personDetaljer)
    }


}


private fun setupMockEngine(
    hentIdenterResponseFilNavn: String,
    hentPersonResponseFilNavn: String,
    statusCode: HttpStatusCode = HttpStatusCode.OK,
): HttpClient {
    return HttpClient(MockEngine { request ->
        val body = request.body as TextContent
        if (body.text.contains("hentIdenter")) {
            respond(
                content = resourceToString(hentIdenterResponseFilNavn),
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                status = statusCode
            )
        } else {
            respond(
                content = resourceToString(hentPersonResponseFilNavn),
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                status = statusCode)
        }
    }) {
        expectSuccess = false
        install(JsonFeature, jsonClientConfiguration)
    }
}
