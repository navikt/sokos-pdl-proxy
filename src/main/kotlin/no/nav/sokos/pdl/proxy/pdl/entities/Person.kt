package no.nav.sokos.pdl.proxy.pdl.entities


import no.nav.pdl.hentperson.Bostedsadresse
import no.nav.pdl.hentperson.Kontaktadresse
import no.nav.pdl.hentperson.Oppholdsadresse


data class Person(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val forkortetNavn: String?,
    var bostedsadresse: List<Bostedsadresse>?,
    var kontaktadresse: List<Kontaktadresse>?,
    var oppholdsadresse: List<Oppholdsadresse>?
)

