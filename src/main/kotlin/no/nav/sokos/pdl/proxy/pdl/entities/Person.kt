package no.nav.sokos.pdl.proxy.pdl.entities

import no.nav.pdl.hentperson.Bostedsadresse
data class Person(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val forkortetNavn: String?,
    var bostedsadresse: List<Bostedsadresse>?
)

