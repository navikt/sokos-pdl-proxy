package no.nav.sokos.pdl.proxy.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond
import no.nav.sokos.pdl.proxy.api.model.TjenestefeilResponse

fun StatusPagesConfig.statusPageConfig() {

    exception<PdlApiException> { call, pdlApiException ->
        val response = TjenestefeilResponse(pdlApiException.feilmelding)
        call.logInfoOgResponder(pdlApiException, HttpStatusCode.fromValue(pdlApiException.feilkode), response)
    }

    exception<Throwable> { call, throwable ->
        val response = TjenestefeilResponse("En teknisk feil har oppstått. Ta kontakt med utviklerne")
        call.logErrorOgResponder(throwable, response)
    }
}

data class PdlApiException(
    val feilkode: Int,
    val feilmelding: String,
) : Exception(feilmelding)

private suspend inline fun ApplicationCall.logInfoOgResponder(
    pdlApiException: PdlApiException,
    status: HttpStatusCode,
    response: TjenestefeilResponse
) {
    logger.info(pdlApiException) { response.melding }

    this.respond(status, response)
}

private suspend inline fun ApplicationCall.logErrorOgResponder(
    exeption: Throwable,
    response: TjenestefeilResponse,
    status: HttpStatusCode = HttpStatusCode.InternalServerError,
) {
    logger.error(exeption) { response.melding }
    this.respond(status, response)
}
