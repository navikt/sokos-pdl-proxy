package no.nav.sokos.pdl.proxy.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.sokos.pdl.proxy.pdl.entities.Person
import org.slf4j.LoggerFactory
import java.lang.Exception

private val LOGGER = LoggerFactory.getLogger("no.nav.sokos.pdl.proxy.api.PdlApi")

fun Application.pdlApi(ident:String) {
    var person = Person("Nav Navnesen", "12345678910")
    routing {
        route("") {
            post("create-person") {
                LOGGER.info("du er på post!")
                try {
                    person = call.receive<Person>()
                }catch (ex :Exception){
                    LOGGER.info("Feil!!", ex)
                }

                call.respondText("Person '$person' stored correctly", status = HttpStatusCode.Created)
            }

            get("hent-person") {
                LOGGER.info("du er her!")
                call.respond (
                    person
                )
            }
        }
    }
}

