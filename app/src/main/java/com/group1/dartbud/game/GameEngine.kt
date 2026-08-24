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
    val hasScored: Boolean = false, // Har spilleren åpnet scoringen (double-in)?
    // Poeng scoret i legs som ER FERDIGSPILT i denne kampen. Statistikk (snitt,
    // høyeste score, piler, runder) gjelder hele kampen, ikke bare den legen som
    // pågår - det er slik dart faktisk rapporteres. Snittet regnes derfor som
    // (dette + poengene i inneværende leg) / dartsThrown, se recalculateAverage.
    // Poengene i inneværende leg utledes fortsatt av 501 - score, slik at bust
    // (som ruller score tilbake til turStart) automatisk trekker fra igjen.
    val pointsScoredPreviousLegs: Int = 0
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
    val winnerNumber: Int = 0, // 0 = ingen har vunnet DENNE LEGEN enda
    val message: GameMessage? = null,
    // Kampen kan bestå av flere legs (best av 1/3/5/7/9, satt i Game Settings).
    // legStartingPlayer holdes fra GameState.new() og endres ALDRI av endTurn() -
    // trengs for å alternere hvem som kaster først fra leg til leg, uavhengig av
    // hvem som vant forrige leg (currentPlayer sier hvem sin tur det er NÅ, ikke
    // hvem som startet legen).
    val legStartingPlayer: Int = 1,
    val player1LegsWon: Int = 0,
    val player2LegsWon: Int = 0,
    val totalLegsInMatch: Int = 1
) {
    val activePlayer: Player get() = if (currentPlayer == 1) player1 else player2

    // Vinneren av DENNE LEGEN - kan være satt selv om kampen fortsetter (flere legs).
    val winner: Player? get() = when (winnerNumber) {
        1 -> player1
        2 -> player2
        else -> null
    }

    // Antall legs én spiller må vinne for å vinne HELE kampen - flertallet av
    // totalLegsInMatch (best av 3 -> 2, best av 5 -> 3, osv.)
    val legsNeededToWinMatch: Int get() = totalLegsInMatch / 2 + 1

    // 0 = kampen er ikke avgjort enda, selv om en enkelt leg nettopp ble vunnet.
    val matchWinnerNumber: Int get() = when {
        player1LegsWon >= legsNeededToWinMatch -> 1
        player2LegsWon >= legsNeededToWinMatch -> 2
        else -> 0
    }

    companion object {
        fun new(
            player1Name: String,
            player2Name: String,
            startingPlayer: Int = 1,
            player1LegsWon: Int = 0,
            player2LegsWon: Int = 0,
            totalLegsInMatch: Int = 1
        ) = GameState(
            player1 = Player(player1Name),
            player2 = Player(player2Name),
            currentPlayer = startingPlayer,
            legStartingPlayer = startingPlayer,
            player1LegsWon = player1LegsWon,
            player2LegsWon = player2LegsWon,
            totalLegsInMatch = totalLegsInMatch
        )
    }
}

// Tegnene regneuttrykket i Round Total-modus bruker. Ligger her, ikke i UI-laget,
// slik at knappene og parseren garantert er enige om hva de skal se etter.
const val MULTIPLY_SYMBOL = "×"
const val PLUS_SYMBOL = "+"

/**
 * Regner ut rundetotalen fra et uttrykk spilleren har tastet inn i Round Total-modus,
 * f.eks. "17×3+13×3+19×3" (=147) eller bare "51+39+57" (=147).
 *
 * Grammatikken er med vilje minimal - kun tall, × og + - fordi det er alt en
 * dartrunde trenger: tre kast, hvert av dem eventuelt et felt ganget med 2 eller 3.
 * Multiplikasjon binder sterkere enn addisjon, slik at "17×3+13" blir 51+13=64 og
 * ikke (17×3+13)=... regnet fra venstre. Det faller sammen med måten man taster
 * uttrykket inn på.
 *
 * Returnerer null hvis uttrykket ikke er ferdig eller ikke gir mening (tomt, ender på
 * en operator, inneholder tegn som ikke er siffer). Kalleren avgjør selv om resultatet
 * er en lovlig rundetotal - se roundTotalFromExpression.
 */
fun evaluateExpression(expression: String): Int? {
    if (expression.isBlank()) return null

    var sum = 0
    for (term in expression.split(PLUS_SYMBOL)) {
        var product = 1
        for (factor in term.split(MULTIPLY_SYMBOL)) {
            // toIntOrNull fanger både tomme ledd ("17+" eller "17××3") og alt annet
            // som ikke er et rent tall, uten at vi trenger en egen validering.
            val value = factor.toIntOrNull() ?: return null
            product *= value
        }
        sum += product
    }
    return sum
}

/**
 * Gjør uttrykket lesbart i score-displayet: hvert kast som er et felt ganget opp får
 * parentes rundt seg, og leddene skilles med luft. "19×3+17×3+13×3" vises altså som
 * "(19×3) + (17×3) + (13×3)", slik at man ser de tre kastene som tre kast.
 *
 * Kun formatering - selve uttrykket som lagres og regnes ut er uendret, se
 * [evaluateExpression]. Sluttparentesen settes først når leddet er ferdig, slik at et
 * halvskrevet kast vises som "(19×" mens man taster.
 *
 * Ledd uten multiplikasjon får ingen parentes: har spilleren regnet selv og tastet
 * "60+45", er det allerede så tydelig som det blir.
 */
fun formatExpressionForDisplay(expression: String): String =
    expression.split(PLUS_SYMBOL).joinToString(" $PLUS_SYMBOL ") { term ->
        when {
            !term.contains(MULTIPLY_SYMBOL) -> term
            term.endsWith(MULTIPLY_SYMBOL) -> "($term"
            else -> "($term)"
        }
    }

/**
 * Som [evaluateExpression], men returnerer kun verdier som faktisk er en lovlig
 * rundetotal (0-180, der 180 er tre trippel-20). Uttrykk som regner seg fram til
 * noe utenfor det - f.eks. "20×20" - gir null, slik at UI-et kan vise at det ikke
 * lar seg registrere.
 */
fun roundTotalFromExpression(expression: String): Int? =
    evaluateExpression(expression)?.takeIf { it in 0..180 }

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
     * å måle en dartspillers nivå på.
     *
     * Regnes over HELE kampen, ikke bare den legen som pågår: poeng fra ferdigspilte
     * legs ligger i pointsScoredPreviousLegs, og poengene i inneværende leg er
     * 501 - score. Begge deler deles på dartsThrown, som også teller på tvers av legs.
     */
    private fun recalculateAverage(player: Player): Double {
        if (player.dartsThrown == 0) return 0.0
        val totalPoints = player.pointsScoredPreviousLegs + (501 - player.score)
        return (totalPoints.toDouble() / player.dartsThrown) * 3
    }

    private fun withPlayer(state: GameState, player: Player): GameState =
        if (state.currentPlayer == 1) state.copy(player1 = player) else state.copy(player2 = player)

    // Setter winnerNumber for DENNE LEGEN og teller den samtidig mot kampens legs-
    // stilling, atomisk - unngår en egen "husk å registrere leg-resultatet"-runde
    // som lett kunne blitt kalt to ganger (eller glemt) ved rekomponering i UI-laget.
    private fun recordLegWin(state: GameState, winner: Int): GameState = state.copy(
        winnerNumber = winner,
        player1LegsWon = state.player1LegsWon + if (winner == 1) 1 else 0,
        player2LegsWon = state.player2LegsWon + if (winner == 2) 1 else 0
    )

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
            // Legen er vunnet - kan skje på hvilket som helst av de tre kastene
            newScore == 0 -> recordLegWin(next, next.currentPlayer)
            next.currentThrow == 3 -> endTurn(next)
            else -> next.copy(currentThrow = next.currentThrow + 1)
        }
    }

    /**
     * Registrerer en hel runde på én gang (Calculator Mode av): spilleren har allerede
     * regnet ut summen selv, i stedet for å taste inn ett og ett kast.
     *
     * Til forskjell fra applyThrow blir double-in/double-out IKKE verifisert her.
     * Spilleren vet selv om reglene ble oppfylt - traff de ikke doblingen som trengtes
     * (for å åpne med double-in, eller for å avslutte med double-out), taster de inn 0
     * (eller trykker "No Score") i stedet for et tall som later som noe annet talte.
     * Motoren kan derfor bare fange de to tilfellene ingen spillerdømmekraft kan endre
     * på: å gå under 0, og å lande på nøyaktig 1 med double-out.
     *
     * dartsUsedForCheckout er kun relevant når summen treffer nøyaktig 0. UI-laget må
     * spørre spilleren "hvor mange piler brukte du?" FØR dette kalles i det tilfellet,
     * siden det trengs for et riktig snitt - ellers antas 3 piler, som stemmer for
     * alle runder som ikke avslutter kampen.
     */
    fun applyRoundTotal(state: GameState, total: Int, dartsUsedForCheckout: Int = 3): GameState {
        if (state.winnerNumber != 0) return state
        if (total !in 0..180) return state

        val active = state.activePlayer

        // Snapshot av alt FØR runden brukes, slik at undo kan spole nøyaktig tilbake.
        val snapshot = state.copy(history = emptyList(), message = null)
        var next = state.copy(history = state.history + snapshot, message = null)

        val newScore = active.score - total

        val bust = checkBust(newScore, lastDartWasDouble = true) // se kommentar under
        // lastDartWasDouble=true her betyr at "må avslutte på dobbel"-sjekken i
        // checkBust aldri slår ut - den sjekken forutsetter at motoren VET hvilken
        // pil som var siste, noe den ikke gjør i denne modusen. De to sjekkene som
        // gjenstår (under 0, og nøyaktig 1) er fortsatt fullt gyldige uansett piler.
        if (bust != null) {
            val dartsThrown = active.dartsThrown + 3
            val updated = active.copy(
                dartsThrown = dartsThrown,
                average = recalculateAverage(active.copy(dartsThrown = dartsThrown))
            )
            next = withPlayer(next, updated).copy(message = GameMessage("BUST!", bust))
            return endTurn(next)
        }

        // I motsetning til applyThrow er hver runde her alltid en FULLFØRT tur - det
        // finnes ingen "midt i runden" å vente på flere kast fra, siden spilleren
        // taster inn hele rundens sum på én gang. Statistikken oppdateres derfor
        // alltid, ikke bare når kampen faktisk vinnes.
        val isCheckout = newScore == 0
        val dartsUsed = if (isCheckout) dartsUsedForCheckout.coerceIn(1, 3) else 3

        var updated = active.copy(
            score = newScore,
            dartsThrown = active.dartsThrown + dartsUsed,
            hasScored = active.hasScored || total > 0,
            lastThrow = total,
            highestScore = maxOf(active.highestScore, total),
            roundsPlayed = active.roundsPlayed + 1
        )
        updated = updated.copy(average = recalculateAverage(updated))
        next = withPlayer(next, updated)

        next = if (next.currentPlayer == 1) {
            next.copy(player1RoundHistory = next.player1RoundHistory + total)
        } else {
            next.copy(player2RoundHistory = next.player2RoundHistory + total)
        }

        return if (isCheckout) recordLegWin(next, next.currentPlayer) else endTurn(next)
    }

    /**
     * Angrer siste kast ved å hente frem snapshotet som ble tatt rett før det.
     * Alt rulles tilbake i ett, inkludert en eventuell seier.
     */
    fun undo(state: GameState): GameState {
        val previous = state.history.lastOrNull() ?: return state
        return previous.copy(history = state.history.dropLast(1))
    }

    /**
     * Starter neste leg i en kamp med flere legs (best av 3/5/7/9): friske 501-
     * poengsummer for begge, men legs-stillingen og formatet på kampen (totalLegsInMatch)
     * tas med videre. Hvem som kaster først alternerer strengt fra leg til leg etter
     * den offisielle regelen (uavhengig av hvem som vant forrige leg) - se
     * legStartingPlayer på GameState.
     */
    fun startNextLeg(state: GameState): GameState {
        val nextStartingPlayer = if (state.legStartingPlayer == 1) 2 else 1
        return GameState(
            player1 = carryStatsIntoNextLeg(state.player1),
            player2 = carryStatsIntoNextLeg(state.player2),
            currentPlayer = nextStartingPlayer,
            legStartingPlayer = nextStartingPlayer,
            player1LegsWon = state.player1LegsWon,
            player2LegsWon = state.player2LegsWon,
            totalLegsInMatch = state.totalLegsInMatch
        )
    }

    /**
     * Nullstiller det som hører til én enkelt leg (poengsum, double-in-status), men tar
     * med all statistikk videre: snitt, høyeste score, piler og runder gjelder hele
     * kampen. Poengene spilleren rakk å score i legen som nettopp ble ferdig
     * (501 - sluttscore) legges til pointsScoredPreviousLegs, slik at snittet fortsatt
     * kan regnes ut riktig når score nå settes tilbake til 501.
     */
    private fun carryStatsIntoNextLeg(player: Player) = player.copy(
        score = 501,
        lastThrow = 0,
        hasScored = false,
        pointsScoredPreviousLegs = player.pointsScoredPreviousLegs + (501 - player.score)
    )

    /**
     * Ny kamp med samme spillere. Motsatt spiller starter, som i en vanlig revansje.
     * Legs-stillingen nullstilles (ny kamp), men formatet (best av X) beholdes.
     */
    fun rematch(state: GameState, startingPlayer: Int): GameState =
        GameState.new(
            player1Name = state.player1.name,
            player2Name = state.player2.name,
            startingPlayer = startingPlayer,
            totalLegsInMatch = state.totalLegsInMatch
        )
}
