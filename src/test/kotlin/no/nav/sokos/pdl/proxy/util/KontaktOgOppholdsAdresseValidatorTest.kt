package no.nav.sokos.pdl.proxy.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.sokos.pdl.proxy.TestData.mockKontaktAdresser
import no.nav.sokos.pdl.proxy.TestData.mockOppholdsAdresser
import no.nav.sokos.pdl.proxy.TestData.mockPerson
import no.nav.sokos.pdl.proxy.config.PdlApiException

internal class KontaktOgOppholdsAdresseValidatorTest : FunSpec({

    test("Person har mer enn 3 kontaktadresser, kaster PdlApiException med feilmelding og feilkode") {
        val person = mockPerson().copy(kontaktadresse = mockKontaktAdresser(4))

        val exepction =
            shouldThrow<PdlApiException> {
                KontaktOgOppholdsAdresseValidator.valider(person)
            }

        exepction.feilmelding shouldBe "For mange kontaktadresser. Personen har 4 og overstiger grensen på 3"
        exepction.feilkode shouldBe 500
    }

    test("Person har mer enn 2 oppholdsadresser, kaster PdlApiException med feilmelding og feilkode") {
        val person = mockPerson().copy(oppholdsadresse = mockOppholdsAdresser(3))

        val exepction =
            shouldThrow<PdlApiException> {
                KontaktOgOppholdsAdresseValidator.valider(person)
            }

        exepction.feilmelding shouldBe "For mange oppholdsadresser. Personen har 3 og overstiger grensen på 2"
        exepction.feilkode shouldBe 500
    }
})
