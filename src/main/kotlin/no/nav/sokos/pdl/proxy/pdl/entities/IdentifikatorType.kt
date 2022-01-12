package no.nav.sokos.pdl.proxy.pdl.entities

import no.nav.pdl.enums.IdentGruppe
import no.nav.person.pdl.aktor.v1.Type

enum class IdentifikatorType(val id: Int) {
    FOLKEREGISTERIDENTIFIKATOR(1),
    N_PID(2);

    companion object {
        fun fra(typeFraPDL: Type): IdentifikatorType {
            return when (typeFraPDL) {
                Type.FOLKEREGISTERIDENTIFIKATOR -> FOLKEREGISTERIDENTIFIKATOR
                Type.N_PID -> N_PID
                else -> {
                    throw IllegalArgumentException("Finner ingen mapping for Type $typeFraPDL i kontoregister person")
                }
            }
        }

        fun fra(typeFraPDL: IdentGruppe): IdentifikatorType {
            return when (typeFraPDL) {
                IdentGruppe.FOLKEREGISTERIDENT -> FOLKEREGISTERIDENTIFIKATOR
                IdentGruppe.NPID -> N_PID
                else -> {
                    throw IllegalArgumentException("Finner ingen mapping for IdentGruppe $typeFraPDL i kontoregister person")
                }
            }
        }

        fun fraId(id: Int): IdentifikatorType {
            return values()
                .first { it.id == id }
        }
    }
}