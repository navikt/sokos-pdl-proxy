package no.nav.sokos.pdl.proxy.person.pdl

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.pdl.HentIdenter
import no.nav.pdl.HentPerson
import no.nav.sokos.pdl.proxy.pdl.entities.Person
import no.nav.sokos.pdl.proxy.person.domain.Ident
import no.nav.sokos.pdl.proxy.person.security.AccessTokenClient

class PdlService (
    private val graphQlClient: GraphQLKtorClient,
    private val pdlUrl: String,
    private val accessTokenClient: AccessTokenClient?
) {
    private val logger = KotlinLogging.logger {}

    fun hentPerson(ident: String): Person? {

        try {
            val result: GraphQLClientResponse<HentPerson.Result> =  runBlocking {
                val accessToken = accessTokenClient?.hentAccessToken()
                graphQlClient.execute(HentPerson(HentPerson.Variables(ident = ident))) {
                    url(pdlUrl)
                    header("Authorization", "Bearer $accessToken")
                    header("Tema", "OKO")
                }
            }
            return if (result.errors?.isNotEmpty() == true) {
                //TODO handle errors og ikke logge så kaste
                logger.error { "Feil i GraphQL-responsen: ${result.errors}" }
                throw Exception("feil med henting av identer")
            } else
                result.data?.hentPerson?.navn?.map {
                    Person(it.fornavn, it.mellomnavn, it.etternavn, it.forkortetNavn)
                }?.first()

        } catch (exception : Exception) {
            logger.error(exception) {"Det har oppstått en feil ved henting av person" }
            return null
        }
    }

    fun hentIdenterForPerson(ident: String): List<Ident> {
        //TODO try catch
        try {
            val result: GraphQLClientResponse<HentIdenter.Result> = runBlocking {
                val accessToken = accessTokenClient?.hentAccessToken()
                graphQlClient.execute(HentIdenter(HentIdenter.Variables(ident = ident))) {
                    url(pdlUrl)
                    header("Authorization", "Bearer $accessToken")
                }
            }

            return result.errors?.let { errors ->
                if (errors.isEmpty()) {
                    hentUtIdenter(result)
                } else {
                    handleErrors(errors)
                }
            } ?: hentUtIdenter(result)
        } catch (exception : Exception) {
            logger.error(exception) {"Det har oppstått en feil ved henting av identer" }
            return emptyList()
        }

    }

    private fun hentUtIdenter(result: GraphQLClientResponse<HentIdenter.Result>) =
        result.data?.hentIdenter?.identer?.map { Ident(it.ident, !it.historisk) } ?: emptyList()

    private fun handleErrors(errors: List<GraphQLClientError>): List<Ident> {
        val ikkeFunnetResponsFraPDL = errors
            .mapNotNull { error -> error.extensions }
            .any { entry -> entry["code"] == "not_found" }

        if (ikkeFunnetResponsFraPDL) {
            return emptyList()
        } else {
            logger.error { "Feil i GraphQL-responsen: $errors" }
            throw Exception("feil med henting av identer")
        }
    }
}

