package no.nav.sokos.pdl.proxy

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import java.net.URL
import mu.KotlinLogging
import no.nav.sokos.pdl.proxy.config.Configuration
import no.nav.sokos.pdl.proxy.config.httpClient
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.pdl.security.ApiSecurityService
import no.nav.sokos.pdl.proxy.util.ApplicationState

private val logger = KotlinLogging.logger {}
const val SECURE_LOGGER_NAME = "secureLogger"

fun main() {

    try {
        val applicationState = ApplicationState()
        val configuration = Configuration()

        val accessTokenClient =
            if (configuration.useAuthentication) AccessTokenClient(
                configuration.azureAdClint,
                httpClient
            ) else null

        val pdlService =
            PdlService(
                GraphQLKtorClient(URL(configuration.pdlUrl), httpClient),
                configuration.pdlUrl,
                accessTokenClient
            )
        val apiSecurityService = ApiSecurityService(
            configuration.azureAdServer.apiAllowLists,
            configuration.azureAdServer.preAutorizedApps
        )
        val httpServer = HttpServer(applicationState, configuration, pdlService = pdlService, apiSecurityService)

        applicationState.ready = true

        Runtime.getRuntime().addShutdownHook(Thread {
            applicationState.ready = false
            httpServer.stop()
        })
        httpServer.start()
    } catch (ex: Throwable) {
        logger.error("Feil i oppstart", ex)
        throw ex
    }
}