package no.nav.sokos.pdl.proxy

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import no.nav.sokos.pdl.proxy.person.pdl.PdlServiceImpl
import no.nav.sokos.pdl.proxy.person.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.person.security.ApiSecurityService
import java.net.URL
import kotlin.properties.Delegates

fun main() {
    val appState = ApplicationState()
    val appConfig = Configuration()
    val accessTokenClient = if(appConfig.useAuthentication) AccessTokenClient(appConfig.azureAdClint, defaultHttpClient) else null
    val pdlService = PdlServiceImpl(GraphQLKtorClient(URL(appConfig.pdlUrl), defaultHttpClient), appConfig.pdlUrl, accessTokenClient)
    val securityService = ApiSecurityService(
        appConfig.azureAdServer.apiAllowLists,
        appConfig.azureAdServer.preAutorizedApps)
    val httpServer = HttpServer(appState, appConfig, pdlService = pdlService, securityService)

    httpServer.start()

    appState.running = true

    Runtime.getRuntime().addShutdownHook(Thread {
        appState.running = false
        httpServer.stop()
    })
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