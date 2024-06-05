package no.nav.sokos.pdl.proxy.util

import kotlinx.serialization.Serializable

@Serializable
data class TjenestefeilResponse(
    val melding: String,
)
