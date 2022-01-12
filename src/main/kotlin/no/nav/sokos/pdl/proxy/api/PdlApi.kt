package no.nav.sokos.pdl.proxy.api



import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import no.nav.sokos.pdl.proxy.pdl.entities.Ident


import no.nav.sokos.pdl.proxy.pdl.entities.PersonIdent
import no.nav.sokos.pdl.proxy.person.pdl.PdlServiceImpl
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger("no.nav.sokos.pdl.proxy.api.PdlApi")

fun Application.pdlApi(pdlServiceImpl: PdlServiceImpl) {
    routing {
        route("") {
            //TODO - Get til Post pga sensitivt informasjon.
            post("hent-person") {
                val personIdent: PersonIdent = call.receive()
                LOGGER.info("Henter person...")
                val person = pdlServiceImpl.hentPerson(personIdent.ident)
                LOGGER.info("du er etter pdl inkalling!")
                call.respond(HttpStatusCode.OK, person!!)
            }

            post("hent-identer") {
                val hentIdenter : Ident = call.receive()
                LOGGER.info("Henter identer...")
                val ident = pdlServiceImpl.hentIdenterForPerson(hentIdenter.ident)
                call.respond(HttpStatusCode.OK, ident)
            }
        }
    }
}

