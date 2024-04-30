package no.nav.sokos.pdl.proxy.api.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.nav.pdl.hentperson.Metadata
import no.nav.pdl.hentperson.Navn
import no.nav.pdl.hentperson.Person
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

class PersonDetaljerTest {

    @Test
    fun `Bare tomme lister går bra`() {
        assertDoesNotThrow {
            PersonDetaljer.fra(emptyList(), testperson())
        }
    }

    @Test
    fun `Skal overføre navn til persondetaljer`() {
        assertThat(
            testPersondetaljer(
                TestNavn(AKTIVT, endret = "2020-02-02T02:02:02", navn = "Aron Åberg")
            ).fornavn
        ).isEqualTo("Aron")
    }


    @Test
    fun `Sammenligning av navn med tomme lister av endringer feiler men ett navn går bra`() {
        assertDoesNotThrow {
            PersonDetaljer.fra(
                emptyList(),
                Person(
                    listOf(
                        Navn(
                            fornavn = "Ingen",
                            etternavn = "Endringer",
                            metadata = Metadata(PDL)
                        )
                    ), emptyList(), emptyList(), emptyList()
                )
            )
        }
    }

    @Test
    fun `Skal velge senest endret aktiv navn når det nyeste er historisk og overstyre PDL`() {
        assertThat(
            testPersondetaljer(
                TestNavn(AKTIVT, endret = "2020-02-02T02:02:02", navn = "Aron Åberg", master = PDL),
                TestNavn(HISTORISK, endret = "2021-02-02T02:02:02", navn = "Berit Berger", master = FREG)
            ).fornavn
        ).isEqualTo("Aron")
    }

    @Test
    fun `Skal velge nyeste aktive navn fra PDL`() {
        assertThat(
            testPersondetaljer(
                TestNavn(HISTORISK, endret = "2020-02-02T02:02:02", navn = "Aron Åberg", master = FREG),
                TestNavn(HISTORISK, endret = "2021-02-02T02:02:02", navn = "Berit Berger", master = FREG),
                TestNavn(HISTORISK, endret = "2022-02-02T02:02:02", navn = "Charlie Carlsson", master = FREG),
                TestNavn(AKTIVT, endret = "2023-02-02T02:02:02", navn = "David Danestad", master = FREG),
                TestNavn(AKTIVT, endret = "2024-02-02T02:02:02", navn = "Erik Eriksen", master = PDL),
                TestNavn(AKTIVT, endret = "2025-02-02T02:02:02", navn = "Fredrik Fransen", master = FREG),
                TestNavn(HISTORISK, endret = "2026-02-02T02:02:02", navn = "Gerd Grostad", master = PDL)
            ).fornavn
        ).isEqualTo("Erik")
    }

    private fun testPersondetaljer(vararg testnavn: TestNavn) = PersonDetaljer.fra(emptyList(), testperson(*testnavn))
    private fun testperson(vararg navn: TestNavn) = Person(navn.map { navn(it) }, emptyList(), emptyList(), emptyList())
    private data class TestNavn(
        val historisk: Boolean,
        val endret: String,
        val navn: String,
        val master: String = FREG
    )

    private fun navn(testNavn: TestNavn) = Navn(
        fornavn = testNavn.navn.split(" ").first(),
        etternavn = testNavn.navn.split(" ").last(),
        metadata = Metadata(master = testNavn.master)
    )

    companion object {
        const val AKTIVT = false
        const val HISTORISK = true
        const val PDL = "PDL"
        const val FREG = "FREG"
    }
}
