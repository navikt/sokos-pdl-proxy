package no.nav.sokos.pdl.proxy.api.model

import no.nav.pdl.hentperson.*
import no.nav.sokos.pdl.proxy.metrics.Metrics
import java.time.LocalDateTime

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
            with(navnList.filter { it.metadata.historisk.not() }.size) {
                if (this == 0) Metrics.noAktivtNavnCounter.inc()
                if (this > 1) Metrics.multipleAktiveNavnCounter.inc()
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


