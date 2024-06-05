package no.nav.sokos.pdl.proxy

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.sokos.pdl.proxy.config.ApplicationState
import no.nav.sokos.pdl.proxy.config.PropertiesConfig
import no.nav.sokos.pdl.proxy.config.applicationLifecycleConfig
import no.nav.sokos.pdl.proxy.config.commonConfig
import no.nav.sokos.pdl.proxy.config.routingConfig
import no.nav.sokos.pdl.proxy.config.securityConfig

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(true)
}

fun Application.module() {
    val applicationState = ApplicationState()
    val applicationConfiguration = PropertiesConfig.Configuration()

    commonConfig()
    applicationLifecycleConfig(applicationState)
    securityConfig(applicationConfiguration.useAuthentication, applicationConfiguration.azureAdProperties)
    routingConfig(applicationConfiguration.useAuthentication, applicationState)
}
