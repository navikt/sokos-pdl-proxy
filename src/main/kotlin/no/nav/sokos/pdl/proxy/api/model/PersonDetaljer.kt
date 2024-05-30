package no.nav.sokos.pdl.proxy.api.model

import kotlinx.serialization.Serializable
import no.nav.pdl.hentperson.Bostedsadresse
import no.nav.pdl.hentperson.Kontaktadresse
import no.nav.pdl.hentperson.Oppholdsadresse
import no.nav.pdl.hentperson.Person
import no.nav.sokos.pdl.proxy.metrics.Metrics

@Serializable
data class PersonDetaljer(
    val identer: List<Ident>,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val forkortetNavn: String?,
    val bostedsadresse: Bostedsadresse?,
    val kontaktadresse: List<Kontaktadresse>,
    val oppholdsadresse: List<Oppholdsadresse>,
) {
    companion object {
        fun fra(
            identer: List<Ident>,
            person: Person?,
        ): PersonDetaljer {
            val navnList = person?.navn ?: emptyList()
            when (navnList.count()) {
                0 -> Metrics.noAktivtNavnCounter.inc()
                in 2..Int.MAX_VALUE -> Metrics.multipleAktiveNavnCounter.inc()
            }

            val navnWithMasterPDL = navnList.find { it.metadata.master == "PDL" }
            val navn = navnWithMasterPDL ?: navnList.firstOrNull()

            return PersonDetaljer(
                identer,
                navn?.fornavn,
                navn?.mellomnavn,
                navn?.etternavn,
                navn?.forkortetNavn,
                person?.bostedsadresse?.firstOrNull(),
                person?.kontaktadresse.orEmpty(),
                person?.oppholdsadresse.orEmpty(),
            )
        }
    }
}
