package no.nav.sokos.pdl.proxy.pdl.entities

import no.nav.pdl.hentperson.Bostedsadresse
import no.nav.pdl.hentperson.Kontaktadresse
import no.nav.pdl.hentperson.Oppholdsadresse

data class PersonDetaljer(
    val identer: List<Ident>,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val forkortetNavn: String?,
    val bostedsadresse: Bostedsadresse?,
    val kontaktadresse: Kontaktadresse?,
    val oppholdsadresse: Oppholdsadresse?
)