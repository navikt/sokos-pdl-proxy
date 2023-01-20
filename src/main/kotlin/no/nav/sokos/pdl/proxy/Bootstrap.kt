package no.nav.sokos.pdl.proxy

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import java.net.URL
import no.nav.sokos.pdl.proxy.config.Configuration
import no.nav.sokos.pdl.proxy.config.httpClient
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.util.ApplicationState

const val SECURE_LOGGER_NAME = "secureLogger"

fun main() {
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

    val httpServer = HttpServer(applicationState, configuration, pdlService = pdlService)

    applicationState.ready = true

    Runtime.getRuntime().addShutdownHook(Thread {
        applicationState.ready = false
        httpServer.stop()
    })
    httpServer.start()

}