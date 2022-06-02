package no.nav.sokos.pdl.proxy.api.model

import no.nav.pdl.enums.IdentGruppe
import no.nav.person.pdl.aktor.v1.Type

enum class IdentifikatorType {
    FOLKEREGISTERIDENTIFIKATOR,
    N_PID,
    AKTOR_ID;

    companion object {
        fun fra(typeFraPDL: Type): IdentifikatorType {
            return when (typeFraPDL) {
                Type.FOLKEREGISTERIDENTIFIKATOR -> FOLKEREGISTERIDENTIFIKATOR
                Type.N_PID -> N_PID
                Type.AKTOR_ID -> AKTOR_ID
                else -> {
                    throw IllegalArgumentException("Finner ingen mapping for Type $typeFraPDL i pdl proxy")
                }
            }
        }

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