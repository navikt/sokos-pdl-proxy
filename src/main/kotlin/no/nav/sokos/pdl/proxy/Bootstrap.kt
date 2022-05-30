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
    var alive: Boolean by Delegates.observable(alive) { _, _, newValue ->


    }
    var ready: Boolean by Delegates.observable(ready) { _, _, newValue ->

    }
}