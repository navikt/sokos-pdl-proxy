package no.nav.sokos.pdl.proxy.pdl

import java.net.URI

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.request.header
import io.ktor.client.request.url
import mu.KotlinLogging
import org.slf4j.MDC

import no.nav.pdl.HentIdenter
import no.nav.pdl.HentPerson
import no.nav.pdl.hentperson.Person
import no.nav.sokos.pdl.proxy.config.PdlApiException
import no.nav.sokos.pdl.proxy.config.PropertiesConfig
import no.nav.sokos.pdl.proxy.config.TEAM_LOGS_MARKER
import no.nav.sokos.pdl.proxy.config.httpClient
import no.nav.sokos.pdl.proxy.domain.Ident
import no.nav.sokos.pdl.proxy.domain.IdentifikatorType.Companion.fra
import no.nav.sokos.pdl.proxy.domain.PersonDetaljer
import no.nav.sokos.pdl.proxy.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.util.KontaktOgOppholdsAdresseValidator

private val logger = KotlinLogging.logger {}

class PdlClientService(
    private val pdlUrl: String = PropertiesConfig.PdlProperties().pdlUrl,
    private val pdlScope: String = PropertiesConfig.PdlProperties().pdlScope,
    private val graphQlClient: GraphQLKtorClient =
        GraphQLKtorClient(
            URI(pdlUrl).toURL(),
            httpClient,
        ),
    private val accessTokenClient: AccessTokenClient = AccessTokenClient(azureAdScope = pdlScope),
) : AutoCloseable {
    suspend fun hentPersonDetaljer(ident: String): PersonDetaljer {
        val identer = hentIdenterForPerson(ident).getOrThrow()
        val person = hentPerson(ident).getOrThrow()
        return PersonDetaljer.fra(identer, person)
    }

    private suspend fun hentIdenterForPerson(ident: String): Result<List<Ident>> {
        logger.info { "Henter identer for person" }
        val accessToken = accessTokenClient.hentAccessToken()
        val respons: GraphQLClientResponse<HentIdenter.Result> =
            graphQlClient.execute(HentIdenter(HentIdenter.Variables(ident = ident))) {
                url(pdlUrl)
                header("Authorization", "Bearer $accessToken")
            }

        return respons.errors?.let { feilmeldingerFraPdl ->
            handleError(feilmeldingerFraPdl, ident)
        } ?: Result.success(hentUtIdenter(respons, ident))
    }

    private fun hentUtIdenter(
        result: GraphQLClientResponse<HentIdenter.Result>,
        ident: String,
    ): List<Ident> =
        result.data?.hentIdenter?.identer?.map {
            Ident(
                ident = it.ident,
                aktiv = !it.historisk,
                identifikatorType = fra(it.gruppe),
            )
        } ?: emptyList<Ident>()
            .also { logger.info(marker = TEAM_LOGS_MARKER) { "Henting av Identer for ident: $ident fra PDL vellykket" } }

    private suspend fun hentPerson(ident: String): Result<Person?> {
        logger.info { "Henter person" }
        val accessToken = accessTokenClient.hentAccessToken() // Removed unnecessary ? operator
        val respons: GraphQLClientResponse<HentPerson.Result> =
            graphQlClient.execute(HentPerson(HentPerson.Variables(ident = ident))) {
                url(pdlUrl)
                header("Authorization", "Bearer $accessToken")
                header("behandlingsnummer", "B154")
                header("Nav-Call-Id", MDC.get("x-correlation-id"))
            }

        return respons.errors?.let { feilmeldingerFraPdl ->
            handleError(feilmeldingerFraPdl, ident)
        } ?: validerOgBehandleResultat(respons, ident)
    }

    private fun <T> handleError(
        errors: List<GraphQLClientError>,
        ident: String,
    ): Result<T> {
        val metoderSomGirFeil = errors.joinToString { error -> error.path.toString() }
        val feilmeldingerFraPDL = errors.map { it.message }
        val feilkoderFraPDL =
            errors.flatMap { it ->
                it.extensions
                    ?.get("code")
                    ?.toString()
                    ?.let { listOf(it) } ?: emptyList()
            }

        val httpFeilkode =
            when {
                "not_found" in feilkoderFraPDL -> {
                    logger.info(marker = TEAM_LOGS_MARKER) { "Person med ident $ident fra PDL ikke funnet" }
                    logger.info { "Person fra PDL ikke funnet, sjekk Team Logs" }
                    404
                }

                else -> {
                    logger.error(marker = TEAM_LOGS_MARKER) { "$metoderSomGirFeil kallet feilet. Feilmelding: $feilmeldingerFraPDL" }
                    logger.error { "$metoderSomGirFeil kallet feilet. sjekk Team Logs" }
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
            KontaktOgOppholdsAdresseValidator.valider(person)
        }
        logger.info(marker = TEAM_LOGS_MARKER) { "Henting av Person med ident $ident fra PDL vellykket" }
        return Result.success(respons.data?.hentPerson)
    }

    override fun close() {
        try {
            graphQlClient.close()
        } catch (e: Exception) {
            logger.warn { "Error closing GraphQL client: ${e.message}" }
        }
    }
}
