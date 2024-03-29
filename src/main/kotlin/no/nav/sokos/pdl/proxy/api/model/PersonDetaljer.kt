package no.nav.sokos.pdl.proxy.api.model

import java.time.LocalDateTime
import no.nav.pdl.hentperson.Bostedsadresse
import no.nav.pdl.hentperson.Kontaktadresse
import no.nav.pdl.hentperson.Oppholdsadresse
import no.nav.pdl.hentperson.Person
import no.nav.sokos.pdl.proxy.metrics.Metrics

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
        fun fra(identer: List<Ident>, person: Person?): PersonDetaljer {
            val navnList = person?.navn ?: emptyList()
            when (navnList.count { !it.metadata.historisk }) {
                0 -> Metrics.noAktivtNavnCounter.inc()
                in 2..Int.MAX_VALUE -> Metrics.multipleAktiveNavnCounter.inc()
            }

            val riktigNavn = navnList
                    .sortedByDescending { it.metadata.endringer.maxOf { endring -> LocalDateTime.parse(endring.registrert) } }
                    .partition { !it.metadata.historisk }.toList()
                    .flatMap { it.toList() }.firstOrNull()

            return PersonDetaljer(
                    identer,
                    riktigNavn?.fornavn,
                    riktigNavn?.mellomnavn,
                    riktigNavn?.etternavn,
                    riktigNavn?.forkortetNavn,
                    person?.bostedsadresse?.firstOrNull(),
                    person?.kontaktadresse.orEmpty(),
                    person?.oppholdsadresse.orEmpty(),
            )
        }
    }
}


