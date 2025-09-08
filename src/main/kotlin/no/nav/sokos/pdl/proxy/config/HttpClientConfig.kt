package no.nav.sokos.pdl.proxy.config

import java.net.ProxySelector

import kotlinx.serialization.json.Json

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import mu.KotlinLogging
import org.apache.http.impl.conn.SystemDefaultRoutePlanner

private val logger = KotlinLogging.logger {}

val httpClient =
    HttpClient(Apache) {
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

        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 30_000
        }

        engine {
            customizeClient {
                setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            }
        }
    }
