package no.nav.sokos.pdl.proxy.api.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class PersonIdent(
    val ident: String,
) {
    fun tilJson(): String = jacksonObjectMapper().writeValueAsString(this)
}