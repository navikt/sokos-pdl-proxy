package no.nav.sokos.pdl.proxy.api.model

import no.nav.pdl.hentperson.Bostedsadresse
import no.nav.pdl.hentperson.Kontaktadresse
import no.nav.pdl.hentperson.Oppholdsadresse
import no.nav.pdl.hentperson.Person

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
        fun fra(identer: List<Ident>, person: Person?) = PersonDetaljer(
            identer,
            person?.navn?.firstOrNull()?.fornavn,
            person?.navn?.firstOrNull()?.mellomnavn,
            person?.navn?.firstOrNull()?.etternavn,
            person?.navn?.firstOrNull()?.forkortetNavn,
            person?.bostedsadresse?.firstOrNull(),
            person?.kontaktadresse.orEmpty(),
            person?.oppholdsadresse.orEmpty(),
        )
    }
}


