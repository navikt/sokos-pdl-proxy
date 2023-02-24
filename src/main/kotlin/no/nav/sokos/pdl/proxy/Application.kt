package no.nav.sokos.pdl.proxy

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import no.nav.sokos.pdl.proxy.config.PropertiesConfig
import no.nav.sokos.pdl.proxy.config.commonConfig
import no.nav.sokos.pdl.proxy.config.routingConfig
import no.nav.sokos.pdl.proxy.config.securityConfig
import no.nav.sokos.pdl.proxy.metrics.Metrics.appStateReadyFalse
import no.nav.sokos.pdl.proxy.metrics.Metrics.appStateRunningFalse
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.util.httpClient

fun main() {
    val applicationState = ApplicationState()
    val propertiesConfig = PropertiesConfig()
    val accessTokenClient =
        if (propertiesConfig.useAuthentication) AccessTokenClient(
            propertiesConfig.azureAdClint,
            httpClient
        ) else null
    val pdlService =
        PdlService(
            GraphQLKtorClient(URL(propertiesConfig.pdlUrl), httpClient),
            propertiesConfig.pdlUrl,
            accessTokenClient
        )

    HttpServer(applicationState, propertiesConfig, pdlService = pdlService).start()
}

class HttpServer(
    private val applicationState: ApplicationState,
    private val propertiesConfig: PropertiesConfig,
    private val pdlService: PdlService,
    port: Int = 8080,
) {

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            this.stop()
        })
    }

    private val embeddedServer = embeddedServer(Netty, port) {
        commonConfig()
        securityConfig(propertiesConfig, propertiesConfig.useAuthentication)
        routingConfig(applicationState, pdlService, propertiesConfig.useAuthentication)
    }

    fun start() {
        applicationState.running = true
        embeddedServer.start(wait = true)
    }

    private fun stop() {
        applicationState.running = false
        embeddedServer.stop(5, 5, TimeUnit.SECONDS)
    }
}

class ApplicationState(
    alive: Boolean = true,
    ready: Boolean = false
) {
    var initialized: Boolean by Delegates.observable(alive) { _, _, newValue ->
        if (!newValue) appStateReadyFalse.inc()
    }
    var running: Boolean by Delegates.observable(ready) { _, _, newValue ->
        if (!newValue) appStateRunningFalse.inc()
    }
}