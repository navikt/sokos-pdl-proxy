package no.nav.sokos.pdl.proxy.api.model

import kotlinx.serialization.Serializable

@Serializable
data class TjenestefeilResponse(
    val melding: String,
)
