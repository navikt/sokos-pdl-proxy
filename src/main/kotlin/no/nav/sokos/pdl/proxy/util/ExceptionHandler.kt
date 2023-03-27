package no.nav.sokos.pdl.proxy.util

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond
import mu.KotlinLogging
import no.nav.sokos.pdl.proxy.api.model.TjenestefeilResponse

private val logger = KotlinLogging.logger { }

fun StatusPagesConfig.exceptionHandler() {

    exception<PdlApiException> { call, pdlApiException ->
        call.logInfoOgResponder(
            pdlApiException, HttpStatusCode.fromValue(pdlApiException.feilkode)
        ) { pdlApiException.feilmelding }
    }

    exception<Throwable> { call, throwable ->
        call.logErrorOgResponder(throwable) { "En teknisk feil har oppstått. Ta kontakt med utviklerne" }
    }
}

data class PdlApiException(
    val feilkode: Int,
    val feilmelding: String,
) : Exception(feilmelding)

private suspend inline fun ApplicationCall.logInfoOgResponder(
    pdlApiException: PdlApiException,
    status: HttpStatusCode,
    lazyMessage: () -> String,
) {
    val feilmelding = lazyMessage()
    logger.info(pdlApiException) { feilmelding }

    val response = TjenestefeilResponse(feilmelding)
    this.respond(status, response)
}

private suspend inline fun ApplicationCall.logErrorOgResponder(
    exeption: Throwable,
    status: HttpStatusCode = HttpStatusCode.InternalServerError,
    lazyMessage: () -> String,
) {
    val message = lazyMessage()
    logger.error(exeption) { message }
    this.respond(status, message)
}