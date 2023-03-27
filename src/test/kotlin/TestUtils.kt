import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import no.nav.sokos.pdl.proxy.ApplicationState
import no.nav.sokos.pdl.proxy.config.commonConfig
import no.nav.sokos.pdl.proxy.config.routingConfig
import no.nav.sokos.pdl.proxy.pdl.PdlService

fun String.readFromResource() = {}::class.java.classLoader.getResource(this)!!.readText()
fun Any.toJson() = jsonMapper().writeValueAsString(this)!!

private fun jsonMapper(): ObjectMapper = jacksonObjectMapper().apply {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    findAndRegisterModules()
}

class EmbeddedTestServer(
    private val port: Int = 1100,
    private val pdlService: PdlService,
    private val applicationState: ApplicationState,

    ) {
    init {
        embeddedServer(Netty, port) {
            commonConfig()
            routingConfig(applicationState, pdlService, false)
            RestAssured.baseURI = "http://localhost"
            RestAssured.basePath = "/api/pdl-proxy/v1"
            RestAssured.port = port
            RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory { _, _ -> no.nav.sokos.pdl.proxy.util.jsonMapper }
            )
        }.start()
    }
}