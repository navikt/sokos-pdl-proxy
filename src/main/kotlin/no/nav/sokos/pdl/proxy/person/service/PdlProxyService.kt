package no.nav.sokos.pdl.proxy.person.service


import mu.KotlinLogging
import no.nav.sokos.pdl.proxy.pdl.entities.Ident
import no.nav.sokos.pdl.proxy.pdl.entities.Person
import no.nav.sokos.pdl.proxy.person.pdl.PdlServiceImpl
import no.nav.sokos.pdl.proxy.person.tid.Naatid
import no.nav.sokos.pdl.proxy.person.tid.NaatidProvider

private val logger = KotlinLogging.logger {}

class PdlProxyService (
    private val pdlServiceImpl: PdlServiceImpl,
    private val nåTid: NaatidProvider = Naatid()
) {
    fun hentPerson(ident: String): Person? {
        return pdlServiceImpl.hentPerson(ident)
    }

    fun hentIdenter(ident: String): List<Ident>? {
        return pdlServiceImpl.hentIdenterForPerson(ident)
    }
}