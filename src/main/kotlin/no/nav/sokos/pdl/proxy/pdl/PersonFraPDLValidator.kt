package no.nav.sokos.pdl.proxy.pdl

import mu.KotlinLogging
import no.nav.pdl.hentperson.Person
import no.nav.sokos.pdl.proxy.exception.PdlApiException

private val logger = KotlinLogging.logger {}
private const val maksAntallKontaktadresser = 3
private const val maksAntallOppholdsadresser = 2

object PersonFraPDLValidator {
    fun valider(person: Person) {
        validerAntallKontaktadresser(person)
        validerAntallOppholdsadresser(person)
    }

    private fun validerAntallKontaktadresser(person: Person) {
        val antall = person.kontaktadresse.count()
        if (antall > maksAntallKontaktadresser) {
            val feilmelding =
                "For mange kontaktadresser. Denne personen har $antall og overstiger grensen på $maksAntallKontaktadresser"
            logger.warn { feilmelding }
            throw PdlApiException(500,
                feilmelding)
        }
    }

    private fun validerAntallOppholdsadresser(person: Person) {
        val antall = person.oppholdsadresse.count()
        if (antall > maksAntallOppholdsadresser) {
            val feilmelding =
                "For mange oppholdsadresser. Denne personen har $antall og overstiger grensen på $maksAntallOppholdsadresser"
            logger.warn { feilmelding }
            throw PdlApiException(500,
                feilmelding)
        }
    }

}
