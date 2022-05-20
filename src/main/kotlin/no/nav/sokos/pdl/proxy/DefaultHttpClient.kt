package no.nav.sokos.pdl.proxy

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.jackson
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import java.net.ProxySelector

val jsonMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

val jsonClientConfiguration: ContentNegotiation.Config.() -> Unit = {
    jsonMapper
    jackson { ContentType.Application.Json }
}

val defaultHttpClient = HttpClient(Apache) {
    expectSuccess = false
    install(ContentNegotiation, jsonClientConfiguration)

    engine {
        customizeClient {
            setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
        }
    }
}
