package no.nav.sokos.pdl.proxy.pdl.entities

data class Ident(
    val ident: String,
    val aktiv: Boolean,
    val identifikatorType: IdentifikatorType
)