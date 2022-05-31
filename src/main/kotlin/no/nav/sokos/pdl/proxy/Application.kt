package no.nav.sokos.pdl.proxy

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import java.net.URL
import no.nav.sokos.pdl.proxy.config.ApplicationProperties
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.pdl.security.ApiSecurityService

const val SECURE_LOGGER_NAME = "secureLogger"

fun main() {
    val applicationState = ApplicationState()
    val applicationProperties = ApplicationProperties()
    val accessTokenClient =
        if (applicationProperties.useAuthentication) AccessTokenClient(
            applicationProperties.azureAdClint,
            httpClient
        ) else null
    val pdlService =
        PdlService(
            GraphQLKtorClient(URL(applicationProperties.pdlUrl), httpClient),
            applicationProperties.pdlUrl,
            accessTokenClient
        )
    val securityService = ApiSecurityService(
        applicationProperties.azureAdServer.apiAllowLists,
        applicationProperties.azureAdServer.preAutorizedApps
    )
    val httpServer = HttpServer(applicationState, applicationProperties, pdlService = pdlService, securityService)

    applicationState.ready = true

    Runtime.getRuntime().addShutdownHook(Thread {
        applicationState.ready = false
        httpServer.stop()
    })
    httpServer.start()
}