package no.nav.sokos.pdl.proxy.domain

import kotlinx.serialization.Serializable

@Serializable
data class Ident(
    val ident: String,
    val aktiv: Boolean,
    val identifikatorType: IdentifikatorType,
)
