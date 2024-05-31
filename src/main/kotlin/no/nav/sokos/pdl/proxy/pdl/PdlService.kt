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
import no.nav.sokos.pdl.proxy.config.PdlApiException
import no.nav.sokos.pdl.proxy.config.PropertiesConfig
import no.nav.sokos.pdl.proxy.config.SECURE_LOGGER
import no.nav.sokos.pdl.proxy.config.httpClient
import no.nav.sokos.pdl.proxy.security.AccessTokenClient
import java.net.URI

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER)

class PdlService(
    private val pdlUrl: String = PropertiesConfig.PdlProperties().pdlUrl,
    private val pdlScope: String = PropertiesConfig.PdlProperties().pdlScope,
    private val graphQlClient: GraphQLKtorClient =
        GraphQLKtorClient(
            URI(pdlUrl).toURL(),
            httpClient,
        ),
    private val accessTokenClient: AccessTokenClient = AccessTokenClient(scope = pdlScope),
) {
    fun hentPersonDetaljer(ident: String): PersonDetaljer {
        val identer = hentIdenterForPerson(ident).getOrThrow()
        val person = hentPerson(ident).getOrThrow()
        return PersonDetaljer.fra(identer, person)
    }

    private fun hentIdenterForPerson(ident: String): Result<List<Ident>> {
        logger.info { "Henter identer for person" }
        val respons: GraphQLClientResponse<HentIdenter.Result> =
            runBlocking {
                val accessToken = accessTokenClient.hentAccessToken()
                graphQlClient.execute(HentIdenter(HentIdenter.Variables(ident = ident))) {
                    url(pdlUrl)
                    header("Authorization", "Bearer $accessToken")
                }
            }

        return respons.errors?.let { feilmeldingerFraPdl ->
            håndterFeilFraPdl(feilmeldingerFraPdl, ident)
        } ?: Result.success(hentUtIdenter(respons, ident))
    }

    private fun hentUtIdenter(
        result: GraphQLClientResponse<HentIdenter.Result>,
        ident: String,
    ): List<Ident> {
        return result.data?.hentIdenter?.identer?.map {
            Ident(
                ident = it.ident,
                aktiv = !it.historisk,
                identifikatorType = fra(it.gruppe),
            )
        } ?: emptyList<Ident>()
            .also { secureLogger.info { "Henting av Identer for ident: $ident fra PDL vellykket" } }
    }

    private fun hentPerson(ident: String): Result<Person?> {
        logger.info { "Henter person" }
        val respons: GraphQLClientResponse<HentPerson.Result> =
            runBlocking {
                val accessToken = accessTokenClient?.hentAccessToken()
                graphQlClient.execute(HentPerson(HentPerson.Variables(ident = ident))) {
                    url(pdlUrl)
                    header("Authorization", "Bearer $accessToken")
                    header("behandlingsnummer", "B154")
                }
            }

        return respons.errors?.let { feilmeldingerFraPdl ->
            håndterFeilFraPdl(feilmeldingerFraPdl, ident)
        } ?: validerOgBehandleResultat(respons, ident)
    }

    @Suppress("FunctionName")
    private fun <T> håndterFeilFraPdl(
        errors: List<GraphQLClientError>,
        ident: String,
    ): Result<T> {
        val metoderSomGirFeil = errors.joinToString { error -> error.path.toString() }
        val feilmeldingerFraPDL = errors.map { it.message }
        val feilkoderFraPDL =
            errors.flatMap { it -> it.extensions?.get("code")?.toString()?.let { listOf(it) } ?: emptyList() }

        val httpFeilkode =
            when {
                "not_found" in feilkoderFraPDL -> {
                    secureLogger.info { "Person med ident $ident fra PDL ikke funnet" }
                    logger.info { "Person fra PDL ikke funnet" }
                    404
                }

                else -> {
                    secureLogger.error { "$metoderSomGirFeil kallet feilet. Feilmelding: $feilmeldingerFraPDL" }
                    logger.error { "$metoderSomGirFeil kallet feilet. Feilmelding: $feilmeldingerFraPDL" }
                    500
                }
            }

        return Result.failure(PdlApiException(httpFeilkode, feilmeldingerFraPDL.joinToString()))
    }

    private fun validerOgBehandleResultat(
        respons: GraphQLClientResponse<HentPerson.Result>,
        ident: String,
    ): Result<Person?> {
        respons.data?.hentPerson?.also { person ->
            PersonFraPDLValidator.valider(person)
        }
        secureLogger.info { "Henting av Person med ident $ident fra PDL vellykket" }
        return Result.success(respons.data?.hentPerson)
    }
}
