package no.nav.sokos.pdl.proxy.pdl.entities

data class PersonDetaljer (
    val identer: List<Ident>,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val forkortetNavn: String?,
)