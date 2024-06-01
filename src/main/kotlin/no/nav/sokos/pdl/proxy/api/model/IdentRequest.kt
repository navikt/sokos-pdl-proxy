package no.nav.sokos.pdl.proxy.api.model

import kotlinx.serialization.Serializable

@Serializable
data class IdentRequest(
    val ident: String,
)
