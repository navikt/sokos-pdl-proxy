package no.nav.sokos.pdl.proxy

const val APPLICATION_JSON = "application/json"
const val PDL_PROXY_API_PATH = "/api/pdl-proxy/v1/hent-person"

object TestUtil {
    fun String.readFromResource() = {}::class.java.classLoader.getResource(this)!!.readText()
}
