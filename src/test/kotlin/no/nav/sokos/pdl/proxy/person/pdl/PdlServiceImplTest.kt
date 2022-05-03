package no.nav.sokos.pdl.proxy.person.pdl

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import no.nav.sokos.pdl.proxy.jsonClientConfiguration
import no.nav.sokos.pdl.proxy.pdl.entities.PersonDetaljer
import org.junit.jupiter.api.Test
import resourceToString
import java.net.URL
import kotlin.test.assertNotNull

internal class PdlServiceImplTest {
    @Test
    fun hentPerson() {

        val mockkGraphQlClient: GraphQLKtorClient = GraphQLKtorClient(URL("http://0.0.0.0"), setupMockEngine(HttpStatusCode.OK))
        val pdlUrl = "http://0.0.0.0"
        val pdlService = PdlServiceImpl(mockkGraphQlClient, pdlUrl, accessTokenClient = null)

        val personDetaljer : PersonDetaljer? = pdlService.hentPersonDetaljer("22334455667")

        assertNotNull(personDetaljer)
    }

}

private fun setupMockEngine(statusCode: HttpStatusCode = HttpStatusCode.OK): HttpClient {
    return HttpClient(MockEngine { request ->
        val body = request.body as TextContent
        if (body.text.contains("hentIdenter")) {
            respond(
                content = resourceToString("Hent_Ident_Success.json"),
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                status = statusCode
            )
        } else {
            respond(
                content = resourceToString("Hent_Person_Success.json"),
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                status = statusCode)
        }
    }) {
        expectSuccess=false
        install(JsonFeature, jsonClientConfiguration)
    }
}
