package no.nav.sokos.pdl.proxy.exception

class PdlApiException(
    val errorKode: Int,
    override val message: String,
) : Exception(message) {
}