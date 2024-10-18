package no.nav.sokos.pdl.proxy

import no.nav.pdl.enums.Endringstype
import no.nav.pdl.enums.KontaktadresseType
import no.nav.pdl.hentperson.Bostedsadresse
import no.nav.pdl.hentperson.Endring
import no.nav.pdl.hentperson.Kontaktadresse
import no.nav.pdl.hentperson.Metadata
import no.nav.pdl.hentperson.Metadata2
import no.nav.pdl.hentperson.Navn
import no.nav.pdl.hentperson.Oppholdsadresse
import no.nav.pdl.hentperson.Person
import no.nav.pdl.hentperson.PostadresseIFrittFormat
import no.nav.pdl.hentperson.Vegadresse
import no.nav.sokos.pdl.proxy.domain.Ident
import no.nav.sokos.pdl.proxy.domain.IdentifikatorType
import no.nav.sokos.pdl.proxy.domain.PersonDetaljer

object TestData {
    fun mockPersonDetaljer(): PersonDetaljer {
        return PersonDetaljer(
            identer = listOf(Ident(ident = "24117920441", aktiv = true, identifikatorType = IdentifikatorType.FOLKEREGISTERIDENTIFIKATOR)),
            fornavn = "Ola",
            mellomnavn = "mellomnavn",
            etternavn = "Nordmann",
            forkortetNavn = "STAUDE ÅPENHJERTIG",
            bostedsadresse =
                Bostedsadresse(
                    angittFlyttedato = "1979-11-24",
                    gyldigFraOgMed = "1979-11-24T00:00",
                    gyldigTilOgMed = "2020-11-24T00:00",
                    coAdressenavn = "null",
                    vegadresse =
                        Vegadresse(
                            husnummer = "55",
                            husbokstav = null,
                            bruksenhetsnummer = null,
                            adressenavn = "HUSANTUNVEIEN",
                            kommunenummer = "3024",
                            bydelsnummer = null,
                            tilleggsnavn = null,
                            postnummer = "1358",
                        ),
                    matrikkeladresse = null,
                    utenlandskAdresse = null,
                    ukjentBosted = null,
                    metadata =
                        Metadata2(
                            opplysningsId = "b3293355-1cde-4299-a99c-bad2984e694e",
                            master = "FREG",
                            endringer =
                                listOf(
                                    Endring(
                                        Endringstype.OPPRETT,
                                        registrert = "2020-12-08T14:32:26",
                                        registrertAv = "Folkeregisteret",
                                        systemkilde = "FREG",
                                        kilde = "Dolly",
                                    ),
                                ),
                            historisk = false,
                        ),
                ),
            kontaktadresse = mockKontaktAdresser(1),
            oppholdsadresse = mockOppholdsAdresser(1),
        )
    }

    fun mockPerson(): Person {
        return Person(
            navn =
                listOf(
                    Navn(
                        fornavn = "ÅPENHJERTIG",
                        mellomnavn = null,
                        etternavn = "STAUDE",
                        metadata =
                            Metadata(
                                master = "FREG",
                            ),
                    ),
                ),
            bostedsadresse = emptyList(),
            oppholdsadresse = emptyList(),
            kontaktadresse = emptyList(),
        )
    }

    fun mockKontaktAdresser(antallKontaktadresse: Int): List<Kontaktadresse> {
        return List(antallKontaktadresse) {
            Kontaktadresse(
                gyldigFraOgMed = "1979-11-24T00:00",
                gyldigTilOgMed = null,
                type = KontaktadresseType.INNLAND,
                coAdressenavn = null,
                postboksadresse = null,
                vegadresse =
                    Vegadresse(
                        husnummer = "55",
                        husbokstav = null,
                        bruksenhetsnummer = null,
                        adressenavn = "HUSANTUNVEIEN",
                        kommunenummer = null,
                        bydelsnummer = null,
                        tilleggsnavn = null,
                        postnummer = "1358",
                    ),
                postadresseIFrittFormat =
                    PostadresseIFrittFormat(
                        adresselinje1 = "adresse 1",
                        adresselinje2 = "adresse 2",
                        adresselinje3 = "adresse 3",
                        postnummer = "4242",
                    ),
                utenlandskAdresse = null,
                utenlandskAdresseIFrittFormat = null,
                metadata =
                    Metadata2(
                        opplysningsId = "6ae8f985-0763-441e-ad33-59ae9f01ad49",
                        master = "FREG",
                        endringer =
                            listOf(
                                Endring(
                                    type = Endringstype.OPPRETT,
                                    registrert = "2020-12-08T14:32:25",
                                    registrertAv = "Folkeregisteret",
                                    systemkilde = "FREG",
                                    kilde = "Dolly",
                                ),
                            ),
                        historisk = false,
                    ),
            )
        }
    }

    fun mockOppholdsAdresser(antallOppholdsadresse: Int): List<Oppholdsadresse> {
        return List(antallOppholdsadresse) {
            Oppholdsadresse(
                oppholdAnnetSted = null,
                coAdressenavn = null,
                gyldigFraOgMed = "1979-11-24T00:00",
                gyldigTilOgMed = "2020-11-24T00:00",
                utenlandskAdresse = null,
                vegadresse =
                    Vegadresse(
                        husnummer = "55",
                        husbokstav = null,
                        bruksenhetsnummer = null,
                        adressenavn = "HUSANTUNVEIEN",
                        kommunenummer = "3024",
                        bydelsnummer = null,
                        tilleggsnavn = null,
                        postnummer = "1358",
                    ),
                matrikkeladresse = null,
                metadata =
                    Metadata2(
                        opplysningsId = "e498f445-704a-4b05-8322-7b9fe4e734b2",
                        master = "FREG",
                        endringer =
                            listOf(
                                Endring(
                                    type = Endringstype.OPPRETT,
                                    registrert = "2020-12-08T14:32:25",
                                    registrertAv = "Folkeregisteret",
                                    systemkilde = "FREG",
                                    kilde = "Dolly",
                                ),
                            ),
                        historisk = false,
                    ),
            )
        }
    }
}
