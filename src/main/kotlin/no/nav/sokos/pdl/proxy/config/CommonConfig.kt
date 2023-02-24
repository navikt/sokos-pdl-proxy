package no.nav.sokos.pdl.proxy.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import java.util.UUID
import mu.KotlinLogging
import no.nav.sokos.pdl.proxy.metrics.Metrics
import no.nav.sokos.pdl.proxy.util.exceptionHandler
import org.slf4j.event.Level

private val logger = KotlinLogging.logger {}
const val SECURE_LOGGER_NAME = "secureLogger"
const val X_CORRELATION_ID = "x-correlation-id"

fun Application.commonConfig() {
    install(StatusPages) {
        exceptionHandler()
    }
    install(CallId) {
        header(X_CORRELATION_ID)
        generate { UUID.randomUUID().toString() }
        verify { it.isNotEmpty() }
    }
    install(CallLogging) {
        logger = no.nav.sokos.pdl.proxy.config.logger
        level = Level.INFO
        callIdMdc(X_CORRELATION_ID)
        filter { call -> call.request.path().startsWith("/api/pdl-proxy") }
        disableDefaultColors()
    }
    install(ContentNegotiation) {
        jackson {
            findAndRegisterModules()
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            enable(SerializationFeature.INDENT_OUTPUT)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }
    install(MicrometerMetrics) {
        registry = Metrics.prometheusRegistry
        meterBinders = listOf(
            UptimeMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            JvmThreadMetrics(),
            ProcessorMetrics()
        )
    }
}
