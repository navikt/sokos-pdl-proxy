package no.nav.sokos.pdl.proxy.api


import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.sokos.pdl.proxy.api.model.PersonIdent
import no.nav.sokos.pdl.proxy.api.model.TjenestefeilResponse
import no.nav.sokos.pdl.proxy.config.autentiser
import no.nav.sokos.pdl.proxy.exception.PdlApiException
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.Api

private val logger = KotlinLogging.logger {}

fun Application.pdlProxyV1Api(
    pdlService: PdlService,
    useAuthentication: Boolean = true,
) {
    routing {
        autentiser(useAuthentication, Api.PDLPROXY.name) {
            route("/api/pdl-proxy/v1") {
                post("hent-person") {
                    try {
                        val personIdent: PersonIdent = call.receive()
                        val person = pdlService.hentPersonDetaljer(personIdent.ident)
                        logger.info("Kall til hent-person gikk ok")
                        call.respond(HttpStatusCode.OK, person)
                    } catch (pdlApiException: PdlApiException) {
                        logger.info("Forbereder respons til klient etter feil: $pdlApiException")
                        call.respond(
                            HttpStatusCode.fromValue(pdlApiException.feilkode),
                            TjenestefeilResponse(pdlApiException.feilmelding)
                        )
                    } catch (exception: Throwable) {
                        logger.error("Teknisk feil: Feilmelding:${exception.message}", exception)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "En teknisk feil har oppstått. Ta kontakt med utviklerne"
                        )
                    }
                }
            }
        }
    }
}


