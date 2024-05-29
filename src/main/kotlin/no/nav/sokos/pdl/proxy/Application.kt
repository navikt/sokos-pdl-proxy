package no.nav.sokos.pdl.proxy

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import no.nav.sokos.pdl.proxy.config.ApplicationState
import no.nav.sokos.pdl.proxy.config.PropertiesConfig
import no.nav.sokos.pdl.proxy.config.applicationLifecycleConfig
import no.nav.sokos.pdl.proxy.config.commonConfig
import no.nav.sokos.pdl.proxy.config.configureSecurity
import no.nav.sokos.pdl.proxy.config.routingConfig
import java.util.concurrent.TimeUnit

fun main() {
    HttpServer(8080).start()
}

fun Application.serverModule() {
    val applicationState = ApplicationState()
    val applicationConfiguration = PropertiesConfig.Configuration()

    commonConfig()
    applicationLifecycleConfig(applicationState)
    configureSecurity(applicationConfiguration.azureAdProperties, applicationConfiguration.useAuthentication)
    routingConfig(applicationState, applicationConfiguration.useAuthentication)
}

private class HttpServer(
    port: Int,
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
            serverModule()
        })

    fun start() {
        embeddedServer.start(wait = true)
    }

    private fun stop() {
        embeddedServer.stop(5, 5, TimeUnit.SECONDS)
    }
}
