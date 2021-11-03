import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.sokos.ereg.proxy.api.installCommonFeatures
import no.nav.sokos.ereg.proxy.api.naisApi
import no.nav.sokos.pdl.proxy.Configuration
import no.nav.sokos.pdl.proxy.api.pdlApi
import no.nav.sokos.pdl.proxy.defaultHttpClient
import no.nav.sokos.pdl.proxy.person.pdl.PdlService
import no.nav.sokos.pdl.proxy.person.security.AccessTokenClient
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

fun main() {
    val appState = ApplicationState()
    val appConfig = Configuration()
    val accessTokenClient = if(appConfig.useAuthentication) AccessTokenClient(appConfig.azureAdClint, defaultHttpClient) else null
    val pdlService = PdlService(GraphQLKtorClient(URL(appConfig.pdlUrl), defaultHttpClient), appConfig.pdlUrl, accessTokenClient)
    val httpServer = HttpServer(appState, pdlService = pdlService)

    httpServer.start()

    appState.running = true

    Runtime.getRuntime().addShutdownHook(Thread {
        appState.running = false
        httpServer.stop()
    })
}

class HttpServer(
    appState: ApplicationState,
    port: Int = 8080,
    pdlService: PdlService
) {
    private val embeddedServer = embeddedServer(Netty, port) {
        installCommonFeatures()
        pdlApi(pdlService = pdlService)
        naisApi({ appState.initialized }, { appState.running })
    }

    fun start() = embeddedServer.start()
    fun stop() = embeddedServer.stop(5, 5, TimeUnit.SECONDS)
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