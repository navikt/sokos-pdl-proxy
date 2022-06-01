package no.nav.sokos.pdl.proxy.api.model

import no.nav.pdl.enums.IdentGruppe

enum class IdentifikatorType {
    FOLKEREGISTERIDENTIFIKATOR,
    N_PID,
    AKTOR_ID;

    companion object {
        fun fra(typeFraPDL: IdentGruppe): IdentifikatorType {
            return when (typeFraPDL) {
                IdentGruppe.FOLKEREGISTERIDENT -> FOLKEREGISTERIDENTIFIKATOR
                IdentGruppe.NPID -> N_PID
                IdentGruppe.AKTORID -> AKTOR_ID
                else -> {
                    throw IllegalArgumentException("Finner ingen mapping for IdentGruppe $typeFraPDL i pdl proxy")
                }
            }
        }
    }
}