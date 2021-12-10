package no.nav.sokos.pdl.proxy.person.service


import mu.KotlinLogging
import no.nav.sokos.pdl.proxy.pdl.entities.Person
import no.nav.sokos.pdl.proxy.pdl.entities.PersonIdent
import no.nav.sokos.pdl.proxy.person.domain.Ident
import no.nav.sokos.pdl.proxy.person.pdl.PdlService
import no.nav.sokos.pdl.proxy.person.tid.Naatid
import no.nav.sokos.pdl.proxy.person.tid.NaatidProvider

private val logger = KotlinLogging.logger {}

class PdlProxyService (
    private val pdlService: PdlService,
    private val nåTid: NaatidProvider = Naatid()
) {
    fun hentPerson(ident: String): Person? {
        return pdlService.hentPerson(ident)
    }

    fun hentIdenter(ident: String): List<PersonIdent>? {
        return pdlService.hentIdenterForPerson(ident)
    }
}