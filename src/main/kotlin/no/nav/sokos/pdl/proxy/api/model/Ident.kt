package no.nav.sokos.pdl.proxy.api.model

data class Ident(
    val ident: String,
    val aktiv: Boolean,
    val identifikatorType: IdentifikatorType,
)
