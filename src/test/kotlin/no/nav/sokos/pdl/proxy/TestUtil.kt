package no.nav.sokos.pdl.proxy

const val PDL_PROXY_API_PATH = "/api/pdl-proxy/v1/hent-person"

object TestUtil {
    fun String.readFromResource() =
        TestUtil::class.java.classLoader
            .getResource(this)!!
            .readText()
}
