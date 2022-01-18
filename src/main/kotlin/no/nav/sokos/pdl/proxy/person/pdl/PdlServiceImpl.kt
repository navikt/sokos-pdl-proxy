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
        var personDetaljer : PersonDetaljer
        var identer: List<Ident>
        var person: Person?


        logger.info{"henter identer"}
        identer = hentIdenterForPerson(ident)
        logger.info{"henter person"}
        person = hentPerson(ident)

        val hasIdenter = !identer.isEmpty()

        if (person != null && hasIdenter) {
            personDetaljer = PersonDetaljer(identer, person.fornavn, person.mellomnavn, person.etternavn, person.forkortetNavn)

            return personDetaljer
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
                if (errors != null || !errors.isEmpty()) {
                    logger.error { "Det ligger en feil når innkalt ${errors[0].path} og feil blir: ${errors[0].message} " }
                    handleErrors(errors)
                }
            }

            result.data?.hentPerson?.navn?.map {
                Person(it.fornavn, it.mellomnavn, it.etternavn, it.forkortetNavn)
            }?.first()

        } catch (exception: PdlApiException) {
            logger.error(exception) { "Det har oppstått en feil ved henting fra pdl api - ${exception.message}" }

            throw exception
        } catch (exception: Exception) {
            logger.error(exception) { "Det har oppstått en internfeil ved sokos-pdl-proxy - ${exception.stackTrace}" }

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

         result.errors?.let { errors ->
            if (errors != null || !errors.isEmpty()) {
                logger.error{"Det ligger en feil når innkalt ${errors[0].path} og feil blir: ${errors[0].message} og kode blir ${errors[0].extensions.get("code")}"}
                handleErrors(errors)
            }
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
        val errorMelding = errors
            .map { error -> error.message }

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
            logger.error { "${errorMelding}" }
            throw PdlApiException(404, "${errorMelding}")
        } else if (ikkeTilgangFraPDL) {
            logger.error { "${errorMelding}" }
            throw PdlApiException(403, "${errorMelding}")
        } else if (badRequestTilPDL) {
            logger.error { "${errorMelding}" }
            throw PdlApiException(400, "${errorMelding}")
        }
    }
}

