package no.nav.sokos.pdl.proxy

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import java.net.ProxySelector

val jsonMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

val jsonClientConfiguration: JsonFeature.Config.() -> Unit = { serializer = JacksonSerializer(jsonMapper) }

val defaultHttpClient = HttpClient(Apache) {
    expectSuccess = false
    install(JsonFeature, jsonClientConfiguration)
    engine {
        customizeClient {
            setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
        }
    }
}
