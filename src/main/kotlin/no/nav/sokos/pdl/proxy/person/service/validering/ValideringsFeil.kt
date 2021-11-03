package no.nav.kontoregister.person.service.validering

open class ValideringsFeil(val feilmelding: String) : RuntimeException(feilmelding)