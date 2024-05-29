package no.nav.sokos.pdl.proxy

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object TestHelper {
    fun String.readFromResource() = {}::class.java.classLoader.getResource(this)!!.readText()

    fun Any.toJson() = jsonMapper().writeValueAsString(this)!!

    private fun jsonMapper(): ObjectMapper =
        jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            findAndRegisterModules()
        }
}
