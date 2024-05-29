package no.nav.sokos.pdl.proxy.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.sokos.pdl.proxy.api.model.PersonIdent
import no.nav.sokos.pdl.proxy.pdl.PdlService

fun Route.pdlProxyApi(pdlService: PdlService = PdlService()) {
    route("/api/pdl-proxy/v1") {
        post("hent-person") {
            val personIdent: PersonIdent = call.receive()
            val person = pdlService.hentPersonDetaljer(personIdent.ident)
            call.respond(HttpStatusCode.OK, person)
        }
    }
}
