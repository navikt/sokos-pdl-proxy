import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.sokos.ereg.proxy.api.commonFeatures
import no.nav.sokos.ereg.proxy.api.naisApi
import no.nav.sokos.pdl.proxy.Configuration
import no.nav.sokos.pdl.proxy.api.pdlApi
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

fun main() {
    val appState = ApplicationState()
    val appConfig = Configuration()

    val httpServer = HttpServer(appState)

    httpServer.start()

    appState.running = true

    Runtime.getRuntime().addShutdownHook(Thread {
        appState.running = false
        httpServer.stop()
    })
}

class HttpServer(
    appState: ApplicationState,
    port: Int = 8083,
) {
    private val embeddedServer = embeddedServer(Netty, port) {
        commonFeatures()
        pdlApi()
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