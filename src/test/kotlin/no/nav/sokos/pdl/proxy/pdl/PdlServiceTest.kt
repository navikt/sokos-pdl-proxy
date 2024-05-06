package no.nav.sokos.pdl.proxy.pdl

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.http.HttpStatusCode
import no.nav.pdl.hentperson.PostadresseIFrittFormat
import no.nav.sokos.pdl.proxy.api.model.Ident
import no.nav.sokos.pdl.proxy.config.PdlApiException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import setupMockEngine
import java.net.URI

private const val PDL_URL = "http://0.0.0.0"

internal class PdlServiceTest {
    @Test
    fun `Vellykket hent av en persons identer, navn og adresser fra Pdl`() {
        val result =
            PdlService(
                GraphQLKtorClient(
                    URI(PDL_URL).toURL(),
                    setupMockEngine(
                        "hentIdenter_success_response.json",
                        "hentPerson_success_response.json",
                        HttpStatusCode.OK,
                    ),
                ),
                PDL_URL,
                accessTokenClient = null,
            )
                .hentPersonDetaljer("22334455667")

        assertThat(result).isNotNull()
        assertThat(result.identer.map(Ident::ident)).containsExactlyInAnyOrder("24117920441")
        assertThat(result.kontaktadresse.first().postadresseIFrittFormat)
            .isNotNull()
            .all {
                prop(PostadresseIFrittFormat::adresselinje1).isEqualTo("adresse 1")
                prop(PostadresseIFrittFormat::adresselinje2).isEqualTo("adresse 2")
                prop(PostadresseIFrittFormat::adresselinje3).isEqualTo("adresse 3")
                prop(PostadresseIFrittFormat::postnummer).isEqualTo("4242")
            }
    }

    @Test
    fun `Finnes ikke person identer fra Pdl`() {
        val exception =
            assertThrows<PdlApiException> {
                PdlService(
                    GraphQLKtorClient(
                        URI(PDL_URL).toURL(),
                        setupMockEngine(
                            "hentIdenter_fant_ikke_person_response.json",
                            "hentPerson_fant_ikke_person_response.json",
                        ),
                    ),
                    PDL_URL,
                    accessTokenClient = null,
                )
                    .hentPersonDetaljer("22334455667")
            }

        assertThat(exception).isNotNull()
        assertThat(exception.feilkode).isEqualTo(404)
        assertThat(exception.feilmelding).contains("Fant ikke person")
    }

    @Test
    fun `Benytte eneste aktive navn hvis de andre er historiske`() {
        assertThat(
            PdlService(
                GraphQLKtorClient(
                    URI(PDL_URL).toURL(),
                    setupMockEngine(
                        "hentIdenter_success_response.json",
                        "hentPerson_flere_navn_ett_aktivt.json",
                        HttpStatusCode.OK,
                    ),
                ),
                PDL_URL,
                accessTokenClient = null,
            )
                .hentPersonDetaljer("22334455667"),
        )
            .isNotNull()
            .all {
                transform { it.identer.map(Ident::ident) }
                    .containsExactlyInAnyOrder("24117920441")
                transform { it.fornavn }.isEqualTo("Eneste")
                transform { it.mellomnavn }.isEqualTo("Aktive")
                transform { it.etternavn }.isEqualTo("Navn")
            }
    }

    @Test
    fun `Skal benytte nyeste aktive navn selv om historiske har nyere registreringsdato`() {
        assertThat(
            PdlService(
                GraphQLKtorClient(
                    URI(PDL_URL).toURL(),
                    setupMockEngine(
                        "hentIdenter_success_response.json",
                        "hentPerson_flere_aktive_navn_noen_nyere_historiske.json",
                        HttpStatusCode.OK,
                    ),
                ),
                PDL_URL,
                accessTokenClient = null,
            )
                .hentPersonDetaljer("22334455667"),
        )
            .isNotNull()
            .all {
                transform { it.identer.map(Ident::ident) }
                    .containsExactlyInAnyOrder("24117920441")
                transform { it.fornavn }.isEqualTo("Seneste")
                transform { it.mellomnavn }.isEqualTo("Navn")
                transform { it.etternavn }.isEqualTo("Fra PDL")
            }
    }

    @Test
    fun `Skal benytte seneste historiske navn hvis det ikke er noen aktive`() {
        assertThat(
            PdlService(
                GraphQLKtorClient(
                    URI(PDL_URL).toURL(),
                    setupMockEngine(
                        "hentIdenter_success_response.json",
                        "hentPerson_flere_bare_historiske_navn.json",
                        HttpStatusCode.OK,
                    ),
                ),
                PDL_URL,
                accessTokenClient = null,
            )
                .hentPersonDetaljer("22334455667"),
        )
            .isNotNull()
            .all {
                transform { it.identer.map(Ident::ident) }
                    .containsExactlyInAnyOrder("24117920441")
                transform { it.fornavn }.isNull()
                transform { it.mellomnavn }.isNull()
                transform { it.etternavn }.isNull()
            }
    }

    @Test
    fun `Ikke authentisert å hente person identer fra Pdl`() {
        val exception =
            assertThrows<PdlApiException> {
                PdlService(
                    GraphQLKtorClient(
                        URI(PDL_URL).toURL(),
                        setupMockEngine(
                            "hentIdenter_ikke_authentisert_response.json",
                            "hentPerson_ikke_authentisert_response.json",
                        ),
                    ),
                    PDL_URL,
                    accessTokenClient = null,
                )
                    .hentPersonDetaljer("22334455667")
            }

        assertThat(exception).isNotNull()
        assertThat(exception.feilkode).isEqualTo(500)
        assertThat(exception.feilmelding).contains("Ikke autentisert")
    }
}
