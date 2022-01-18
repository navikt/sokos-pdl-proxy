package no.nav.sokos.pdl.proxy.person.pdl


import no.nav.sokos.pdl.proxy.pdl.entities.PersonDetaljer

interface PdlService {
    fun hentPersonDetaljer(ident: String): PersonDetaljer?
}