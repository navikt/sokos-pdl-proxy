package no.nav.sokos.pdl.proxy.person.tid

import java.time.LocalDateTime

interface NaatidProvider {
    fun nåtid(): LocalDateTime
}