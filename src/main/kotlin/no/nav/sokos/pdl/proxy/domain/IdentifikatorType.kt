package no.nav.sokos.pdl.proxy.domain

import no.nav.pdl.enums.IdentGruppe

enum class IdentifikatorType {
    FOLKEREGISTERIDENTIFIKATOR,
    N_PID,
    ;

    companion object {
        fun fra(typeFraPDL: IdentGruppe): IdentifikatorType {
            return when (typeFraPDL) {
                IdentGruppe.FOLKEREGISTERIDENT -> FOLKEREGISTERIDENTIFIKATOR
                IdentGruppe.NPID -> N_PID
                else -> {
                    throw IllegalArgumentException("Finner ingen mapping for IdentGruppe $typeFraPDL i pdl proxy")
                }
            }
        }
    }
}
