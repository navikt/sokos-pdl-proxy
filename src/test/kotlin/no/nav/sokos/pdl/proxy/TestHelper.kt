package no.nav.sokos.pdl.proxy

object TestHelper {
    fun String.readFromResource() = {}::class.java.classLoader.getResource(this)!!.readText()
}
