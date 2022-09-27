package no.nav.sokos.pdl.proxy

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import mu.KotlinLogging
import java.net.URL
import no.nav.sokos.pdl.proxy.config.Configuration
import no.nav.sokos.pdl.proxy.config.httpClient
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.pdl.security.ApiSecurityService
import no.nav.sokos.pdl.proxy.util.ApplicationState

const val SECURE_LOGGER_NAME = "secureLogger"
val logger = KotlinLogging.logger {}

fun main() {
    val applicationState = ApplicationState()
    val configuration = Configuration()
    try {
        val accessTokenClient =
            if (configuration.useAuthentication) AccessTokenClient(
                configuration.azureAdClint,
                httpClient
            ) else null
        try {

            val pdlService =
                PdlService(
                    GraphQLKtorClient(URL(configuration.pdlUrl), httpClient),
                    configuration.pdlUrl,
                    accessTokenClient
                )
            try {

                val apiSecurityService = ApiSecurityService(
                    configuration.azureAdServer.apiAllowLists,
                    configuration.azureAdServer.preAutorizedApps
                )
                try {

                    val httpServer = HttpServer(applicationState, configuration, pdlService = pdlService, apiSecurityService)

                    applicationState.ready = true

                    Runtime.getRuntime().addShutdownHook(Thread {
                        applicationState.ready = false
                        httpServer.stop()
                    })
                    httpServer.start()
                } catch (ex: Exception) {
                    logger.error { "Exception å skape httpServer ${ex}" }
                    logger.error { "${ex.stackTrace}" }
                }
            } catch (e: Exception) {
                logger.error { "Exception på apiSecurityService ${e}" }
                logger.error { "${e.stackTrace}" }
            }
        } catch (error: Exception) {
            logger.error { "Exception på PdlService: ${error} " }
            logger.error { "${error.stackTrace}" }
        }
    } catch (exception: Exception) {
        logger.error { "Exception på hent av accessToken: ${exception}" }
        logger.error { "${exception.stackTrace}" }
    }
}