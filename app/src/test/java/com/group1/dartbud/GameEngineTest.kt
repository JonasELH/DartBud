package com.group1.dartbud

import com.group1.dartbud.game.GameEngine
import com.group1.dartbud.game.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tester for 501-reglene. Hver test svarer til en konkret feil som ble funnet i
 * gjennomgangen, eller til en regel som ikke må ryke når logikken endres.
 */
class GameEngineTest {

    private val standard = GameEngine(doubleInEnabled = false, doubleOutEnabled = true)
    private val noDoubleOut = GameEngine(doubleInEnabled = false, doubleOutEnabled = false)
    private val doubleIn = GameEngine(doubleInEnabled = true, doubleOutEnabled = true)

    private fun start() = GameState.new("A", "B")

    // Hjelper: sett spiller 1 sin score direkte, som om kampen alt er i gang
    private fun GameState.withP1Score(score: Int) =
        copy(player1 = player1.copy(score = score), turnStartScore = score)

    // ---------- vanlig scoring ----------

    @Test
    fun `tre kast trekkes fra og turen gar videre til neste spiller`() {
        var s = start()
        s = standard.applyThrow(s, 20, 3) // 60
        assertEquals(441, s.player1.score)
        assertEquals(2, s.currentThrow)
        assertEquals(1, s.currentPlayer)

        s = standard.applyThrow(s, 20, 1) // 20
        s = standard.applyThrow(s, 5, 1)  // 5

        assertEquals(416, s.player1.score)
        assertEquals(2, s.currentPlayer) // turen er over
        assertEquals(1, s.currentThrow)
        assertEquals(3, s.player1.dartsThrown)
        assertEquals(1, s.player1.roundsPlayed)
        assertEquals(85, s.player1.lastThrow)
        assertEquals(listOf(85), s.player1RoundHistory)
    }

    @Test
    fun `totalverdi kan skrives inn direkte uten multiplikator`() {
        var s = start()
        s = standard.applyThrow(s, 57, 1) // T19 skrevet rett inn
        assertEquals(444, s.player1.score)
    }

    @Test
    fun `umulig kast avvises og endrer ingenting`() {
        val s = start()
        val etter = standard.applyThrow(s, 23, 1)
        assertEquals(s, etter)
    }

    // ---------- bust ----------

    @Test
    fun `bust pa kast 1 avslutter hele turen`() {
        // Feilen som ble funnet: spilleren fikk fortsette a kaste etter bust
        var s = start().withP1Score(20)
        s = standard.applyThrow(s, 20, 3) // 60 -> under 0

        assertEquals("scoren skal tilbakestilles", 20, s.player1.score)
        assertEquals("turen skal vaere over", 2, s.currentPlayer)
        assertEquals(1, s.currentThrow)
        assertEquals("BUST!", s.message?.title)
    }

    @Test
    fun `bust pa kast 2 gir tilbake begge kastene og avslutter turen`() {
        var s = start().withP1Score(30)
        s = standard.applyThrow(s, 10, 1) // 30 -> 20
        assertEquals(20, s.player1.score)

        s = standard.applyThrow(s, 20, 3) // 60 -> under 0, bust
        assertEquals("hele turen annulleres", 30, s.player1.score)
        assertEquals(2, s.currentPlayer)
    }

    @Test
    fun `score under 0 fanges pa kast 1 og ikke forst pa kast 3`() {
        // Tidligere ble bust kun sjekket nar scoren traff akkurat 0 pa kast 1 og 2,
        // sa spilleren gikk rundt med negativ score resten av runden
        var s = start().withP1Score(10)
        s = standard.applyThrow(s, 20, 3)
        assertTrue("scoren skal aldri bli negativ", s.player1.score >= 0)
    }

    @Test
    fun `kan ikke avslutte pa 1 med double out`() {
        var s = start().withP1Score(21)
        s = standard.applyThrow(s, 20, 1) // ville gitt 1
        assertEquals(21, s.player1.score)
        assertEquals("BUST! Cannot finish on 1", s.message?.text)
    }

    @Test
    fun `null uten double er bust med double out`() {
        var s = start().withP1Score(10)
        s = standard.applyThrow(s, 10, 1) // treffer 0, men ikke pa double
        assertEquals(10, s.player1.score)
        assertEquals(0, s.winnerNumber)
        assertEquals("BUST! Must finish on a double", s.message?.text)
    }

    @Test
    fun `spiller kan ikke vinne pa piler som gikk tapt i en bust`() {
        // Kjernen i feilen: bust pa kast 1, deretter D5 pa kast 2 ga seier
        var s = start().withP1Score(10)
        s = standard.applyThrow(s, 10, 1) // bust - turen skal vaere over
        assertEquals(2, s.currentPlayer)

        // Neste kast tilhorer spiller 2, ikke spiller 1
        s = standard.applyThrow(s, 5, 2)
        assertEquals(0, s.winnerNumber)
        assertEquals(491, s.player2.score)
    }

    // ---------- seier ----------

    @Test
    fun `seier pa double`() {
        var s = start().withP1Score(40)
        s = standard.applyThrow(s, 20, 2) // D20
        assertEquals(0, s.player1.score)
        assertEquals(1, s.winnerNumber)
        assertEquals("A", s.winner?.name)
    }

    @Test
    fun `indre bull teller som double ved avslutning`() {
        var s = start().withP1Score(50)
        s = standard.applyThrow(s, 50, 1)
        assertEquals(1, s.winnerNumber)
    }

    @Test
    fun `uten double out kan man avslutte pa hva som helst`() {
        var s = start().withP1Score(10)
        s = noDoubleOut.applyThrow(s, 10, 1)
        assertEquals(1, s.winnerNumber)
    }

    @Test
    fun `seier midt i runden teller runden og rundetotalen`() {
        var s = start().withP1Score(60)
        s = standard.applyThrow(s, 20, 1) // 40 igjen
        s = standard.applyThrow(s, 20, 2) // D20 -> 0
        assertEquals(1, s.winnerNumber)
        assertEquals("rundetotal skal vaere begge kastene", 60, s.player1.lastThrow)
        assertEquals(listOf(60), s.player1RoundHistory)
        assertEquals(1, s.player1.roundsPlayed)
    }

    @Test
    fun `kast etter seier ignoreres`() {
        var s = start().withP1Score(40)
        s = standard.applyThrow(s, 20, 2)
        val etter = standard.applyThrow(s, 20, 1)
        assertEquals(s, etter)
    }

    // ---------- double in ----------

    @Test
    fun `poeng teller ikke for en double er truffet`() {
        var s = start()
        s = doubleIn.applyThrow(s, 20, 1) // enkel 20, teller ikke
        assertEquals(501, s.player1.score)
        assertEquals("pilen telles likevel", 1, s.player1.dartsThrown)
        assertEquals("NO SCORE", s.message?.title)
        assertEquals("turen fortsetter", 2, s.currentThrow)
    }

    @Test
    fun `double apner scoringen`() {
        var s = start()
        s = doubleIn.applyThrow(s, 20, 2) // D20 = 40
        assertEquals(461, s.player1.score)
        assertTrue(s.player1.hasScored)
    }

    @Test
    fun `indre bull apner scoringen`() {
        var s = start()
        s = doubleIn.applyThrow(s, 50, 1)
        assertTrue(s.player1.hasScored)
        assertEquals(451, s.player1.score)
    }

    @Test
    fun `bom gir ingen melding ved double in`() {
        var s = start()
        s = doubleIn.applyThrow(s, 0, 1)
        assertNull(s.message)
        assertEquals(1, s.player1.dartsThrown)
    }

    // ---------- undo ----------

    @Test
    fun `undo tilbakestiller score og pileteller`() {
        var s = start()
        s = standard.applyThrow(s, 20, 3)
        s = standard.undo(s)
        assertEquals(501, s.player1.score)
        assertEquals(0, s.player1.dartsThrown)
        assertEquals(1, s.currentThrow)
    }

    @Test
    fun `undo etter bust gir riktig score og ikke for hoy`() {
        // Feilen: scoren var alt gitt tilbake ved bust, og undo la poengene til igjen
        var s = start().withP1Score(30)
        s = standard.applyThrow(s, 10, 1) // 20 igjen
        s = standard.applyThrow(s, 20, 3) // bust -> tilbake til 30

        s = standard.undo(s)
        assertEquals("skal tilbake til midt i turen", 20, s.player1.score)
        assertEquals(1, s.currentPlayer)
        assertEquals(2, s.currentThrow)
    }

    @Test
    fun `undo krysser spillerbytte`() {
        var s = start()
        s = standard.applyThrow(s, 20, 1)
        s = standard.applyThrow(s, 20, 1)
        s = standard.applyThrow(s, 20, 1) // turen over, spiller 2 sin tur
        assertEquals(2, s.currentPlayer)

        s = standard.undo(s)
        assertEquals("skal tilbake til spiller 1", 1, s.currentPlayer)
        assertEquals(3, s.currentThrow)
        assertEquals(461, s.player1.score)
        assertEquals("roundsPlayed skal rulles tilbake", 0, s.player1.roundsPlayed)
        assertEquals(emptyList<Int>(), s.player1RoundHistory)
    }

    @Test
    fun `undo av vinnende kast fjerner seieren`() {
        var s = start().withP1Score(40)
        s = standard.applyThrow(s, 20, 2)
        assertEquals(1, s.winnerNumber)

        s = standard.undo(s)
        assertEquals(0, s.winnerNumber)
        assertEquals(40, s.player1.score)
    }

    @Test
    fun `undo gjenoppretter hasScored ved double in`() {
        var s = start()
        s = doubleIn.applyThrow(s, 20, 2) // apner scoringen
        assertTrue(s.player1.hasScored)

        s = doubleIn.undo(s)
        assertTrue("hasScored skal rulles tilbake", !s.player1.hasScored)
    }

    @Test
    fun `undo helt tilbake til start`() {
        var s = start()
        repeat(7) { s = standard.applyThrow(s, 20, 1) }
        repeat(7) { s = standard.undo(s) }
        assertEquals(start(), s.copy(history = emptyList()))
    }

    @Test
    fun `undo uten historikk gjor ingenting`() {
        val s = start()
        assertEquals(s, standard.undo(s))
    }

    // ---------- statistikk ----------

    @Test
    fun `snitt regnes per tre piler`() {
        var s = start()
        s = standard.applyThrow(s, 20, 3)
        s = standard.applyThrow(s, 20, 3)
        s = standard.applyThrow(s, 20, 3) // 180 pa 3 piler
        assertEquals(180.0, s.player1.average, 0.001)
    }

    @Test
    fun `hoyeste score foljer rundetotalen`() {
        var s = start()
        s = standard.applyThrow(s, 20, 3)
        s = standard.applyThrow(s, 20, 3)
        s = standard.applyThrow(s, 20, 3)
        assertEquals(180, s.player1.highestScore)
    }

    @Test
    fun `bustet runde teller piler men ikke poeng`() {
        var s = start().withP1Score(20)
        s = standard.applyThrow(s, 20, 3)
        assertEquals(20, s.player1.score)
        assertEquals(1, s.player1.dartsThrown)
    }

    // ---------- revansje ----------

    @Test
    fun `rematch nullstiller alt og bytter startspiller`() {
        var s = start().withP1Score(40)
        s = standard.applyThrow(s, 20, 2)
        assertEquals(1, s.winnerNumber)

        val ny = standard.rematch(s, startingPlayer = 2)
        assertEquals(501, ny.player1.score)
        assertEquals(501, ny.player2.score)
        assertEquals(0, ny.winnerNumber)
        assertEquals(2, ny.currentPlayer)
        assertEquals(emptyList<Int>(), ny.player1RoundHistory)
        assertTrue(ny.history.isEmpty())
        assertEquals("A", ny.player1.name)
    }

    // ---------- full kamp ----------

    @Test
    fun `en hel kamp gar opp i null uten negative scorer`() {
        var s = start()
        var vakt = 0
        while (s.winnerNumber == 0 && vakt < 400) {
            val igjen = s.activePlayer.score
            // Enkel bot som faktisk klarer a ga ut: gjor scoren partall forst, spis
            // deretter unna med T20, og avslutt pa en double nar den er innen rekkevidde
            val (verdi, multiplikator) = when {
                igjen <= 40 && igjen % 2 == 0 -> Pair(igjen / 2, 2)
                igjen % 2 == 1 -> Pair(1, 1)
                igjen - 60 >= 2 -> Pair(20, 3)
                igjen - 20 >= 2 -> Pair(20, 1)
                else -> Pair(2, 1)
            }
            s = standard.applyThrow(s, verdi, multiplikator)
            assertTrue("score ble negativ", s.player1.score >= 0 && s.player2.score >= 0)
            assertNull("boten skal aldri buste", s.message)
            vakt++
        }
        assertTrue("kampen ble aldri vunnet", s.winnerNumber != 0)
        assertEquals(0, s.winner?.score)
        assertNotNull(s.winner)
    }

    // ---------- Calculator Mode av: rundetotal i stedet for enkeltkast ----------

    @Test
    fun `rundetotal trekkes fra og turen gar videre`() {
        var s = start()
        s = standard.applyRoundTotal(s, 100)
        assertEquals(401, s.player1.score)
        assertEquals(2, s.currentPlayer)
        assertEquals(3, s.player1.dartsThrown)
        assertEquals(1, s.player1.roundsPlayed)
        assertEquals(100, s.player1.lastThrow)
        assertEquals(listOf(100), s.player1RoundHistory)
    }

    @Test
    fun `no score er det samme som a taste inn 0`() {
        var s = start()
        s = standard.applyRoundTotal(s, 0)
        assertEquals(501, s.player1.score)
        assertEquals(2, s.currentPlayer)
        assertEquals(3, s.player1.dartsThrown)
        assertEquals(listOf(0), s.player1RoundHistory)
    }

    @Test
    fun `rundetotal under 0 er bust og ruller tilbake`() {
        var s = start().withP1Score(50)
        s = standard.applyRoundTotal(s, 60)
        assertEquals("scoren skal tilbakestilles", 50, s.player1.score)
        assertEquals(2, s.currentPlayer)
        assertEquals("BUST!", s.message?.title)
        assertEquals(3, s.player1.dartsThrown)
    }

    @Test
    fun `rundetotal som lander pa 1 er bust med double out`() {
        var s = start().withP1Score(61)
        s = standard.applyRoundTotal(s, 60)
        assertEquals(61, s.player1.score)
        assertEquals("BUST! Cannot finish on 1", s.message?.text)
    }

    @Test
    fun `rundetotal som lander pa 1 er ikke bust uten double out`() {
        var s = start().withP1Score(61)
        s = noDoubleOut.applyRoundTotal(s, 60)
        assertEquals(1, s.player1.score)
        assertNull(s.message)
    }

    @Test
    fun `motoren spor ikke om siste kast var dobbel - spilleren avgjor selv`() {
        // Kjernen i forenklingen: traff spilleren ikke faktisk en dobbel, skal de
        // heller ha tastet 0 (eller trykket No Score). Taster de en sum som treffer
        // nøyaktig 0, stoler motoren pa at det var et gyldig avslutningskast.
        var s = start().withP1Score(40)
        s = standard.applyRoundTotal(s, 40)
        assertEquals(1, s.winnerNumber)
        assertNull(s.message)
    }

    @Test
    fun `checkout bruker antall piler som er oppgitt for riktig snitt`() {
        var s = start().withP1Score(100)
        s = standard.applyRoundTotal(s, 100, dartsUsedForCheckout = 2)
        assertEquals(1, s.winnerNumber)
        assertEquals(2, s.player1.dartsThrown)
    }

    @Test
    fun `checkout uten oppgitt antall piler antar 3`() {
        var s = start().withP1Score(100)
        s = standard.applyRoundTotal(s, 100)
        assertEquals(3, s.player1.dartsThrown)
    }

    @Test
    fun `rundetotal utenfor 0 til 180 avvises`() {
        val s = start()
        assertEquals(s, standard.applyRoundTotal(s, 181))
        assertEquals(s, standard.applyRoundTotal(s, -1))
    }

    @Test
    fun `kast etter seier med rundetotal ignoreres`() {
        var s = start().withP1Score(40)
        s = standard.applyRoundTotal(s, 40)
        val etter = standard.applyRoundTotal(s, 50)
        assertEquals(s, etter)
    }

    @Test
    fun `undo fungerer for rundetotal som for enkeltkast`() {
        var s = start()
        s = standard.applyRoundTotal(s, 100)
        s = standard.undo(s)
        assertEquals(start(), s.copy(history = emptyList()))
    }

    @Test
    fun `undo av vinnende rundetotal fjerner seieren`() {
        var s = start().withP1Score(40)
        s = standard.applyRoundTotal(s, 40)
        assertEquals(1, s.winnerNumber)
        s = standard.undo(s)
        assertEquals(0, s.winnerNumber)
        assertEquals(40, s.player1.score)
    }

    @Test
    fun `hoyeste score folger rundetotalen`() {
        var s = start()
        s = standard.applyRoundTotal(s, 140)
        assertEquals(140, s.player1.highestScore)
    }

    @Test
    fun `snitt regnes riktig over flere rundetotaler`() {
        var s = start()
        s = standard.applyRoundTotal(s, 100)
        s = standard.applyRoundTotal(s, 60) // spiller 2 sin tur
        s = standard.applyRoundTotal(s, 80) // spiller 1 igjen
        // spiller 1: 100 + 80 = 180 poeng pa 6 piler -> snitt 90.0
        assertEquals(90.0, s.player1.average, 0.001)
    }
}
