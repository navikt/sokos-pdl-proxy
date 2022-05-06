package no.nav.sokos.pdl.proxy.api



import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import no.nav.kontoregister.person.api.authenticate
import no.nav.sokos.pdl.proxy.api.model.PersonIdent
import no.nav.sokos.pdl.proxy.exception.PdlApiException
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.Api
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.sokos.pdl.proxy.api.PdlApi")

object PdlproxyApi {

    fun Application.pdlproxyV1Api(
        pdlService: PdlService,
        useAuthentication: Boolean = true,
    ) {
        routing {
            authenticate(useAuthentication, Api.PDLPROXY.name) {
                route("/api/pdl-proxy/v1") {
                    post("hent-person") {
                        try {
                            val personIdent: PersonIdent = call.receive()
                            logger.info("Henter person detaljer...")
                            val person = pdlService.hentPersonDetaljer(personIdent.ident)
                            logger.info("du er etter pdl inkalling!")
                            call.respond(HttpStatusCode.OK, person!!)
                        } catch (pdlApiException: PdlApiException) {
                            logger.error("Error message på API er : ${pdlApiException.message}")
                            logger.error("Error kode på API er : ${pdlApiException.errorKode}")

                            call.respond(HttpStatusCode.fromValue(pdlApiException.errorKode), pdlApiException.message)
                        } catch (exception: Exception) {
                            logger.error("Det står en exception - ${exception.stackTrace} ")
                            logger.error("Error message er : ${exception.message}")
                            logger.error("Error grun er : - ${exception.cause}")
                            call.respond(HttpStatusCode.InternalServerError, exception.stackTrace)
                        }
                    }
                }
            }
        }
    }

}


