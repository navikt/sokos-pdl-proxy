package no.nav.sokos.pdl.proxy.api



import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.getOrFail
import no.nav.sokos.pdl.proxy.person.pdl.PdlService
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger("no.nav.sokos.pdl.proxy.api.PdlApi")

fun Application.pdlApi(pdlService: PdlService) {
    routing {
        route("") {
            //TODO - Get til Post pga sensitivt informasjon.
            get("hent-person/{ident}") {
                val personIdent = call.parameters.getOrFail("ident")
                LOGGER.info("du er her!")
                val person = pdlService.hentPerson(personIdent)
                LOGGER.info("du er etter pdl inkalling!")
                call.respond(HttpStatusCode.OK, person)
            }
        }
    }
}

