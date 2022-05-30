package no.nav.sokos.pdl.proxy

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import java.net.URL
import kotlin.properties.Delegates
import no.nav.sokos.pdl.proxy.config.ApplicationConfiguration
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.pdl.security.ApiSecurityService

const val SECURE_LOGGER_NAME = "secureLogger"

fun main() {
    val appState = ApplicationState()
    val applicationConfiguration = ApplicationConfiguration()
    val accessTokenClient =
        if (applicationConfiguration.useAuthentication) AccessTokenClient(applicationConfiguration.azureAdClint, httpClient) else null
    val pdlService =
        PdlService(GraphQLKtorClient(URL(applicationConfiguration.pdlUrl), httpClient), applicationConfiguration.pdlUrl, accessTokenClient)
    val securityService = ApiSecurityService(
        applicationConfiguration.azureAdServer.apiAllowLists,
        applicationConfiguration.azureAdServer.preAutorizedApps
    )
    val httpServer = HttpServer(appState, applicationConfiguration, pdlService = pdlService, securityService)

    appState.ready = true

    Runtime.getRuntime().addShutdownHook(Thread {
        appState.ready = false
        httpServer.stop()
    })
    httpServer.start()
}

class ApplicationState(
    alive: Boolean = true,
    ready: Boolean = false
) {
    var alive: Boolean by Delegates.observable(alive) { _, _, _ -> }
    var ready: Boolean by Delegates.observable(ready) { _, _, _ -> }
}