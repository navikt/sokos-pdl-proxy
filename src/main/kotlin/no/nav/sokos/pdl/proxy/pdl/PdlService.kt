package no.nav.sokos.pdl.proxy.pdl

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
import no.nav.sokos.pdl.proxy.api.model.Ident
import no.nav.sokos.pdl.proxy.api.model.IdentifikatorType
import no.nav.sokos.pdl.proxy.api.model.Person
import no.nav.sokos.pdl.proxy.api.model.PersonDetaljer
import no.nav.sokos.pdl.proxy.exception.PdlApiException
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient

private val logger = KotlinLogging.logger {}

class PdlService(
    private val graphQlClient: GraphQLKtorClient,
    private val pdlUrl: String,
    private val accessTokenClient: AccessTokenClient?,
) {

    fun hentPersonDetaljer(ident: String): PersonDetaljer? {
        val personDetaljer: PersonDetaljer
        val identer: List<Ident>
        val person: Person?

        try {
            logger.info { "henter identer" }
            identer = hentIdenterForPerson(ident)
            logger.info { "henter person" }
            person = hentPerson(ident)

            val hasIdenter = !identer.isEmpty()

            if (person != null && hasIdenter) {
                personDetaljer = PersonDetaljer(
                    identer,
                    person.fornavn,
                    person.mellomnavn,
                    person.etternavn,
                    person.forkortetNavn,
                    null,
                    null,
                    null
                )

                return personDetaljer
            }
        } catch (pdlApiException: PdlApiException) {
            logger.error { "hent person detaljer kaster Pdl api exception" }
            throw pdlApiException
        } catch (exception: Exception) {
            logger.error { "hent person detaljer kaster ubehandlet exception" }
            throw exception
        }

        return null
    }

    fun hentPerson(ident: String): Person? {
        //TODO se på feilhåndteringen her
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
                logger.error { "Det ligger en feil når innkalt ${errors[0].path} og feil blir: ${errors[0].message} " }
                handleErrors(errors)
            }

            if (result.data?.hentPerson?.navn.isNullOrEmpty() == true) {
                logger.warn() { "Det har oppstått en feil ved henting av person fra pdl api - navn er empty" }
                return Person("", "", "", "", null, null, null)
            }

            val bostedAdresse = result.data?.hentPerson?.bostedsadresse
            val kontaktAdresse = result.data?.hentPerson?.kontaktadresse
            val oppholdsAdresse = result.data?.hentPerson?.oppholdsadresse

            return result.data?.hentPerson?.navn?.map {
                Person(it.fornavn,
                    it.mellomnavn,
                    it.etternavn,
                    it.forkortetNavn,
                    bostedAdresse,
                    kontaktAdresse,
                    oppholdsAdresse)
            }?.first()

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
                logger.error { "Det ligger en feil når innkalt ${errors[0].path} og feil blir: ${errors[0].message}" }
                logger.error { "Error code ${errors.mapNotNull { error -> error.extensions }[0].get("code")}. " }
                handleErrors(errors)

            }
        } catch (pdlApiException: PdlApiException) {
            logger.error { "det oppstå en feil med hent identer og kaster Pdl api exception" }

            throw pdlApiException
        } catch (exception: Exception) {
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

    private fun handleErrors(pdlKlientFeil: List<GraphQLClientError>) {
        val feilmeldingPrefiks = "Henting av data fra PDL feilet: "
        val feilmeldingerFraPDL = pdlKlientFeil
            .map { error -> error.message }

        val feilkoderFraPDL = pdlKlientFeil
            .mapNotNull { error -> error.extensions }
            .map { entry -> entry["code"].toString() }

        when {
            feilkoderFraPDL.contains("not_found") -> {
                logger.error { feilmeldingPrefiks + feilmeldingerFraPDL }
                throw PdlApiException(404, "${feilmeldingerFraPDL}")
            }
            else -> {
                logger.error { feilmeldingPrefiks + feilmeldingerFraPDL }
                throw PdlApiException(500, "$feilmeldingerFraPDL")
            }
        }
    }
}

