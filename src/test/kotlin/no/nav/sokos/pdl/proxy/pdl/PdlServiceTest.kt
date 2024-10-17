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
import no.nav.pdl.hentperson.PostadresseIFrittFormat
import no.nav.sokos.pdl.proxy.APPLICATION_JSON
import no.nav.sokos.pdl.proxy.TestUtil.readFromResource
import no.nav.sokos.pdl.proxy.config.PdlApiException
import no.nav.sokos.pdl.proxy.listener.WiremockListener
import no.nav.sokos.pdl.proxy.listener.WiremockListener.wiremock
import org.junit.jupiter.api.assertThrows

internal class PdlServiceTest : FunSpec({

    extensions(listOf(WiremockListener))

    val pdlService: PdlService by lazy {
        PdlService(
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

        val result = pdlService.hentPersonDetaljer("22334455667")

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

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(
                            "hentIdenter_fant_ikke_person_response.json".readFromResource(),
                        ),
                ),
        )

        val exception =
            assertThrows<PdlApiException> {
                pdlService.hentPersonDetaljer("22334455667")
            }

        exception.shouldNotBeNull()
        exception.feilkode shouldBe 404
        exception.feilmelding shouldBe "Fant ikke person"
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

        val result = pdlService.hentPersonDetaljer("24117920441")

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

        val result = pdlService.hentPersonDetaljer("24117920441")

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

        val result = pdlService.hentPersonDetaljer("24117920441")

        result.shouldNotBeNull()
        result.identer.forAny { it.ident shouldBe "24117920441" }
        result.fornavn.shouldBeNull()
        result.mellomnavn.shouldBeNull()
        result.etternavn.shouldBeNull()
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
                pdlService.hentPersonDetaljer("22334455667")
            }

        exception.shouldNotBeNull()
        exception.feilkode shouldBe 500
        exception.feilmelding shouldBe "Ikke autentisert"
    }
})
