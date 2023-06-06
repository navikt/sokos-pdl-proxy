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
import no.nav.sokos.pdl.proxy.api.model.Ident
import no.nav.sokos.pdl.proxy.api.model.IdentifikatorType.Companion.fra
import no.nav.sokos.pdl.proxy.api.model.PersonDetaljer
import no.nav.sokos.pdl.proxy.config.SECURE_LOGGER_NAME
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.util.PdlApiException

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

    private fun hentPerson(ident: String): Result<Person?> {
        val respons: GraphQLClientResponse<HentPerson.Result> = runBlocking {
            val accessToken = accessTokenClient?.hentAccessToken()
            graphQlClient.execute(HentPerson(HentPerson.Variables(ident = ident))) {
                url(pdlUrl)
                header("Authorization", "Bearer $accessToken")
                header("Tema", "OKO")
            }
        }

        return respons.errors?.let { feilmeldingerFraPdl ->
            håndterFeilFraPdl(feilmeldingerFraPdl, ident)
        } ?: validerOgBehandleResultat(respons, ident)
    }

    private fun validerOgBehandleResultat(
        respons: GraphQLClientResponse<HentPerson.Result>,
        ident: String
    ): Result<Person?> {
        respons.data?.hentPerson?.also { person ->
            PersonFraPDLValidator.valider(person)
        }

        secureLogger.info { "Henting av Person med ident $ident fra PDL vellykket" }

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

        return respons.errors?.let { feilmeldingerFraPdl ->
            håndterFeilFraPdl(feilmeldingerFraPdl, ident)
        } ?: Result.success(hentUtIdenter(respons, ident))
    }

    @Suppress("FunctionName")
    private fun <T> håndterFeilFraPdl(errors: List<GraphQLClientError>, ident: String): Result<T> {
        val metoderSomGirFeil = errors.joinToString { error -> error.path.toString() }
        val feilmeldingerFraPDL = errors.map { it.message }
        val feilkoderFraPDL =
            errors.flatMap { it.extensions?.get("code")?.toString()?.let { listOf(it) } ?: emptyList() }

        val httpFeilkode = when {
            "not_found" in feilkoderFraPDL -> {
                secureLogger.info { "Henting av person med ident $ident fra PDL mislykket" }
                logger.info { "Fant ikke person i PDL ved kall til $metoderSomGirFeil." }
                404
            }

            else -> {
                secureLogger.error { "Henting av data fra PDL feilet ved kall til $metoderSomGirFeil. Feilmeldinger er: $feilmeldingerFraPDL" }
                logger.error { "Henting av data fra PDL feilet ved kall til $metoderSomGirFeil. Feilmeldinger er: $feilmeldingerFraPDL" }
                500
            }
        }

        return Result.failure(PdlApiException(httpFeilkode, feilmeldingerFraPDL.joinToString()))
    }

    private fun hentUtIdenter(result: GraphQLClientResponse<HentIdenter.Result>, ident: String): List<Ident> {
        secureLogger.info { "Henting av Identer med ident $ident fra PDL vellykket" }
        return result.data?.hentIdenter?.identer?.map {
            Ident(
                ident = it.ident,
                aktiv = !it.historisk,
                identifikatorType = fra(it.gruppe)
            )
        } ?: emptyList<Ident>()
            .also { secureLogger.info { "Henting av Identer med ident $ident fra PDL vellykket" } }
    }
}

