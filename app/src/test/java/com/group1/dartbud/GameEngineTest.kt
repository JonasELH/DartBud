package com.group1.dartbud

import com.group1.dartbud.game.GameEngine
import com.group1.dartbud.game.GameState
import com.group1.dartbud.game.evaluateExpression
import com.group1.dartbud.game.formatExpressionForDisplay
import com.group1.dartbud.game.roundTotalFromExpression
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

    // ---------- legs / kampformat (best av 1/3/5/7/9) ----------

    @Test
    fun `enkelt-leg kamp - vinner av legen vinner ogsa kampen med en gang`() {
        var s = start().withP1Score(40) // totalLegsInMatch = 1 (standard)
        s = standard.applyRoundTotal(s, 40)
        assertEquals(1, s.winnerNumber)
        assertEquals(1, s.player1LegsWon)
        assertEquals(1, s.matchWinnerNumber)
    }

    @Test
    fun `legsNeededToWinMatch er flertallet av formatet`() {
        assertEquals(1, GameState.new("A", "B", totalLegsInMatch = 1).legsNeededToWinMatch)
        assertEquals(2, GameState.new("A", "B", totalLegsInMatch = 3).legsNeededToWinMatch)
        assertEquals(3, GameState.new("A", "B", totalLegsInMatch = 5).legsNeededToWinMatch)
        assertEquals(4, GameState.new("A", "B", totalLegsInMatch = 7).legsNeededToWinMatch)
        assertEquals(5, GameState.new("A", "B", totalLegsInMatch = 9).legsNeededToWinMatch)
    }

    @Test
    fun `kampen er ikke avgjort etter forste leg i et flerleg-format`() {
        var s = GameState.new("A", "B", totalLegsInMatch = 3).withP1Score(40)
        s = standard.applyRoundTotal(s, 40)
        assertEquals(1, s.winnerNumber) // legen er vunnet
        assertEquals(1, s.player1LegsWon)
        assertEquals(0, s.matchWinnerNumber) // men ikke kampen - trenger 2 av 3
    }

    @Test
    fun `startNextLeg gir friske poengsummer men beholder legs-stilling og format`() {
        var s = GameState.new("A", "B", totalLegsInMatch = 3).withP1Score(40)
        s = standard.applyRoundTotal(s, 40)
        s = standard.startNextLeg(s)

        assertEquals(501, s.player1.score)
        assertEquals(501, s.player2.score)
        assertEquals(0, s.winnerNumber)
        assertEquals(1, s.player1LegsWon) // tatt med videre
        assertEquals(0, s.player2LegsWon)
        assertEquals(3, s.totalLegsInMatch)
    }

    @Test
    fun `startNextLeg alternerer strengt uavhengig av hvem som vant forrige leg`() {
        // Spiller 1 starter og vinner leg 1
        var s = GameState.new("A", "B", startingPlayer = 1, totalLegsInMatch = 3).withP1Score(40)
        s = standard.applyRoundTotal(s, 40)
        s = standard.startNextLeg(s)
        assertEquals(2, s.currentPlayer) // spiller 2 skal starte leg 2

        // Spiller 2 vinner OGSÅ leg 2 (som spillets starter denne gangen)
        s = s.copy(player2 = s.player2.copy(score = 40), turnStartScore = 40)
        s = standard.applyRoundTotal(s, 40)
        s = standard.startNextLeg(s)
        // Strengt alternerende betyr spiller 1 starter leg 3 - IKKE "taperen (spiller 1)
        // fra leg 2 starter", som tilfeldigvis ville gitt samme svar her. Testen under
        // med annen rekkefølge viser at det faktisk er alterneringen som styrer.
        assertEquals(1, s.currentPlayer)
    }

    @Test
    fun `matchWinnerNumber settes forst nar flertallet av legs er vunnet`() {
        var s = GameState.new("A", "B", totalLegsInMatch = 3).withP1Score(40)
        s = standard.applyRoundTotal(s, 40) // leg 1 til spiller 1
        assertEquals(0, s.matchWinnerNumber)
        s = standard.startNextLeg(s)

        s = s.copy(player2 = s.player2.copy(score = 40), turnStartScore = 40)
        s = standard.applyRoundTotal(s, 40) // leg 2 til spiller 2
        assertEquals(0, s.matchWinnerNumber)
        s = standard.startNextLeg(s)

        s = s.withP1Score(40)
        s = standard.applyRoundTotal(s, 40) // leg 3 til spiller 1 - 2 av 3, kampen avgjort
        assertEquals(2, s.player1LegsWon)
        assertEquals(1, s.matchWinnerNumber)
    }

    // ---------- statistikk gjelder hele kampen, ikke bare den legen som pagar ----------

    @Test
    fun `snittet regnes over alle legs i kampen`() {
        // Leg 1: spiller 1 tar 501 pa 15 piler (100 + 100 + 100 + 100 + 101)
        var s = GameState.new("A", "B", totalLegsInMatch = 3)
        repeat(4) {
            s = standard.applyRoundTotal(s, 100) // spiller 1
            s = standard.applyRoundTotal(s, 0)   // spiller 2
        }
        s = standard.applyRoundTotal(s, 101) // spiller 1 sjekker ut
        assertEquals(1, s.player1LegsWon)
        assertEquals(100.2, s.player1.average, 0.01) // 501 poeng / 15 piler * 3

        // Leg 2: spiller 2 apner (turen alternerer mellom legs), sa spiller 1 tar 60.
        // Snittet til spiller 1 skal na dekke BEGGE legs:
        // (501 + 60) poeng / 18 piler * 3 = 93.5
        s = standard.startNextLeg(s)
        assertEquals(2, s.currentPlayer)
        s = standard.applyRoundTotal(s, 0)  // spiller 2
        s = standard.applyRoundTotal(s, 60) // spiller 1
        assertEquals(93.5, s.player1.average, 0.01)
    }

    @Test
    fun `piler og runder telles videre inn i neste leg`() {
        var s = GameState.new("A", "B", totalLegsInMatch = 3).withP1Score(40)
        s = standard.applyRoundTotal(s, 40)
        val pilerEtterLeg1 = s.player1.dartsThrown
        val runderEtterLeg1 = s.player1.roundsPlayed

        s = standard.startNextLeg(s)
        assertEquals(pilerEtterLeg1, s.player1.dartsThrown)
        assertEquals(runderEtterLeg1, s.player1.roundsPlayed)

        s = standard.applyRoundTotal(s, 0)  // spiller 2 apner leg 2 (turen alternerer)
        s = standard.applyRoundTotal(s, 60) // spiller 1
        assertEquals(pilerEtterLeg1 + 3, s.player1.dartsThrown)
        assertEquals(runderEtterLeg1 + 1, s.player1.roundsPlayed)
    }

    @Test
    fun `hoyeste score fra et tidligere leg star seg ut kampen`() {
        var s = GameState.new("A", "B", totalLegsInMatch = 3)
        s = standard.applyRoundTotal(s, 180) // spiller 1 apner med maksrunde
        assertEquals(180, s.player1.highestScore)

        s = s.copy(player1 = s.player1.copy(score = 40), turnStartScore = 40)
        s = standard.applyRoundTotal(s, 40) // vinner legen
        s = standard.startNextLeg(s)
        s = standard.applyRoundTotal(s, 0)  // spiller 2 apner leg 2 (turen alternerer)
        s = standard.applyRoundTotal(s, 60) // en helt vanlig runde for spiller 1 i leg 2

        assertEquals("180-runden fra leg 1 skal fortsatt vare hoyeste score", 180, s.player1.highestScore)
    }

    @Test
    fun `poengsummen nullstilles selv om statistikken folger med`() {
        var s = GameState.new("A", "B", totalLegsInMatch = 3).withP1Score(40)
        s = standard.applyRoundTotal(s, 40)
        s = standard.startNextLeg(s)

        assertEquals(501, s.player1.score)
        assertEquals(501, s.player2.score)
        // ... men poengene fra leg 1 er tatt vare pa til snittutregningen
        assertEquals(501, s.player1.pointsScoredPreviousLegs)
    }

    @Test
    fun `taperens uferdige leg teller ogsa med i snittet`() {
        // Spiller 2 rekker 100 poeng for spiller 1 vinner legen
        var s = GameState.new("A", "B", totalLegsInMatch = 3)
        s = standard.applyRoundTotal(s, 60)  // spiller 1
        s = standard.applyRoundTotal(s, 100) // spiller 2
        s = s.copy(player1 = s.player1.copy(score = 40), turnStartScore = 40)
        s = standard.applyRoundTotal(s, 40)  // spiller 1 vinner legen

        s = standard.startNextLeg(s)
        // Spiller 2 scoret 100 av 501 i leg 1 - de 100 poengene skal folge med videre
        assertEquals(100, s.player2.pointsScoredPreviousLegs)
        assertEquals(100.0, s.player2.average, 0.01) // 100 poeng / 3 piler * 3
    }

    // ---------- regneuttrykk i Round Total-modus ----------

    @Test
    fun `rene tall regnes ut som for`() {
        assertEquals(60, evaluateExpression("60"))
        assertEquals(0, evaluateExpression("0"))
        assertEquals(180, evaluateExpression("180"))
    }

    @Test
    fun `multiplikasjon binder sterkere enn addisjon`() {
        // 51 + 13 = 64, IKKE (17+13)*3 = 90 og ikke ((17*3)+13)*3
        assertEquals(64, evaluateExpression("17×3+13"))
    }

    @Test
    fun `tre tripler summeres riktig`() {
        assertEquals(147, evaluateExpression("17×3+13×3+19×3"))
        // Samme runde tastet inn som ferdig utregnede kast
        assertEquals(147, evaluateExpression("51+39+57"))
    }

    @Test
    fun `maksrunden lar seg taste inn begge veier`() {
        assertEquals(180, evaluateExpression("20×3+20×3+20×3"))
        assertEquals(180, evaluateExpression("60+60+60"))
    }

    @Test
    fun `uferdige uttrykk gir null`() {
        assertNull(evaluateExpression(""))
        assertNull(evaluateExpression("17×"))
        assertNull(evaluateExpression("17+"))
        assertNull(evaluateExpression("17××3"))
        assertNull(evaluateExpression("+17"))
    }

    @Test
    fun `uttrykk som gir rundetotal utenfor 0 til 180 avvises`() {
        assertNull(roundTotalFromExpression("20×20"))
        assertNull(roundTotalFromExpression("60+60+60+60"))
        assertEquals(180, roundTotalFromExpression("60+60+60"))
        assertEquals(0, roundTotalFromExpression("0"))
    }

    @Test
    fun `rundetotal fra uferdig uttrykk er ogsa null`() {
        assertNull(roundTotalFromExpression("17×"))
        assertNull(roundTotalFromExpression(""))
    }

    @Test
    fun `kast med multiplikasjon far parentes i displayet`() {
        assertEquals("(19×3) + (17×3) + (13×3)", formatExpressionForDisplay("19×3+17×3+13×3"))
    }

    @Test
    fun `ledd uten multiplikasjon far ingen parentes`() {
        assertEquals("60", formatExpressionForDisplay("60"))
        assertEquals("60 + 45", formatExpressionForDisplay("60+45"))
        // Blandet: kun leddet som faktisk er ganget opp far parentes
        assertEquals("(20×3) + 45", formatExpressionForDisplay("20×3+45"))
    }

    @Test
    fun `halvskrevet kast far apen parentes mens man taster`() {
        assertEquals("19", formatExpressionForDisplay("19"))
        assertEquals("(19×", formatExpressionForDisplay("19×"))
        assertEquals("(19×3)", formatExpressionForDisplay("19×3"))
        assertEquals("(19×3) + ", formatExpressionForDisplay("19×3+"))
    }

    @Test
    fun `formatering endrer ikke hva uttrykket regnes ut til`() {
        // Formateringen er kun visuell - parseren ser fortsatt det samme
        val expr = "19×3+17×3+13×3"
        assertEquals(147, evaluateExpression(expr))
        assertEquals("(19×3) + (17×3) + (13×3)", formatExpressionForDisplay(expr))
    }

    @Test
    fun `uttrykk kan mates rett inn i motoren`() {
        var s = start()
        val total = roundTotalFromExpression("17×3+13×3+19×3")
        assertEquals(147, total)
        s = standard.applyRoundTotal(s, total!!)
        assertEquals(354, s.player1.score) // 501 - 147
    }

    @Test
    fun `rematch nullstiller legs-stillingen men beholder formatet`() {
        var s = GameState.new("A", "B", totalLegsInMatch = 5).withP1Score(40)
        s = standard.applyRoundTotal(s, 40)
        s = standard.rematch(s, startingPlayer = 2)

        assertEquals(0, s.player1LegsWon)
        assertEquals(0, s.player2LegsWon)
        assertEquals(5, s.totalLegsInMatch)
        assertEquals(2, s.currentPlayer)
        assertEquals(501, s.player1.score)
    }
}
