package no.nav.sokos.pdl.proxy.api.model

import io.micrometer.core.instrument.Counter
import no.nav.pdl.hentperson.*
import java.text.SimpleDateFormat

data class PersonDetaljer(
    val identer: List<Ident>,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val forkortetNavn: String?,
    val bostedsadresse: Bostedsadresse?,
    val kontaktadresse: List<Kontaktadresse>,
    val oppholdsadresse: List<Oppholdsadresse>,
) {
    companion object {
        fun fra(
            identer: List<Ident>,
            person: Person?,
            allNamesCounter: Counter,
            fregNamesCounter: Counter,
            pdlNamesCounter: Counter): PersonDetaljer {
            if (person?.navn?.size == 1) {

                return PersonDetaljer(
                    identer,
                    person.navn.firstOrNull()?.fornavn,
                    person.navn.firstOrNull()?.mellomnavn,
                    person.navn.firstOrNull()?.etternavn,
                    person.navn.firstOrNull()?.forkortetNavn,
                    person.bostedsadresse?.firstOrNull(),
                    person.kontaktadresse.orEmpty(),
                    person.oppholdsadresse.orEmpty(),
                )
            }


            val navnFraPdlKilde : Navn? = person?.navn?.filter { navn -> navn.metadata.master.equals(NavnKilder.PDL.toString(), ignoreCase = true) }?.firstOrNull()
            val navnFraFregKilde: Navn? = person?.navn?.filter { navn -> navn.metadata.master.equals(NavnKilder.FREG.toString(), ignoreCase = true) }?.firstOrNull()
            allNamesCounter.increment()

            if (null == navnFraPdlKilde?.gyldigFraOgMed || null == navnFraFregKilde?.gyldigFraOgMed) {
                pdlNamesCounter.increment()
                return PersonDetaljer(
                    identer,
                    navnFraPdlKilde?.fornavn,
                    navnFraPdlKilde?.mellomnavn,
                    navnFraPdlKilde?.etternavn,
                    navnFraPdlKilde?.forkortetNavn,
                    person?.bostedsadresse?.firstOrNull(),
                    person?.kontaktadresse.orEmpty(),
                    person?.oppholdsadresse.orEmpty(),
                )
            }
            val dateComparison = sammenlikneDatoerFraForskelligeKilder(navnFraPdlKilde.gyldigFraOgMed, navnFraFregKilde.gyldigFraOgMed)

            when {
                dateComparison > 0 -> {
                    pdlNamesCounter.increment()
                    return PersonDetaljer(
                        identer,
                        navnFraPdlKilde.fornavn,
                        navnFraPdlKilde.mellomnavn,
                        navnFraPdlKilde.etternavn,
                        navnFraPdlKilde.forkortetNavn,
                        person.bostedsadresse?.firstOrNull(),
                        person.kontaktadresse.orEmpty(),
                        person.oppholdsadresse.orEmpty(),
                    )
                }

                dateComparison < 0 -> {
                    fregNamesCounter.increment()
                    return PersonDetaljer(
                        identer,
                        navnFraFregKilde.fornavn,
                        navnFraFregKilde.mellomnavn,
                        navnFraFregKilde.etternavn,
                        navnFraFregKilde.forkortetNavn,
                        person.bostedsadresse?.firstOrNull(),
                        person.kontaktadresse.orEmpty(),
                        person.oppholdsadresse.orEmpty(),
                    )
                }

                else -> {
                    pdlNamesCounter.increment()
                    return PersonDetaljer(
                        identer,
                        navnFraPdlKilde.fornavn,
                        navnFraPdlKilde.mellomnavn,
                        navnFraPdlKilde.etternavn,
                        navnFraPdlKilde.forkortetNavn,
                        person.bostedsadresse?.firstOrNull(),
                        person.kontaktadresse.orEmpty(),
                        person.oppholdsadresse.orEmpty(),
                    )
                }
            }
        }

        private fun sammenlikneDatoerFraForskelligeKilder(datoFraPdlKilde: String, datoFraFregKilde: String): Int {
            val dateFormater = SimpleDateFormat("yyyy-MM-dd")
            val endringDatoFraPdlKile = dateFormater.parse(datoFraPdlKilde)
            val endringDatoFraFregKilde = dateFormater.parse(datoFraFregKilde)

            val dateComparison = endringDatoFraPdlKile.compareTo(endringDatoFraFregKilde)
            return dateComparison
        }
    }
}


