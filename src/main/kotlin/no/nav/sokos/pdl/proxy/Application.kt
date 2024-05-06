package no.nav.sokos.pdl.proxy

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import no.nav.sokos.pdl.proxy.config.PropertiesConfig
import no.nav.sokos.pdl.proxy.config.commonConfig
import no.nav.sokos.pdl.proxy.config.routingConfig
import no.nav.sokos.pdl.proxy.config.securityConfig
import no.nav.sokos.pdl.proxy.metrics.Metrics.appStateReadyFalse
import no.nav.sokos.pdl.proxy.metrics.Metrics.appStateRunningFalse
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.util.httpClient
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

fun main() {
    val applicationState = ApplicationState()
    val applicationConfiguration = PropertiesConfig.Configuration()
    val accessTokenClient =
        if (applicationConfiguration.useAuthentication) {
            AccessTokenClient(
                applicationConfiguration.azureAdClientConfig,
                httpClient,
            )
        } else {
            null
        }
    val pdlService =
        PdlService(
            GraphQLKtorClient(URI(applicationConfiguration.pdlConfig.pdlUrl).toURL(), httpClient),
            applicationConfiguration.pdlConfig.pdlUrl,
            accessTokenClient,
        )

    HttpServer(applicationState, applicationConfiguration, pdlService).start()
}

private class HttpServer(
    private val applicationState: ApplicationState,
    private val applicationConfiguration: PropertiesConfig.Configuration,
    private val pdlService: PdlService,
    port: Int = 8080,
) {
    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                this.stop()
            },
        )
    }

    private val embeddedServer =
        embeddedServer(Netty, port, module = {
            applicationModule(applicationConfiguration, applicationState, pdlService)
        })

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
    ready: Boolean = false,
) {
    var initialized: Boolean by Delegates.observable(alive) { _, _, newValue ->
        if (!newValue) appStateReadyFalse.inc()
    }
    var running: Boolean by Delegates.observable(ready) { _, _, newValue ->
        if (!newValue) appStateRunningFalse.inc()
    }
}

fun Application.applicationModule(
    applicationConfiguration: PropertiesConfig.Configuration,
    applicationState: ApplicationState,
    pdlService: PdlService,
) {
    commonConfig()
    securityConfig(applicationConfiguration.azureAdServerConfig, applicationConfiguration.useAuthentication)
    routingConfig(applicationState, pdlService, applicationConfiguration.useAuthentication)
}
