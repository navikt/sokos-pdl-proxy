package no.nav.sokos.pdl.proxy.exception

data class PdlApiException(
    val feilkode: Int,
    val feilmelding: String,
) : Exception(feilmelding)