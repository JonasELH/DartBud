package com.group1.dartbud

import com.group1.dartbud.game.calculateCheckout
import com.group1.dartbud.game.calculateCheckoutAlternatives
import com.group1.dartbud.game.genericCheckout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tester for checkout-forslagene.
 *
 * Bakgrunn: den håndskrevne tabellen i GameScreen var eksplisitt ned til 60 og dekket
 * deretter kun partall 2..40. Alt mellom 41 og 59, og alle oddetall under 40, falt
 * gjennom og ga tom streng - altså ingen hjelp på svært vanlige utganger. Disse
 * testene låser fast at hullet er tettet, og at forslagene faktisk går opp i score.
 */
class CheckoutTest {

    // Regner ut hva et forslag som "9 D16" faktisk summerer til, slik at vi tester
    // at forslaget stemmer - ikke bare at det finnes en streng.
    private fun sumOf(suggestion: String): Int =
        suggestion.split(" ").sumOf { part ->
            when {
                part == "Bull" -> 50 // indre bull, teller som D25
                part.startsWith("D") -> part.drop(1).toInt() * 2
                part.startsWith("T") -> part.drop(1).toInt() * 3
                else -> part.toInt()
            }
        }

    @Test
    fun `alle scorer mellom 2 og 60 far et forslag`() {
        // 1 kan ikke avsluttes med double-out og skal derfor ikke ha forslag
        for (score in 2..60) {
            val suggestion = genericCheckout(score)
            assertTrue("Mangler checkout for $score", suggestion.isNotEmpty())
        }
    }

    @Test
    fun `forslagene summerer til riktig score`() {
        for (score in 2..60) {
            val suggestion = genericCheckout(score)
            assertEquals("Feil sum for $score ($suggestion)", score, sumOf(suggestion))
        }
    }

    @Test
    fun `forslagene avslutter alltid pa en double`() {
        for (score in 2..60) {
            val lastDart = genericCheckout(score).split(" ").last()
            assertTrue("$score avslutter ikke på double: $lastDart", lastDart.startsWith("D"))
        }
    }

    @Test
    fun `partall opp til 40 gis som ren double`() {
        assertEquals("D20", genericCheckout(40))
        assertEquals("D16", genericCheckout(32))
        assertEquals("D1", genericCheckout(2))
    }

    @Test
    fun `hullet mellom 41 og 59 er tettet`() {
        assertEquals("9 D16", genericCheckout(41))
        assertEquals("11 D16", genericCheckout(43))
        assertEquals("17 D20", genericCheckout(57))
        assertEquals("19 D20", genericCheckout(59))
    }

    @Test
    fun `oddetall under 40 er tettet`() {
        assertEquals("1 D16", genericCheckout(33))
        assertEquals("7 D16", genericCheckout(39))
        assertEquals("15 D8", genericCheckout(31))
        assertEquals("1 D1", genericCheckout(3))
    }

    @Test
    fun `score som ikke kan avsluttes gir tom streng`() {
        assertEquals("", genericCheckout(1))
        assertEquals("", genericCheckout(0))
    }

    // ---------- hele tabellen, ikke bare den generiske delen ----------

    @Test
    fun `hele tabellen 2 til 170 gir forslag som summerer riktig`() {
        // 159, 162, 163, 165, 166, 168 og 169 kan ikke avsluttes i det hele tatt
        val umulige = setOf(159, 162, 163, 165, 166, 168, 169)
        for (score in 2..170) {
            val forslag = calculateCheckout(score)
            if (score in umulige) {
                assertEquals("$score skal vaere No out shot", "No out shot", forslag)
                continue
            }
            assertTrue("Mangler checkout for $score", forslag.isNotEmpty())
            assertEquals("Feil sum for $score ($forslag)", score, sumOf(forslag))
        }
    }

    @Test
    fun `hele tabellen avslutter pa double eller bull`() {
        val umulige = setOf(159, 162, 163, 165, 166, 168, 169)
        for (score in 2..170) {
            if (score in umulige) continue
            val siste = calculateCheckout(score).split(" ").last()
            assertTrue(
                "$score avslutter ikke pa double: $siste",
                siste.startsWith("D") || siste == "Bull"
            )
        }
    }

    @Test
    fun `tabellen bruker maks tre kast`() {
        for (score in 2..170) {
            val forslag = calculateCheckout(score)
            if (forslag.isEmpty() || forslag == "No out shot") continue
            assertTrue(
                "$score bruker mer enn tre kast: $forslag",
                forslag.split(" ").size <= 3
            )
        }
    }

    @Test
    fun `over 170 kan ikke avsluttes`() {
        assertEquals("No out shot", calculateCheckout(171))
        assertEquals("No out shot", calculateCheckout(501))
    }

    // ---------- calculateCheckoutAlternatives - "veksle mellom utganger"-knappen ----------

    @Test
    fun `alternativer gir kun No out shot for umulige scorer`() {
        assertEquals(listOf("No out shot"), calculateCheckoutAlternatives(169))
        assertEquals(listOf("No out shot"), calculateCheckoutAlternatives(501))
    }

    @Test
    fun `forste alternativ er alltid samme som calculateCheckout`() {
        for (score in listOf(2, 32, 41, 60, 100, 132, 170)) {
            assertEquals(calculateCheckout(score), calculateCheckoutAlternatives(score).first())
        }
    }

    @Test
    fun `alle alternativer summerer til riktig score og avslutter pa double eller bull`() {
        val umulige = setOf(159, 162, 163, 165, 166, 168, 169)
        for (score in 2..170) {
            if (score in umulige) continue
            for (forslag in calculateCheckoutAlternatives(score)) {
                assertEquals("Feil sum for $score ($forslag)", score, sumOf(forslag))
                val siste = forslag.split(" ").last()
                assertTrue(
                    "$score avslutter ikke pa double: $forslag",
                    siste.startsWith("D") || siste == "Bull"
                )
            }
        }
    }

    @Test
    fun `alternativer er aldri flere enn maxAlternativer pluss primaeren`() {
        for (score in 2..170) {
            assertTrue(calculateCheckoutAlternatives(score, maxAlternatives = 2).size <= 3)
        }
    }

    @Test
    fun `121 har 25 T20 D18 som alternativ rett etter hovedforslaget`() {
        val alternativer = calculateCheckoutAlternatives(121)
        // Hovedforslaget skal vaere urort
        assertEquals("T17 T10 D20", alternativer.first())
        // Den handplukkede ruten kommer forst nar man blar
        assertEquals("25 T20 D18", alternativer[1])
        // ... og den summerer riktig: 25 + 60 + 36
        assertEquals(121, sumOf("25 T20 D18"))
    }

    @Test
    fun `handplukkede ruter pavirker bare scoren de gjelder`() {
        // 120 og 122 ligger rundt 121 og skal ikke ha faatt noe ekstra
        assertTrue("25 T20 D18" !in calculateCheckoutAlternatives(120))
        assertTrue("25 T20 D18" !in calculateCheckoutAlternatives(122))
    }

    @Test
    fun `132 har T20 T16 D12 som et av alternativene`() {
        assertTrue(
            "Forventet T20 T16 D12 blant alternativene for 132: ${calculateCheckoutAlternatives(132)}",
            "T20 T16 D12" in calculateCheckoutAlternatives(132)
        )
    }

    @Test
    fun `32 har et reelt alternativ til ren double`() {
        val alternativer = calculateCheckoutAlternatives(32)
        assertEquals("D16", alternativer.first())
        assertTrue("Forventet flere enn ett forslag for 32: $alternativer", alternativer.size > 1)
    }
}
