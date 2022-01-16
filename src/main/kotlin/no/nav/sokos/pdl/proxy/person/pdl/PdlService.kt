package no.nav.sokos.pdl.proxy.person.pdl


import no.nav.sokos.pdl.proxy.pdl.entities.Ident
import no.nav.sokos.pdl.proxy.pdl.entities.Person

interface PdlService {
    fun hentPerson(ident: String): Person?
    fun hentIdenterForPerson(ident: String): List<Ident>
}