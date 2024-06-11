package no.nav.sokos.pdl.proxy.domain

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.pdl.hentperson.Metadata
import no.nav.pdl.hentperson.Navn
import no.nav.pdl.hentperson.Person
import org.junit.jupiter.api.Assertions.assertDoesNotThrow

private const val AKTIVT = false
private const val HISTORISK = true
private const val PDL = "PDL"
private const val FREG = "FREG"

internal class PersonDetaljerTest : FunSpec({

    test("Hvis liste av identer er tom så skal det ikke kaste exception") {
        shouldNotThrow<Exception> {
            PersonDetaljer.fra(emptyList(), testPerson())
        }
    }

    test("Teste om navn overførs til Persondetaljer objekt") {
        testPersondetaljer(
            TestNavn(AKTIVT, endret = "2020-02-02T02:02:02", navn = "Aron Åberg"),
        ).fornavn shouldBe "Aron"
    }

    test("Sammenligning av navn med tomme lister av endringer feiler men ett navn går bra") {
        assertDoesNotThrow {
            PersonDetaljer.fra(
                emptyList(),
                Person(
                    listOf(
                        Navn(
                            fornavn = "Ingen",
                            etternavn = "Endringer",
                            metadata = Metadata(PDL),
                        ),
                    ),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                ),
            )
        }
    }

    test("Skal velge senest endret aktiv navn når det nyeste er historisk og overstyre PDL") {
        testPersondetaljer(
            TestNavn(AKTIVT, endret = "2020-02-02T02:02:02", navn = "Aron Åberg", master = PDL),
            TestNavn(HISTORISK, endret = "2021-02-02T02:02:02", navn = "Berit Berger", master = FREG),
        ).fornavn shouldBe "Aron"
    }

    test("Skal velge nyeste aktive navn fra PDL") {
        testPersondetaljer(
            TestNavn(HISTORISK, endret = "2020-02-02T02:02:02", navn = "Aron Åberg", master = FREG),
            TestNavn(HISTORISK, endret = "2021-02-02T02:02:02", navn = "Berit Berger", master = FREG),
            TestNavn(HISTORISK, endret = "2022-02-02T02:02:02", navn = "Charlie Carlsson", master = FREG),
            TestNavn(AKTIVT, endret = "2023-02-02T02:02:02", navn = "David Danestad", master = FREG),
            TestNavn(AKTIVT, endret = "2024-02-02T02:02:02", navn = "Erik Eriksen", master = PDL),
            TestNavn(AKTIVT, endret = "2025-02-02T02:02:02", navn = "Fredrik Fransen", master = FREG),
            TestNavn(HISTORISK, endret = "2026-02-02T02:02:02", navn = "Gerd Grostad", master = PDL),
        ).fornavn shouldBe "Erik"
    }
})

private fun testPersondetaljer(vararg testnavn: TestNavn) = PersonDetaljer.fra(emptyList(), testPerson(*testnavn))

private fun testPerson(vararg navn: TestNavn) = Person(navn.map { navn(it) }, emptyList(), emptyList(), emptyList())

private fun navn(testNavn: TestNavn) =
    Navn(
        fornavn = testNavn.navn.split(" ").first(),
        etternavn = testNavn.navn.split(" ").last(),
        metadata = Metadata(master = testNavn.master),
    )

private data class TestNavn(
    val historisk: Boolean,
    val endret: String,
    val navn: String,
    val master: String = FREG,
)
