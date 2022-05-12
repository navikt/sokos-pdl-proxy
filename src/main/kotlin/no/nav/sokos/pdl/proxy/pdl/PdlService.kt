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
import no.nav.sokos.pdl.proxy.LOGGER
import no.nav.sokos.pdl.proxy.api.model.Ident
import no.nav.sokos.pdl.proxy.api.model.IdentifikatorType
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
                personDetaljer =
                    PersonDetaljer(
                        identer,
                        //TODO pdl leverer liste med navn og adresser. Enten kan vi hente first() eller kan kontrakt endres til å bruke lister
                        person.navn.firstOrNull()?.fornavn,
                        person.navn.firstOrNull()?.mellomnavn,
                        person.navn.firstOrNull()?.etternavn,
                        person.navn.firstOrNull()?.forkortetNavn,
                        person.bostedsadresse.firstOrNull(),
                        person.kontaktadresse.firstOrNull(),
                        person.oppholdsadresse.firstOrNull(),
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
        val result: GraphQLClientResponse<HentPerson.Result> = runBlocking {
            val accessToken = accessTokenClient?.hentAccessToken()
            graphQlClient.execute(HentPerson(HentPerson.Variables(ident = ident))) {
                url(pdlUrl)
                header("Authorization", "Bearer $accessToken")
                header("Tema", "OKO")
            }
        }

        //TODO Secure logg
        logger.info { "Fikk følgende fra PDL hentPerson: ${result.data?.hentPerson}" }

        result.errors?.let { errors ->
            handleErrors(errors)
        }

        return result.data?.hentPerson
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
        val metoderSomGirFeil = pdlKlientFeil
            .mapNotNull { error -> error.path }
            .joinToString { s -> s.toString() }

        val feilmeldingerFraPDL = pdlKlientFeil
            .map { error -> error.message }

        val feilkoderFraPDL = pdlKlientFeil
            .mapNotNull { error -> error.extensions }
            .map { entry -> entry["code"].toString() }


        logger.error { "Henting av data fra PDL feilet ved kall til $metoderSomGirFeil. Feilmeldinger er: $feilmeldingerFraPDL" }
        when {
            feilkoderFraPDL.contains("not_found") -> {
                throw PdlApiException(404, "${feilmeldingerFraPDL}")
            }
            else -> {
                throw PdlApiException(500, "$feilmeldingerFraPDL")
            }
        }
    }
}

