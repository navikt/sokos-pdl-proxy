package no.nav.sokos.pdl.proxy.api



import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import no.nav.sokos.pdl.proxy.pdl.entities.HentIdenter

import no.nav.sokos.pdl.proxy.pdl.entities.PersonIdent
import no.nav.sokos.pdl.proxy.person.pdl.PdlService
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger("no.nav.sokos.pdl.proxy.api.PdlApi")

fun Application.pdlApi(pdlService: PdlService) {
    routing {
        route("") {
            //TODO - Get til Post pga sensitivt informasjon.
            post("hent-person") {
                val personIdent: PersonIdent = call.receive()
                LOGGER.info("Henter person...")
                val person = pdlService.hentPerson(personIdent.ident)
                LOGGER.info("du er etter pdl inkalling!")
                call.respond(HttpStatusCode.OK, person!!)
            }

            post("hent-identer") {
                val hentIdenter : HentIdenter = call.receive()
                LOGGER.info("Henter identer...")
                val ident = pdlService.hentIdenterForPerson(hentIdenter.ident)
                call.respond(HttpStatusCode.OK, ident)
            }
        }
    }
}

