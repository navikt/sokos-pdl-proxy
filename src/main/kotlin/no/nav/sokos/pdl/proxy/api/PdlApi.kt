package no.nav.sokos.pdl.proxy.api



import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import no.nav.sokos.pdl.proxy.exception.PdlApiException


import no.nav.sokos.pdl.proxy.pdl.entities.PersonIdent
import no.nav.sokos.pdl.proxy.person.pdl.PdlServiceImpl
import no.nav.sokos.pdl.proxy.person.security.Api
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger("no.nav.sokos.pdl.proxy.api.PdlApi")

fun Application.pdlApi(pdlServiceImpl: PdlServiceImpl/*,
                       useAuthentication: Boolean = true*/) {
    routing {
        //authenticate(useAuthentication, Api.PDLPROXY.name) {
            route("") {
                post("hent-person") {
                    try {
                        val personIdent: PersonIdent = call.receive()
                        LOGGER.info("Henter person detaljer...")
                        val person = pdlServiceImpl.hentPersonDetaljer(personIdent.ident)
                        LOGGER.info("du er etter pdl inkalling!")
                        call.respond(HttpStatusCode.OK, person!!)
                    } catch (pdlApiException: PdlApiException) {
                        LOGGER.error("Error message på API er : ${pdlApiException.message}")
                        LOGGER.error("Error kode på API er : ${pdlApiException.errorKode}")

                        call.respond(HttpStatusCode.fromValue(pdlApiException.errorKode), pdlApiException.message)
                    } catch (exception: Exception) {
                        LOGGER.error("Det står en exception - ${exception.stackTrace}")
                        call.respond(HttpStatusCode.InternalServerError, exception.stackTrace)
                    }
                }
            }
        //}
    }
}

