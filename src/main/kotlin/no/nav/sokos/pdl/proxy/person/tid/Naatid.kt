package no.nav.sokos.pdl.proxy.person.tid

import java.time.LocalDateTime

class Naatid : NaatidProvider {

    override fun nåtid(): LocalDateTime {
        return LocalDateTime.now()
    }
}
