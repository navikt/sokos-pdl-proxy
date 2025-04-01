package no.nav.sokos.pdl.proxy.pdl

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAny
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.assertThrows

import no.nav.pdl.hentperson.PostadresseIFrittFormat
import no.nav.sokos.pdl.proxy.APPLICATION_JSON
import no.nav.sokos.pdl.proxy.TestUtil.readFromResource
import no.nav.sokos.pdl.proxy.config.PdlApiException
import no.nav.sokos.pdl.proxy.listener.WiremockListener
import no.nav.sokos.pdl.proxy.listener.WiremockListener.wiremock

internal class PdlServiceTest : FunSpec({

    extensions(listOf(WiremockListener))

    val pdlClientService: PdlClientService by lazy {
        PdlClientService(
            pdlUrl = wiremock.baseUrl() + "/graphql",
            accessTokenClient = WiremockListener.accessTokenClient,
        )
    }

    beforeTest {
        wiremock.resetAll()
    }

    test("Vellykket hent av en persons identer, navn og adresser fra Pdl") {

        val hentIdenter = "hentIdenter_success_response.json".readFromResource()
        val hentPerson = "hentPerson_success_response.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                )
                .willSetStateTo("SecondRequest"),
        )

        // Second state: "SecondRequest"
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs("SecondRequest")
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPerson),
                ),
        )

        val result = pdlClientService.hentPersonDetaljer("22334455667")

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

        val hentIdenter = "hentIdenter_fant_ikke_person_response.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                ),
        )

        val exception =
            assertThrows<PdlApiException> {
                pdlClientService.hentPersonDetaljer("22334455667")
            }

        exception.shouldNotBeNull()
        exception.feilkode shouldBe 404
        exception.feilmelding shouldBe "Fant ikke person"
    }

    test("Finner person identer fra Pdl, men ikke person") {

        val hentIdenter = "hentIdenter_success_response.json".readFromResource()
        val hentPerson = "hentPerson_fant_ikke_person_response.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                )
                .willSetStateTo("SecondRequest"),
        )

        // Second state: "SecondRequest"
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs("SecondRequest")
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPerson),
                ),
        )

        val exception =
            assertThrows<PdlApiException> {
                pdlClientService.hentPersonDetaljer("22334455667")
            }

        exception.shouldNotBeNull()
        exception.feilkode shouldBe 404
        exception.feilmelding shouldBe "Fant ikke person"
    }

    test("Finner person identer fra Pdl, finner person men tomt navn respons") {

        val hentIdenter = "hentIdenter_success_response.json".readFromResource()
        val hentPerson = "hentPerson_tomt_navn_response.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                )
                .willSetStateTo("SecondRequest"),
        )

        // Second state: "SecondRequest"
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs("SecondRequest")
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPerson),
                ),
        )

        val result = pdlClientService.hentPersonDetaljer("24117920441")

        result.fornavn shouldBe null
    }

    test("Benytte eneste aktive navn hvis de andre er historiske") {

        val hentIdenter = "hentIdenter_success_response.json".readFromResource()
        val hentPerson = "hentPerson_flere_navn_ett_aktivt.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                )
                .willSetStateTo("SecondRequest"),
        )

        // Second state: "SecondRequest"
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs("SecondRequest")
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPerson),
                ),
        )

        val result = pdlClientService.hentPersonDetaljer("24117920441")

        result.shouldNotBeNull()
        result.identer.forAny { it.ident shouldBe "24117920441" }
        result.fornavn shouldBe "Eneste"
        result.mellomnavn shouldBe "Aktive"
        result.etternavn shouldBe "Navn"
    }

    test("Skal benytte nyeste aktive navn selv om historiske har nyere registreringsdato") {

        val hentIdenter = "hentIdenter_success_response.json".readFromResource()
        val hentPerson = "hentPerson_flere_aktive_navn_noen_nyere_historiske.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                )
                .willSetStateTo("SecondRequest"),
        )

        // Second state: "SecondRequest"
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs("SecondRequest")
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPerson),
                ),
        )

        val result = pdlClientService.hentPersonDetaljer("24117920441")

        result.shouldNotBeNull()
        result.identer.forAny { it.ident shouldBe "24117920441" }
        result.fornavn shouldBe "Seneste"
        result.mellomnavn shouldBe "Navn"
        result.etternavn shouldBe "Fra PDL"
    }

    test("Skal benytte seneste historiske navn hvis det ikke er noen aktive") {

        val hentIdenter = "hentIdenter_success_response.json".readFromResource()
        val hentPerson = "hentPerson_flere_bare_historiske_navn.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                )
                .willSetStateTo("SecondRequest"),
        )

        // Second state: "SecondRequest"
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs("SecondRequest")
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPerson),
                ),
        )

        val result = pdlClientService.hentPersonDetaljer("24117920441")

        result.shouldNotBeNull()
        result.identer.forAny { it.ident shouldBe "24117920441" }
        result.fornavn.shouldBeNull()
        result.mellomnavn.shouldBeNull()
        result.etternavn.shouldBeNull()
    }

    test("Finner person men men har flere enn 2 oppholdsadresser, kaster PdlApiException med feilmelding og feilkode") {

        val hentIdenter = "hentIdenter_success_response.json".readFromResource()
        val hentPerson = "hentPerson_success_response_med_3_oppholdsadresser.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                )
                .willSetStateTo("SecondRequest"),
        )

        // Second state: "SecondRequest"
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs("SecondRequest")
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPerson),
                ),
        )

        val exception =
            assertThrows<PdlApiException> {
                pdlClientService.hentPersonDetaljer("24117920441")
            }

        exception.shouldNotBeNull()
        exception.feilkode shouldBe 500
        exception.feilmelding shouldBe "For mange oppholdsadresser. Personen har 3 og overstiger grensen på 2"
    }

    test("Finner person men har flere enn 3 kontaktadresser, kaster PdlApiException med feilmelding og feilkode") {

        val hentIdenter = "hentIdenter_success_response.json".readFromResource()
        val hentPerson = "hentPerson_success_response_med_4_kontaktadresser.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                )
                .willSetStateTo("SecondRequest"),
        )

        // Second state: "SecondRequest"
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("GraphQL Scenario")
                .whenScenarioStateIs("SecondRequest")
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPerson),
                ),
        )

        val exception =
            assertThrows<PdlApiException> {
                pdlClientService.hentPersonDetaljer("24117920441")
            }

        exception.shouldNotBeNull()
        exception.feilkode shouldBe 500
        exception.feilmelding shouldBe "For mange kontaktadresser. Personen har 4 og overstiger grensen på 3"
    }

    test("Ikke authentisert å hente person identer fra Pdl") {

        val hentIdenter = "hentIdenter_ikke_authentisert_response.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                ),
        )

        val exception =
            assertThrows<PdlApiException> {
                pdlClientService.hentPersonDetaljer("22334455667")
            }

        exception.shouldNotBeNull()
        exception.feilkode shouldBe 500
        exception.feilmelding shouldBe "Ikke autentisert"
    }

    test("Feilkoder fra PDL skal returnere 500 med en beskrivende feilmelding") {

        val hentIdenter = "hentIdenter_annen_feilmelding_response.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentIdenter),
                ),
        )

        val exception =
            assertThrows<PdlApiException> {
                pdlClientService.hentPersonDetaljer("22334455667")
            }

        exception.shouldNotBeNull()
        exception.feilkode shouldBe 500
        exception.feilmelding shouldBe "En annen feilmelding fra PDL"
    }
})
