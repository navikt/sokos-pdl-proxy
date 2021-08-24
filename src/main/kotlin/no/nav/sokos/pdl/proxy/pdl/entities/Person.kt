package no.nav.sokos.pdl.proxy.pdl.entities

/**
 * Informasjon om virksomhet
 *
 * @param navn Informasjon om personsnavn
 * @param adresse Informasjon om personsadressen
 */
data class Person(
    val navn: String,
    val ident: String
)
