package devtools


import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

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
    }.start(wait = true)
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
          "fornavn": "ÅPENHJERTIG",
          "mellomnavn": null,
          "etternavn": "STAUDE",
          "forkortetNavn": "STAUDE ÅPENHJERTIG"
        }
      ],
      "bostedsadresse": [
        {
          "angittFlyttedato": "1979-11-24",
          "gyldigFraOgMed": "1979-11-24T00:00",
          "gyldigTilOgMed": null,
          "coAdressenavn": null,
          "vegadresse": {
            "husnummer": "55",
            "husbokstav": null,
            "bruksenhetsnummer": null,
            "adressenavn": "HUSANTUNVEIEN",
            "kommunenummer": "3024",
            "bydelsnummer": null,
            "tilleggsnavn": null,
            "postnummer": "1358"
          },
          "matrikkeladresse": null,
          "utenlandskAdresse": null,
          "ukjentBosted": null,
          "metadata": {
            "opplysningsId": "b3293355-1cde-4299-a99c-bad2984e694e",
            "master": "Freg",
            "endringer": [
              {
                "type": "OPPRETT",
                "registrert": "2020-12-08T14:32:26",
                "registrertAv": "Folkeregisteret",
                "systemkilde": "FREG",
                "kilde": "Dolly"
              }
            ],
            "historisk": false
          }
        }
      ],
      "oppholdsadresse": [
        {
          "oppholdAnnetSted": null,
          "coAdressenavn": null,
          "gyldigFraOgMed": "1979-11-24T00:00",
          "utenlandskAdresse": null,
          "vegadresse": {
            "husnummer": "55",
            "husbokstav": null,
            "bruksenhetsnummer": null,
            "adressenavn": "HUSANTUNVEIEN",
            "kommunenummer": "3024",
            "bydelsnummer": null,
            "tilleggsnavn": null,
            "postnummer": "1358"
          },
          "matrikkeladresse": null,
          "metadata": {
            "opplysningsId": "e498f445-704a-4b05-8322-7b9fe4e734b2",
            "master": "Freg",
            "endringer": [
              {
                "type": "OPPRETT",
                "registrert": "2020-12-08T14:32:25",
                "registrertAv": "Folkeregisteret",
                "systemkilde": "FREG",
                "kilde": "Dolly"
              }
            ],
            "historisk": false
          }
        }
      ],
      "kontaktadresse": [
        {
          "gyldigFraOgMed": "1979-11-24T00:00",
          "gyldigTilOgMed": null,
          "type": "Innland",
          "coAdressenavn": null,
          "postboksadresse": null,
          "vegadresse": {
            "husnummer": "55",
            "husbokstav": null,
            "bruksenhetsnummer": null,
            "adressenavn": "HUSANTUNVEIEN",
            "kommunenummer": null,
            "bydelsnummer": null,
            "tilleggsnavn": null,
            "postnummer": "1358"
          },
          "postadresseIFrittFormat": null,
          "utenlandskAdresse": null,
          "utenlandskAdresseIFrittFormat": null,
          "metadata": {
            "opplysningsId": "6ae8f985-0763-441e-ad33-59ae9f01ad49",
            "master": "Freg",
            "endringer": [
              {
                "type": "OPPRETT",
                "registrert": "2020-12-08T14:32:25",
                "registrertAv": "Folkeregisteret",
                "systemkilde": "FREG",
                "kilde": "Dolly"
              }
            ],
            "historisk": false
          }
        }
      ]
    }
  }
}
""".trimIndent()


