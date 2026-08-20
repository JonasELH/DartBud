package com.group1.dartbud

import com.group1.dartbud.game.isPossibleDartScore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tester at inndata-valideringen godtar akkurat de summene én enkelt pil kan gi.
 *
 * Balansen her er viktig i begge retninger: valideringen skal stoppe tastefeil som 23
 * og 41 (finnes ikke på brettet), men den må ikke stå i veien for at man skriver
 * totalverdien rett inn - 57 for T19 er en helt vanlig måte å registrere kast på.
 */
class DartScoreTest {

    @Test
    fun `bom og enkeltfelt godtas`() {
        assertTrue(isPossibleDartScore(0))
        for (v in 1..20) {
            assertTrue("enkel $v skal godtas", isPossibleDartScore(v))
        }
    }

    @Test
    fun `bull godtas`() {
        assertTrue(isPossibleDartScore(25))
        assertTrue(isPossibleDartScore(50))
    }

    @Test
    fun `doble godtas som totalverdi`() {
        for (felt in 1..20) {
            assertTrue("D$felt skal godtas", isPossibleDartScore(felt * 2))
        }
    }

    @Test
    fun `tripler godtas som totalverdi`() {
        for (felt in 1..20) {
            assertTrue("T$felt skal godtas", isPossibleDartScore(felt * 3))
        }
        // T19 og T20 er de vanligste - skrives ofte rett inn
        assertTrue(isPossibleDartScore(57))
        assertTrue(isPossibleDartScore(60))
    }

    @Test
    fun `umulige summer avvises`() {
        // Ikke enkeltfelt, ikke dobbel, ikke trippel, ikke bull
        listOf(23, 29, 31, 35, 37, 41, 43, 44, 46, 47, 49, 53, 55, 58, 59).forEach { v ->
            assertFalse("$v finnes ikke pa brettet", isPossibleDartScore(v))
        }
    }

    @Test
    fun `over maks avvises`() {
        assertFalse(isPossibleDartScore(61))
        assertFalse(isPossibleDartScore(100))
    }

    @Test
    fun `44 avvises selv om det er partall`() {
        // 44 / 2 = 22, og det finnes ingen D22 - hoyeste dobbel er D20 = 40
        assertFalse(isPossibleDartScore(44))
    }
}
