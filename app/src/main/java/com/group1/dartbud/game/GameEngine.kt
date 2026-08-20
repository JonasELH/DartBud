package com.group1.dartbud.game

/**
 * Ren 501-spillogikk, uten Compose og uten database.
 *
 * Logikken lå tidligere inne i GameScreen som lokale funksjoner over en haug med
 * mutableStateOf-variabler. Det gjorde den umulig å teste automatisk, og det var
 * nettopp der feilene fikk leve: bust-regelen var ulik mellom kast 1, 2 og 3 fordi
 * hvert kast hadde sin egen nesten-like kodegren per spiller.
 *
 * Her er alt uttrykt som rene funksjoner fra én tilstand til den neste, slik at
 * hver regel kan testes direkte (se GameEngineTest).
 */

// Ett spillerobjekt i den pågående kampen. Dette er spilltilstand, ikke databasen -
// permanent lagring (GameStatsEntity) skjer først når kampen er vunnet.
data class Player(
    val name: String,
    val score: Int = 501,
    val lastThrow: Int = 0,
    val highestScore: Int = 0,
    val average: Double = 0.0,
    val roundsPlayed: Int = 0,
    val dartsThrown: Int = 0,
    val hasScored: Boolean = false // Har spilleren åpnet scoringen (double-in)?
)

// Melding som skal vises til spilleren etter et kast (bust, eller "ikke åpnet enda").
data class GameMessage(val title: String, val text: String)

/**
 * Hele tilstanden i en pågående kamp.
 *
 * [history] er undo-stacken: en kopi av tilstanden slik den var rett FØR hvert kast.
 * Å spole tilbake er derfor bare å hente frem forrige element - i motsetning til å
 * regne seg bakover, som ga feil score etter bust og mistet hasScored.
 * Elementene i stacken har selv tom history, så tilstanden vokser lineært.
 */
data class GameState(
    val player1: Player,
    val player2: Player,
    val currentPlayer: Int = 1,
    val currentThrow: Int = 1,
    val overallRound: Int = 1,
    val turnStartScore: Int = 501,
    val throw1: Int? = null,
    val throw2: Int? = null,
    val throw3: Int? = null,
    val throw1WasDouble: Boolean = false,
    val throw2WasDouble: Boolean = false,
    val throw3WasDouble: Boolean = false,
    val player1RoundHistory: List<Int> = emptyList(),
    val player2RoundHistory: List<Int> = emptyList(),
    val history: List<GameState> = emptyList(),
    val winnerNumber: Int = 0, // 0 = ingen vinner enda
    val message: GameMessage? = null
) {
    val activePlayer: Player get() = if (currentPlayer == 1) player1 else player2
    val winner: Player? get() = when (winnerNumber) {
        1 -> player1
        2 -> player2
        else -> null
    }

    companion object {
        fun new(player1Name: String, player2Name: String, startingPlayer: Int = 1) = GameState(
            player1 = Player(player1Name),
            player2 = Player(player2Name),
            currentPlayer = startingPlayer
        )
    }
}

/**
 * Kan én enkelt pil gi denne poengsummen?
 *
 * Brukes til å avvise tastefeil uten å stå i veien for måten appen er ment å brukes
 * på: man kan skrive totalverdien rett inn (57 for T19) i stedet for å gå veien om
 * multiplikator-knappene. Umulige verdier som 23, 41 og 46 finnes ikke på brettet.
 */
fun isPossibleDartScore(value: Int): Boolean {
    if (value == 0) return true
    if (value in 1..20) return true
    if (value == 25 || value == 50) return true
    if (value % 2 == 0 && value / 2 in 1..20) return true
    if (value % 3 == 0 && value / 3 in 1..20) return true
    return false
}

/**
 * Er dette en gyldig registrering?
 *  - Uten multiplikator kan totalverdien skrives rett inn, så lenge en pil kan gi den.
 *  - Med 2x/3x er det selve feltet som skrives inn, altså 1-20 (pluss bull som D25).
 */
fun isValidThrowInput(value: Int, multiplier: Int): Boolean = when (multiplier) {
    1 -> isPossibleDartScore(value)
    2 -> value in 1..20 || value == 25
    3 -> value in 1..20
    else -> false
}

class GameEngine(
    private val doubleInEnabled: Boolean = false,
    private val doubleOutEnabled: Boolean = true
) {

    /**
     * Sjekker om resultatet av et kast er en bust etter 501-reglene:
     *  1. Går scoren under 0 -> alltid bust.
     *  2. Havner du på nøyaktig 1 med double-out -> bust, siden laveste double er D1 = 2.
     *  3. Havner du på nøyaktig 0 med double-out, men siste pil var ikke en double -> bust.
     */
    fun checkBust(newScore: Int, lastDartWasDouble: Boolean): String? = when {
        newScore < 0 -> "BUST! Score under 0"
        newScore == 1 && doubleOutEnabled -> "BUST! Cannot finish on 1"
        newScore == 0 && doubleOutEnabled && !lastDartWasDouble -> "BUST! Must finish on a double"
        else -> null
    }

    /**
     * Gjennomsnittlig poengsum per tre kast ("three-dart average"), den vanlige måten
     * å måle en dartspillers nivå på. 501 - score = poeng scoret så langt.
     */
    private fun recalculateAverage(player: Player): Double {
        if (player.dartsThrown == 0) return 0.0
        return ((501 - player.score).toDouble() / player.dartsThrown) * 3
    }

    private fun withPlayer(state: GameState, player: Player): GameState =
        if (state.currentPlayer == 1) state.copy(player1 = player) else state.copy(player2 = player)

    /**
     * Avslutter den aktive spillerens tur: bytter spiller, nullstiller rundens kast og
     * setter turnStartScore for den som nå skal kaste.
     *
     * Kalles både når alle tre kastene er brukt OG ved bust - en bust koster deg resten
     * av turen (501-regel). Det siste gjorde koden tidligere ikke: etter bust på kast 1
     * eller 2 fikk spilleren fortsette, og kunne vinne på piler som egentlig var tapt.
     */
    private fun endTurn(state: GameState): GameState {
        val next = if (state.currentPlayer == 1) 2 else 1
        return state.copy(
            currentPlayer = next,
            turnStartScore = if (next == 1) state.player1.score else state.player2.score,
            overallRound = state.overallRound + 1,
            throw1 = null,
            throw2 = null,
            throw3 = null,
            throw1WasDouble = false,
            throw2WasDouble = false,
            throw3WasDouble = false,
            currentThrow = 1
        )
    }

    /**
     * Registrerer ett kast og returnerer den nye tilstanden. Ugyldige inndata og kast
     * etter at kampen er vunnet returnerer tilstanden uendret.
     */
    fun applyThrow(state: GameState, value: Int, multiplier: Int): GameState {
        if (state.winnerNumber != 0) return state
        if (!isValidThrowInput(value, multiplier)) return state

        val throwValue = value * multiplier
        // Indre bull (50) er i praksis D25, og teller derfor som double - både for
        // double-in og double-out.
        val isDouble = multiplier == 2 || throwValue == 50

        val active = state.activePlayer

        // Snapshot av alt FØR kastet brukes, slik at undo kan spole nøyaktig tilbake.
        val snapshot = state.copy(history = emptyList(), message = null)
        var next = state.copy(history = state.history + snapshot, message = null)

        // Med double-in teller ingen poeng før spilleren har åpnet med en double.
        // Kastet er fortsatt en kastet pil (teller i dartsThrown og dermed i snittet),
        // men gir null poeng. Dette er ikke bust - spilleren beholder resten av turen.
        val opensScoring = !doubleInEnabled || active.hasScored || isDouble
        val scoringValue = if (opensScoring) throwValue else 0

        next = when (next.currentThrow) {
            1 -> next.copy(throw1 = scoringValue, throw1WasDouble = isDouble)
            2 -> next.copy(throw2 = scoringValue, throw2WasDouble = isDouble)
            else -> next.copy(throw3 = scoringValue, throw3WasDouble = isDouble)
        }

        val newScore = active.score - scoringValue
        var updated = active.copy(
            score = newScore,
            dartsThrown = active.dartsThrown + 1,
            hasScored = active.hasScored || (opensScoring && throwValue > 0)
        )

        if (!opensScoring) {
            // En ren bom (0) trenger ingen forklaring, den gir uansett ingen poeng.
            val message = if (throwValue > 0) {
                GameMessage("NO SCORE", "You must hit a double to start scoring (double in).")
            } else null

            updated = updated.copy(average = recalculateAverage(updated))
            next = withPlayer(next, updated).copy(message = message)
            return if (next.currentThrow == 3) endTurn(next) else next.copy(currentThrow = next.currentThrow + 1)
        }

        val bust = checkBust(newScore, isDouble)
        if (bust != null) {
            // Hele turen annulleres: scoren settes tilbake til slik den var da turen
            // startet, og spilleren mister eventuelle gjenværende kast.
            updated = updated.copy(score = next.turnStartScore)
            updated = updated.copy(average = recalculateAverage(updated))
            next = withPlayer(next, updated).copy(message = GameMessage("BUST!", bust))
            return endTurn(next)
        }

        // Gyldig kast. Rundetotalen er alt som faktisk er scoret i denne turen så langt.
        val roundTotal = (next.throw1 ?: 0) + (next.throw2 ?: 0) + (next.throw3 ?: 0)
        val turnIsOver = newScore == 0 || next.currentThrow == 3

        if (turnIsOver) {
            updated = updated.copy(
                lastThrow = roundTotal,
                highestScore = maxOf(updated.highestScore, roundTotal),
                roundsPlayed = updated.roundsPlayed + 1
            )
        }
        updated = updated.copy(average = recalculateAverage(updated))
        next = withPlayer(next, updated)

        if (turnIsOver) {
            next = if (next.currentPlayer == 1) {
                next.copy(player1RoundHistory = next.player1RoundHistory + roundTotal)
            } else {
                next.copy(player2RoundHistory = next.player2RoundHistory + roundTotal)
            }
        }

        return when {
            // Kampen er vunnet - kan skje på hvilket som helst av de tre kastene
            newScore == 0 -> next.copy(winnerNumber = next.currentPlayer)
            next.currentThrow == 3 -> endTurn(next)
            else -> next.copy(currentThrow = next.currentThrow + 1)
        }
    }

    /**
     * Angrer siste kast ved å hente frem snapshotet som ble tatt rett før det.
     * Alt rulles tilbake i ett, inkludert en eventuell seier.
     */
    fun undo(state: GameState): GameState {
        val previous = state.history.lastOrNull() ?: return state
        return previous.copy(history = state.history.dropLast(1))
    }

    /** Ny kamp med samme spillere. Motsatt spiller starter, som i en vanlig revansje. */
    fun rematch(state: GameState, startingPlayer: Int): GameState =
        GameState.new(state.player1.name, state.player2.name, startingPlayer)
}
