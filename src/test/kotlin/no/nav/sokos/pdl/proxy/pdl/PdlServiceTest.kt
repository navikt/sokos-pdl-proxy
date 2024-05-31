package no.nav.sokos.pdl.proxy.pdl

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAny
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.pdl.hentperson.PostadresseIFrittFormat
import no.nav.sokos.pdl.proxy.config.PdlApiException
import no.nav.sokos.pdl.proxy.config.setupMockEngine
import no.nav.sokos.pdl.proxy.security.AccessTokenClient
import org.junit.jupiter.api.assertThrows
import java.net.URI

private const val PDL_URL = "http://0.0.0.0"
private val accessTokenClient = mockk<AccessTokenClient>()

internal class PdlServiceTest : FunSpec({

    test("Vellykket hent av en persons identer, navn og adresser fra Pdl") {
        coEvery { accessTokenClient.hentAccessToken() } returns "token"

        val result =
            PdlService(
                pdlUrl = PDL_URL,
                graphQlClient =
                    GraphQLKtorClient(
                        URI(PDL_URL).toURL(),
                        setupMockEngine(
                            "hentIdenter_success_response.json",
                            "hentPerson_success_response.json",
                        ),
                    ),
                accessTokenClient = accessTokenClient,
            )
                .hentPersonDetaljer("22334455667")

        result.shouldNotBeNull()
        result.identer.forAny { it.ident shouldBe "24117920441" }
        result.kontaktadresse.first().postadresseIFrittFormat.shouldNotBeNull()
        result.kontaktadresse.first().postadresseIFrittFormat shouldBe
            PostadresseIFrittFormat(
                adresselinje1 = "adresse 1",
                adresselinje2 = "adresse 2",
                adresselinje3 = "adresse 3",
                postnummer = "4242",
            )
    }

    test("Finnes ikke person identer fra Pdl") {
        coEvery { accessTokenClient.hentAccessToken() } returns "token"

        val exception =
            assertThrows<PdlApiException> {
                PdlService(
                    pdlUrl = PDL_URL,
                    graphQlClient =
                        GraphQLKtorClient(
                            URI(PDL_URL).toURL(),
                            setupMockEngine(
                                "hentIdenter_fant_ikke_person_response.json",
                                "hentPerson_fant_ikke_person_response.json",
                            ),
                        ),
                    accessTokenClient = accessTokenClient,
                )
                    .hentPersonDetaljer("22334455667")
            }
        exception.shouldNotBeNull()
        exception.feilkode shouldBe 404
        exception.feilmelding shouldBe "Fant ikke person"
    }

    test("Benytte eneste aktive navn hvis de andre er historiske") {
        coEvery { accessTokenClient.hentAccessToken() } returns "token"

        val result =
            PdlService(
                pdlUrl = PDL_URL,
                graphQlClient =
                    GraphQLKtorClient(
                        URI(PDL_URL).toURL(),
                        setupMockEngine(
                            "hentIdenter_success_response.json",
                            "hentPerson_flere_navn_ett_aktivt.json",
                        ),
                    ),
                accessTokenClient = accessTokenClient,
            )
                .hentPersonDetaljer("22334455667")

        result.shouldNotBeNull()
        result.identer.forAny { it.ident shouldBe "24117920441" }
        result.fornavn shouldBe "Eneste"
        result.mellomnavn shouldBe "Aktive"
        result.etternavn shouldBe "Navn"
    }

    test("Skal benytte nyeste aktive navn selv om historiske har nyere registreringsdato") {
        coEvery { accessTokenClient.hentAccessToken() } returns "token"

        val result =
            PdlService(
                pdlUrl = PDL_URL,
                graphQlClient =
                    GraphQLKtorClient(
                        URI(PDL_URL).toURL(),
                        setupMockEngine(
                            "hentIdenter_success_response.json",
                            "hentPerson_flere_aktive_navn_noen_nyere_historiske.json",
                        ),
                    ),
                accessTokenClient = accessTokenClient,
            )
                .hentPersonDetaljer("22334455667")

        result.shouldNotBeNull()
        result.identer.forAny { it.ident shouldBe "24117920441" }
        result.fornavn shouldBe "Seneste"
        result.mellomnavn shouldBe "Navn"
        result.etternavn shouldBe "Fra PDL"
    }

    test("Skal benytte seneste historiske navn hvis det ikke er noen aktive") {
        coEvery { accessTokenClient.hentAccessToken() } returns "token"

        val result =
            PdlService(
                pdlUrl = PDL_URL,
                graphQlClient =
                    GraphQLKtorClient(
                        URI(PDL_URL).toURL(),
                        setupMockEngine(
                            "hentIdenter_success_response.json",
                            "hentPerson_flere_bare_historiske_navn.json",
                        ),
                    ),
                accessTokenClient = accessTokenClient,
            )
                .hentPersonDetaljer("22334455667")

        result.shouldNotBeNull()
        result.identer.forAny { it.ident shouldBe "24117920441" }
        result.fornavn.shouldBeNull()
        result.mellomnavn.shouldBeNull()
        result.etternavn.shouldBeNull()
    }

    test("Ikke authentisert å hente person identer fra Pdl") {
        coEvery { accessTokenClient.hentAccessToken() } returns "token"

        val exception =
            assertThrows<PdlApiException> {
                PdlService(
                    pdlUrl = PDL_URL,
                    graphQlClient =
                        GraphQLKtorClient(
                            URI(PDL_URL).toURL(),
                            setupMockEngine(
                                "hentIdenter_ikke_authentisert_response.json",
                                "hentPerson_ikke_authentisert_response.json",
                            ),
                        ),
                    accessTokenClient = accessTokenClient,
                )
                    .hentPersonDetaljer("22334455667")
            }

        exception.shouldNotBeNull()
        exception.feilkode shouldBe 500
        exception.feilmelding shouldBe "Ikke autentisert"
    }
})
