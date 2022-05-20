package no.nav.sokos.pdl.proxy

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import java.net.URL
import kotlin.properties.Delegates
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.pdl.security.ApiSecurityService

const val SECURE_LOGGER_NAME = "secureLogger"

fun main() {
    val appState = ApplicationState()
    val appConfig = Configuration()
    val accessTokenClient = if(appConfig.useAuthentication) AccessTokenClient(appConfig.azureAdClint, defaultHttpClient) else null
    val pdlService = PdlService(GraphQLKtorClient(URL(appConfig.pdlUrl), defaultHttpClient), appConfig.pdlUrl, accessTokenClient)
    val securityService = ApiSecurityService(
        appConfig.azureAdServer.apiAllowLists,
        appConfig.azureAdServer.preAutorizedApps)
    val httpServer = HttpServer(appState, appConfig, pdlService = pdlService, securityService)



    appState.running = true

    Runtime.getRuntime().addShutdownHook(Thread {
        appState.running = false
        httpServer.stop()
    })
    httpServer.start()
}

class ApplicationState(
    defaultInitialized: Boolean = true,
    defaultRunning: Boolean = false
) {
    var initialized: Boolean by Delegates.observable(defaultInitialized) { _, _, newValue ->


    }
    var running: Boolean by Delegates.observable(defaultRunning) { _, _, newValue ->

    }
}