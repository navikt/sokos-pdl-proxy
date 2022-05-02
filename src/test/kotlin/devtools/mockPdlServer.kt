package devtools

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(){
    embeddedServer(Netty, 9090) {
        routing {
            route("graphql") {
                post {
                    val request = call.receive<String>()
                    if (request.contains("hentIdenter")) call.respond(identer)
                    else call.respond(person)
                }
            }
        }
    }.start()
}

private val identer = """
    {
        "data": {
            "hentIdenter": {
                "identer": [
                    {
                        "ident": "02055201571",
                        "historisk": false,
                        "gruppe": "FOLKEREGISTERIDENT"
                    },
                    {
                        "ident": "12345678911",
                        "historisk": true,
                        "gruppe": "FOLKEREGISTERIDENT"
                    },
                    {
                        "ident": "1234567890123",
                        "historisk": true,
                        "gruppe": "FOLKEREGISTERIDENT"
                    }
                ]
            }
        }
    }
""".trimIndent()

private val person = """
    {
      "data": {
        "hentPerson": {
          "navn": [
            {
              "fornavn": "Ola",
              "mellomnavn": null,
              "etternavn": "Normann"
            }
          ]
        }
      }
    }
""".trimIndent()


