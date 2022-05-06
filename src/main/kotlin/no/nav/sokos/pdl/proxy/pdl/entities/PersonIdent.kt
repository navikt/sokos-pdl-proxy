package no.nav.sokos.pdl.proxy.pdl.entities

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class PersonIdent(
    val ident: String,
) {
    fun tilJson(): String = jacksonObjectMapper().writeValueAsString(this)
}