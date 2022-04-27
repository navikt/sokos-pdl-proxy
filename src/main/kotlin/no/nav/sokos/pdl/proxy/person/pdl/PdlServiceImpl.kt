package no.nav.sokos.pdl.proxy.person.pdl

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.request.header
import io.ktor.client.request.url
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.pdl.HentIdenter
import no.nav.pdl.HentPerson
import no.nav.sokos.pdl.proxy.LOGGER
import no.nav.sokos.pdl.proxy.exception.PdlApiException
import no.nav.sokos.pdl.proxy.pdl.entities.Ident
import no.nav.sokos.pdl.proxy.pdl.entities.IdentifikatorType
import no.nav.sokos.pdl.proxy.pdl.entities.Person
import no.nav.sokos.pdl.proxy.pdl.entities.PersonDetaljer
import no.nav.sokos.pdl.proxy.person.security.AccessTokenClient


class PdlServiceImpl (
    private val graphQlClient: GraphQLKtorClient,
    private val pdlUrl: String,
    private val accessTokenClient: AccessTokenClient?
) : PdlService{
    private val logger = KotlinLogging.logger {}

    override fun hentPersonDetaljer(ident: String): PersonDetaljer? {
        val personDetaljer : PersonDetaljer
        val identer: List<Ident>
        val person: Person?

        try {
                logger.info{"henter identer"}
                identer = hentIdenterForPerson(ident)
                logger.info{"henter person"}
                person = hentPerson(ident)

                val hasIdenter = !identer.isEmpty()

                if (person != null && hasIdenter) {
                    personDetaljer = PersonDetaljer(
                        identer,
                        person.fornavn,
                        person.mellomnavn,
                        person.etternavn,
                        person.forkortetNavn,
                        person.bostedsadresse?.first()
                    )

                    return personDetaljer
                }
        } catch (pdlApiException : PdlApiException) {
            logger.error { "hent person detaljer kaster Pdl api exception" }
            throw pdlApiException
        } catch (exception: Exception) {
            logger.error { "hent person detaljer kaster ubehandlet exception" }
            throw exception
        }

        return null
    }

    fun hentPerson(ident: String): Person? {

        return try {
            val result: GraphQLClientResponse<HentPerson.Result> = runBlocking {
                val accessToken = accessTokenClient?.hentAccessToken()
                graphQlClient.execute(HentPerson(HentPerson.Variables(ident = ident))) {
                    url(pdlUrl)
                    header("Authorization", "Bearer $accessToken")
                    header("Tema", "OKO")
                }
            }

            result.errors?.let { errors ->
                {
                    logger.error { "Det ligger en feil når innkalt ${errors[0].path} og feil blir: ${errors[0].message} " }
                    handleErrors(errors)
                }
            }

            if (result.data?.hentPerson?.navn.isNullOrEmpty() == true){
                logger.warn() { "Det har oppstått en feil ved henting av person fra pdl api - navn er empty" }
                return Person("", "", "", "", null, null, null)
            }

            val bostedsadresse = result.data?.hentPerson?.bostedsadresse
            val kontaktadresse = result.data?.hentPerson?.kontaktadresse
            val oppholdsadresse = result.data?.hentPerson?.oppholdsadresse
            val person = result.data?.hentPerson?.navn?.map {
                Person(it.fornavn, it.mellomnavn, it.etternavn, it.forkortetNavn, bostedsadresse, kontaktadresse, oppholdsadresse)
            }?.first()

            return person
        } catch (pdlApiException: PdlApiException) {
            logger.error(pdlApiException) { "Det har oppstått en feil ved henting av person fra pdl api - ${pdlApiException.message}" }

            throw pdlApiException
        } catch (exception: Exception) {
            logger.error(exception) { "Det har oppstått en internfeil ved sokos-pdl-proxy - ${exception.message}" }

            throw exception
        }
    }

    fun hentIdenterForPerson(ident: String): List<Ident> {
        LOGGER.info("Inkalling hent identer")
        val hentIdenter = HentIdenter(HentIdenter.Variables(ident = ident))
        //TODO try catch
        LOGGER.info("Inkalling hent identer")
        val result: GraphQLClientResponse<HentIdenter.Result> = runBlocking {
            val accessToken = accessTokenClient?.hentAccessToken()
            LOGGER.info("Hentet token....")
            graphQlClient.execute(hentIdenter) {
                url(pdlUrl)
                header("Authorization", "Bearer $accessToken")
            }
        }
        try {
            result.errors?.let { errors ->
                {
                    logger.error { "Det ligger en feil når innkalt ${errors[0].path} og feil blir: ${errors[0].message}" }
                    logger.error { "Error code ${errors.mapNotNull { error -> error.extensions }[0].get("code")}. " }
                    handleErrors(errors)
                }
            }
        } catch (pdlApiException : PdlApiException) {
            logger.error { "det oppstå en feil med hent identer og kaster Pdl api exception" }

            throw pdlApiException
        } catch (exception : Exception) {
            logger.error { "det oppstå en feil med hent identer og kaster ubehandlet exception." }

            throw exception
        }

        return hentUtIdenter(result)
    }

    private fun hentUtIdenter(result: GraphQLClientResponse<HentIdenter.Result>) =
        result.data?.hentIdenter?.identer?.map {
            Ident(
                ident = it.ident,
                aktiv = !it.historisk,
                identifikatorType = IdentifikatorType.fra(it.gruppe)
            )
        } ?: emptyList()

    private fun handleErrors(errors: List<GraphQLClientError>) {
        val errorCode = errors
            .mapNotNull { error -> error.extensions }[0]["code"]
        val errorMelding = errors
            .map { error -> error.message }

        logger.error("Error code er ${errorCode}")

        val ikkeFunnetResponsFraPDL = errors
            .mapNotNull { error -> error.extensions }
            .any { entry -> entry["code"] == "not_found" }
        val ikkeTilgangFraPDL = errors
            .mapNotNull { error -> error.extensions }
            .any { entry -> entry["code"] == "forbidden" }
        val badRequestTilPDL = errors
            .mapNotNull { error -> error.extensions }
            .any { entry -> entry["code"] == "bad_request" }

        if (ikkeFunnetResponsFraPDL) {
            logger.error { "Ikke funnet error melding er - ${errorMelding}" }
            throw PdlApiException(404, "${errorMelding}")
        } else if (ikkeTilgangFraPDL) {
            logger.error { "Ingent tilgang til å hente denne ressurs - ${errorMelding}" }
            throw PdlApiException(403, "${errorMelding}")
        } else if (badRequestTilPDL) {
            logger.error { "Dette er en bad request - ${errorMelding}" }
            throw PdlApiException(400, "${errorMelding}")
        } else {
            logger.error { "Denne scenario er ikke behandlet." }
            throw Exception("Ikke behandlet scenario.")
        }
    }
}

