package no.nav.sokos.pdl.proxy.util

import mu.KotlinLogging
import no.nav.pdl.hentperson.Person
import no.nav.sokos.pdl.proxy.config.PdlApiException

private val logger = KotlinLogging.logger {}
private const val MAKS_ANTALL_KONTAKT_ADRESSER = 3
private const val MAKS_ANTALL_OPPHOLD_ADRESSER = 2

object KontaktOgOppholdsAdresseValidator {
    fun valider(person: Person) {
        validerAntallKontaktadresser(person)
        validerAntallOppholdsadresser(person)
    }

    private fun validerAntallKontaktadresser(person: Person) {
        val antall = person.kontaktadresse.count()
        if (antall > MAKS_ANTALL_KONTAKT_ADRESSER) {
            val feilmelding =
                "For mange kontaktadresser. Personen har $antall og overstiger grensen på $MAKS_ANTALL_KONTAKT_ADRESSER"
            logger.warn { feilmelding }
            throw PdlApiException(
                500,
                feilmelding,
            )
        }
    }

    private fun validerAntallOppholdsadresser(person: Person) {
        val antall = person.oppholdsadresse.count()
        if (antall > MAKS_ANTALL_OPPHOLD_ADRESSER) {
            val feilmelding =
                "For mange oppholdsadresser. Personen har $antall og overstiger grensen på $MAKS_ANTALL_OPPHOLD_ADRESSER"
            logger.warn { feilmelding }
            throw PdlApiException(
                500,
                feilmelding,
            )
        }
    }
}
