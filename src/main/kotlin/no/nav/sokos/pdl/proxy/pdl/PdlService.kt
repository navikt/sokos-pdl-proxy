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
import no.nav.pdl.hentperson.Person
import no.nav.sokos.pdl.proxy.SECURE_LOGGER_NAME
import no.nav.sokos.pdl.proxy.api.model.Ident
import no.nav.sokos.pdl.proxy.api.model.IdentifikatorType.Companion.fra
import no.nav.sokos.pdl.proxy.api.model.PersonDetaljer
import no.nav.sokos.pdl.proxy.exception.PdlApiException
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER_NAME)

class PdlService(
    private val graphQlClient: GraphQLKtorClient,
    private val pdlUrl: String,
    private val accessTokenClient: AccessTokenClient?,
) {

    fun hentPersonDetaljer(ident: String): PersonDetaljer {
        val identer = hentIdenterForPerson(ident).getOrThrow()
        val person = hentPerson(ident).getOrThrow()

        return PersonDetaljer.fra(identer, person)
    }

    fun hentPerson(ident: String): Result<Person?> {
        val respons: GraphQLClientResponse<HentPerson.Result> = runBlocking {
            val accessToken = accessTokenClient?.hentAccessToken()
            graphQlClient.execute(HentPerson(HentPerson.Variables(ident = ident))) {
                url(pdlUrl)
                header("Authorization", "Bearer $accessToken")
                header("Tema", "OKO")
            }
        }

        secureLogger.info { "Fikk følgende fra PDL hentPerson: ${respons.data?.hentPerson}" }

        return respons.errors?.let { feilmeldingerFraPdl ->
            håndterFeilFraPdl(feilmeldingerFraPdl)
        } ?: validerOgBehandleResultat(respons)
    }

    private fun validerOgBehandleResultat(respons: GraphQLClientResponse<HentPerson.Result>): Result<Person?> {
        respons.data?.hentPerson?.let {
        PersonFraPDLValidator.valider(it)
        }

        return Result.success(respons.data?.hentPerson)
    }

    private fun hentIdenterForPerson(ident: String): Result<List<Ident>> {
        val respons: GraphQLClientResponse<HentIdenter.Result> = runBlocking {
            val accessToken = accessTokenClient?.hentAccessToken()
            graphQlClient.execute(HentIdenter(HentIdenter.Variables(ident = ident))) {
                url(pdlUrl)
                header("Authorization", "Bearer $accessToken")
            }
        }
        secureLogger.info { "Fikk følgende fra PDL hentIdenter: ${respons.data?.hentIdenter}" }

        return respons.errors?.let { feilmeldingerFraPdl ->
            håndterFeilFraPdl(feilmeldingerFraPdl)
        } ?: Result.success(hentUtIdenter(respons))
    }

    @Suppress("FunctionName")
    private fun <T> håndterFeilFraPdl(errors: List<GraphQLClientError>): Result<T> {
        val metoderSomGirFeil = errors
            .mapNotNull { error -> error.path }
            .joinToString { s -> s.toString() }
        val feilmeldingerFraPDL = errors
            .map { error -> error.message }
        val feilkoderFraPDL = errors
            .mapNotNull { error -> error.extensions }
            .map { entry -> entry["code"].toString() }
        logger.error { "Henting av data fra PDL feilet ved kall til $metoderSomGirFeil. Feilmeldinger er: $feilmeldingerFraPDL" }

        val httpFeilkode = when {
            feilkoderFraPDL.contains("not_found") -> 404
            else -> 500
        }
        return Result.failure(PdlApiException(httpFeilkode, "$feilmeldingerFraPDL"))
    }

    private fun hentUtIdenter(result: GraphQLClientResponse<HentIdenter.Result>) =
        result.data?.hentIdenter?.identer?.map {
            Ident(
                ident = it.ident,
                aktiv = !it.historisk,
                identifikatorType = fra(it.gruppe)
            )
        } ?: emptyList()

}

