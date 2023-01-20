package no.nav.sokos.pdl.proxy.pdl

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.prop
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import java.net.URL
import no.nav.pdl.hentperson.PostadresseIFrittFormat
import no.nav.sokos.pdl.proxy.api.model.Ident
import no.nav.sokos.pdl.proxy.util.PdlApiException
import org.junit.jupiter.api.Test
import readFromResource

private const val pdlUrl = "http://0.0.0.0"

internal class PdlServiceTest {
    @Test
    fun `Vellykket hent av en persons identer, navn og adresser fra Pdl`() {
        assertThat(
            PdlService(
                GraphQLKtorClient(
                    URL(pdlUrl),
                    setupMockEngine(
                        "hentIdenter_success_response.json",
                        "hentPerson_success_response.json",
                        HttpStatusCode.OK
                    )
                ),
                pdlUrl,
                accessTokenClient = null
            )
                .hentPersonDetaljer("22334455667")
        )
            .isNotNull()
            .all {
                transform { it.identer.map(Ident::ident) }
                    .containsExactlyInAnyOrder("2804958208728", "24117920441")
                transform { it.kontaktadresse.first().postadresseIFrittFormat }
                    .isNotNull()
                    .all {
                        prop(PostadresseIFrittFormat::adresselinje1).isEqualTo("adresse 1")
                        prop(PostadresseIFrittFormat::adresselinje2).isEqualTo("adresse 2")
                        prop(PostadresseIFrittFormat::adresselinje3).isEqualTo("adresse 3")
                        prop(PostadresseIFrittFormat::postnummer).isEqualTo("4242")
                    }

            }
    }

    @Test
    fun `Finnes ikke person identer fra Pdl`() {
        assertThat {
            PdlService(
                pdlSomReturnerer(
                    "hentIdenter_fant_ikke_person_response.json",
                    "hentPerson_fant_ikke_person_response.json"
                ),
                pdlUrl,
                accessTokenClient = null
            )
                .hentPersonDetaljer("22334455667")
        }
            .isFailure()
            .transform { it as PdlApiException }
            .all {
                prop(PdlApiException::feilkode).isEqualTo(404)
                prop(PdlApiException::feilmelding).contains("Fant ikke person")
            }
    }

    @Test
    fun `Ikke authentisert å hente person identer fra Pdl`() {
        assertThat {
            PdlService(
                pdlSomReturnerer(
                    "hentIdenter_ikke_authentisert_response.json",
                    "hentPerson_ikke_authentisert_response.json"
                ),
                pdlUrl,
                accessTokenClient = null
            )
                .hentPersonDetaljer("22334455667")
        }
            .isFailure()
            .transform { it as PdlApiException }
            .all {
                prop(PdlApiException::feilkode).isEqualTo(500)
                prop(PdlApiException::feilmelding).contains("Ikke autentisert")
            }
    }

    private fun pdlSomReturnerer(hentIdenterRespons: String, hentPersonRespons: String) = GraphQLKtorClient(
        URL(pdlUrl),
        setupMockEngine(
            hentIdenterRespons,
            hentPersonRespons,
            HttpStatusCode.OK
        )
    )
}

fun setupMockEngine(
    hentIdenterResponseFilNavn: String?,
    hentPersonResponseFilNavn: String?,
    statusCode: HttpStatusCode = HttpStatusCode.OK,
): HttpClient {
    return HttpClient(MockEngine { request ->
        val body = request.body as TextContent
        if (body.text.contains("hentIdenter")) {
            respond(
                content = hentIdenterResponseFilNavn?.let { (it).readFromResource() }.orEmpty(),
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                status = statusCode
            )
        } else {
            respond(
                content = hentPersonResponseFilNavn?.let { (it).readFromResource() }.orEmpty(),
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                status = statusCode
            )
        }
    }) {
        expectSuccess = false
        install(HttpClient())
    }
}
