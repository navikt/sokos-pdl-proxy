package no.nav.sokos.pdl.proxy.config

import kotlinx.serialization.json.Json

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

val httpClient =
    HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                },
            )
        }
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(5)
            modifyRequest { request ->
                logger.warn { "$retryCount retry feilet mot: ${request.url}" }
            }
            exponentialDelay()
        }
    }
