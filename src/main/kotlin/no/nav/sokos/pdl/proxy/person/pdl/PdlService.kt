package no.nav.sokos.pdl.proxy.person.pdl

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.pdl.HentPerson
import no.nav.sokos.pdl.proxy.pdl.entities.Person
import no.nav.sokos.pdl.proxy.person.security.AccessTokenClient

class PdlService (
    private val graphQlClient: GraphQLKtorClient,
    private val pdlUrl: String,
    private val accessTokenClient: AccessTokenClient?
) {
    private val logger = KotlinLogging.logger {}

    fun hentPerson(ident: String): Person? {
        //TODO try catch

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
                    Person(it.fornavn, it.mellomnavn, it.etternavn)
                }?.first()

        } catch (exception : Exception) {
            logger.error(exception) {"Det har oppstått en feil ved henting av person $ident" }
            return null
        }
    }
}

