package no.nav.sokos.pdl.proxy.api.model


enum class NavnKilder {
    FREG,
    PDL;

    companion object {
        fun fra(kilde: String): NavnKilder {
            return when (kilde) {
                "PDL" -> PDL
                "FREG" -> FREG
                else -> {
                    throw IllegalArgumentException("Finner ingen mapping om kilde for navn $kilde i pdl proxy")
                }
            }
        }
    }
}