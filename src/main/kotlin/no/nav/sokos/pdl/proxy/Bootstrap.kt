package no.nav.sokos.pdl.proxy

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.sokos.pdl.proxy.config.Configuration
import no.nav.sokos.pdl.proxy.config.httpClient
import no.nav.sokos.pdl.proxy.pdl.PdlService
import no.nav.sokos.pdl.proxy.pdl.security.AccessTokenClient
import no.nav.sokos.pdl.proxy.pdl.security.ApiSecurityService
import no.nav.sokos.pdl.proxy.util.ApplicationState
import java.net.URL

const val SECURE_LOGGER_NAME = "secureLogger"

fun main() {
    val applicationState = ApplicationState()
    val configuration = Configuration()
    val prometheusMeterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val allNamesCounter: Counter =  Counter.builder("pdl.person")
        .tag("name", "ALL")
        .description("The number of persons having two active names")
        .register(prometheusMeterRegistry);
    val fregNamesCounter: Counter =  Counter.builder("pdl.person")
        .tag("name", "FREG")
        .description("The number of person name from FREG in use")
        .register(prometheusMeterRegistry);
    val pdlNamesCounter: Counter = Counter.builder("pdl.person")
        .tag("name", "PDL")
        .description("The number of person name from PDL in use")
        .register(prometheusMeterRegistry);
    val accessTokenClient =
        if (configuration.useAuthentication) AccessTokenClient(
            configuration.azureAdClint,
            httpClient
        ) else null
    val pdlService =
        PdlService(
            GraphQLKtorClient(URL(configuration.pdlUrl), httpClient),
            configuration.pdlUrl,
            accessTokenClient,
            allNamesCounter,
            fregNamesCounter,
            pdlNamesCounter
        )
    val apiSecurityService = ApiSecurityService(
        configuration.azureAdServer.apiAllowLists,
        configuration.azureAdServer.preAutorizedApps
    )
    val httpServer = HttpServer(applicationState, configuration, pdlService = pdlService, apiSecurityService)

    applicationState.ready = true

    Runtime.getRuntime().addShutdownHook(Thread {
        applicationState.ready = false
        httpServer.stop()
    })
    httpServer.start()



}