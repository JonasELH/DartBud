package com.group1.dartbud.screens

import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group1.dartbud.viewmodel.GameViewModel
import com.group1.dartbud.viewmodel.PlayerViewModel
import com.group1.dartbud.data.GameStatsEntity
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalDensity

// Ett spillerobjekt i den pågående kampen. Dette er UI-state, ikke databasen -
// permanent lagring (GameStatsEntity) skjer først når kampen er vunnet.
data class Player(
    val name: String,
    var score: Int = 501,
    var lastThrow: Int = 0,
    var highestScore: Int = 0,
    var average: Double = 0.0,
    var roundsPlayed: Int = 0,
    var dartsThrown: Int = 0,
    var hasScored: Boolean = false // Har spilleren fått godkjent (double-in) treff enda?
)

// Én enkelt dart-kast, brukt til å bygge opp dartHistory slik at undo kan spole tilbake.
data class DartThrow(
    val playerId: Int,
    val value: Int,
    val wasDouble: Boolean,
    val throwNumber: Int // 1, 2 eller 3 - hvilket kast i runden dette var
)

// Hjelpefunksjon for å skalere ned skriftstørrelse uten å gå over en maks-grense
fun TextUnit.coerceAtMost(maximumValue: TextUnit): TextUnit {
    return if (this.value > maximumValue.value) maximumValue else this
}

// Selve spillskjermen for en 501-kamp. All spillogikk (bust, checkout, undo, snitt)
// ligger nested her inne som lokale funksjoner, siden de kun gir mening sammen med
// denne skjermens state (throw1/2/3, currentPlayer, osv.).
@Composable
fun GameScreen(
    navController: NavController,
    doubleInEnabled: Boolean = false, // Krever double for å "åpne" scoring (double-in)
    doubleOutEnabled: Boolean = true, // Krever double (eller bullseye) for å avslutte kampen
    player1Name: String = "PLAYER 1",
    player2Name: String = "PLAYER 2",
    gameViewModel: GameViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {

    // Skjermstørrelse basert på device-konfigurasjon, brukt til å regne ut skriftstørrelser
    // lenger ned. Merk: playerCardHeight/throwButtonHeight/osv. her blir skygget av
    // nye variabler med samme navn inne i BoxWithConstraints under (målt fra faktisk
    // tilgjengelig plass), så det er kun font-størrelsene herfra som faktisk brukes i layoutet.
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    val playerCardHeight = (screenHeight * 0.25f).coerceIn(220.dp, 320.dp)
    val throwButtonHeight = (screenHeight * 0.045f).coerceIn(35.dp, 55.dp)
    val scoreDisplayHeight = (screenHeight * 0.07f).coerceIn(45.dp, 55.dp)
    val actionButtonHeight = (screenHeight * 0.065f).coerceIn(48.dp,62.dp)
    val numberButtonHeight = (screenHeight * 0.078f).coerceIn(52.dp,70.dp)

    val playerCardFontSize = (playerCardHeight.value * 0.20f).coerceIn(44f, 60f).sp
    val throwButtonFontSize = (throwButtonHeight.value * 0.24f).coerceIn(10f, 13f).sp
    val scoreDisplayFontSize = (scoreDisplayHeight.value * 0.35f).coerceIn(20f, 28f).sp
    val actionButtonFontSize = (actionButtonHeight.value * 0.26f).coerceIn(14f, 18f).sp
    val numberButtonFontSize = (numberButtonHeight.value * 0.4f).coerceIn(24f, 32f).sp

    val scope = rememberCoroutineScope()

    // Spillerne sin state. player1/player2 er "kilden til sannhet" for score, snitt osv,
    // og oppdateres kun via .copy() inne i confirmThrow/undoLastThrow.
    var player1 by remember { mutableStateOf(Player(player1Name)) }
    var player2 by remember { mutableStateOf(Player(player2Name)) }
    var currentPlayer by remember { mutableStateOf(1) } // 1 eller 2 - hvem sin tur er det
    var firstPlayer by remember { mutableStateOf(1) } // Brukes ved rematch for å bytte hvem som starter

    // De tre kastene i inneværende runde. Holdes som nullable Int slik at UI-et
    // (ThrowButton) vet om et kast enda ikke er registrert.
    var throw1 by remember { mutableStateOf<Int?>(null) }
    var throw2 by remember { mutableStateOf<Int?>(null) }
    var throw3 by remember { mutableStateOf<Int?>(null) }
    var throw1WasDouble by remember { mutableStateOf(false) }
    var throw2WasDouble by remember { mutableStateOf(false) }
    var throw3WasDouble by remember { mutableStateOf(false) }
    var currentThrow by remember { mutableStateOf(1) } // Hvilket av de 3 kastene i runden vi er på nå
    var overallRound by remember { mutableStateOf(1) } // Teller opp for hver runde (begge spillere kastet)

    // Tallpad-input: tallet brukeren har skrevet inn og hvilken multiplikator (Double/Triple) som er valgt
    var inputValue by remember { mutableStateOf("") }
    var multiplier by remember { mutableStateOf(1) }

    var showBustDialog by remember { mutableStateOf(false) }
    var bustMessage by remember { mutableStateOf("") }
    var showWinDialog by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<Player?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    var player1RoundHistory by remember { mutableStateOf(listOf<Int>()) } // Historikk over rundetotaler, vist i UI
    var player2RoundHistory by remember { mutableStateOf(listOf<Int>()) }
    // Full logg over hvert enkelt kast (begge spillere, hele kampen). Dette er det som
    // gjør undo mulig - vi kan "spole tilbake" state ved å se på siste oppføring.
    var dartHistory by remember { mutableStateOf(listOf<DartThrow>()) }

    // Verdien det aktuelle tallpad-inputet representerer, med multiplikator tatt hensyn til
    val currentInputScore = if (inputValue.isNotEmpty()) {
        (inputValue.toIntOrNull() ?: 0) * multiplier
    } else {
        0
    }
    // Et enkelt dart-kast kan maks gi 60 poeng (Triple 20), så alt utenfor 0..60 er ugyldig
    val isValidInput = if (inputValue.isEmpty()) {
        false
    } else {
        currentInputScore in 0..60
    }

    val roundTotal = (throw1 ?: 0) + (throw2 ?: 0) + (throw3 ?: 0)

    // Slår opp anbefalt utgangs-kombinasjon (checkout) for en gjenstående score, vist
    // på PlayerCard slik at spilleren ser hvordan de kan avslutte kampen. Tabellen dekker
    // alle scorer som faktisk er mulig å fullføre på 1-3 kast (maks er 170 = T20 T20 Bull).
    // 2..40 (partall) dekkes generisk med "D{score/2}" siden alle disse er en ren double.
    fun calculateCheckout(score: Int): String {
        return when (score) {
            170 -> "T20 T20 Bull"
            167 -> "T20 T19 Bull"
            164 -> "T20 T18 Bull"
            161 -> "T20 T17 Bull"
            160 -> "T20 T20 D20"
            159 -> "No out shot"
            158 -> "T20 T20 D19"
            157 -> "T20 T19 D20"
            156 -> "T20 T20 D18"
            155 -> "T20 T19 D19"
            154 -> "T20 T18 D20"
            153 -> "T20 T19 D18"
            152 -> "T20 T20 D16"
            151 -> "T20 T17 D20"
            150 -> "T20 T18 D18"
            149 -> "T20 T19 D16"
            148 -> "T20 T16 D20"
            147 -> "T20 T17 D18"
            146 -> "T20 T18 D16"
            145 -> "T20 T15 D20"
            144 -> "T20 T20 D12"
            143 -> "T20 T17 D16"
            142 -> "T20 T14 D20"
            141 -> "T20 T19 D12"
            140 -> "T20 T16 D16"
            139 -> "T19 T14 D20"
            138 -> "T20 T18 D12"
            137 -> "T19 T16 D16"
            136 -> "T20 T20 D8"
            135 -> "T20 T17 D12"
            134 -> "T20 T14 D16"
            133 -> "T20 T19 D8"
            132 -> "T20 T16 D12"
            131 -> "T20 T13 D16"
            130 -> "T20 20 Bull"
            129 -> "T19 T16 D12"
            128 -> "T18 T14 D16"
            127 -> "T20 T17 D8"
            126 -> "T19 T19 D6"
            125 -> "25 T20 D20"
            124 -> "T20 T16 D8"
            123 -> "T19 T16 D9"
            122 -> "T18 T20 D4"
            121 -> "T17 T10 D20"
            120 -> "T20 20 D20"
            119 -> "T19 T10 D16"
            118 -> "T20 18 D20"
            117 -> "T20 17 D20"
            116 -> "T20 16 D20"
            115 -> "T20 15 D20"
            114 -> "T20 14 D20"
            113 -> "T20 13 D20"
            112 -> "T20 12 D20"
            111 -> "T20 19 D16"
            110 -> "T20 18 D16"
            109 -> "T19 20 D16"
            108 -> "T20 16 D16"
            107 -> "T19 18 D16"
            106 -> "T20 14 D16"
            105 -> "T19 16 D16"
            104 -> "T18 18 D16"
            103 -> "T20 3 D20"
            102 -> "T20 10 D16"
            101 -> "T20 1 D20"
            100 -> "T20 D20"
            99 -> "T19 10 D16"
            98 -> "T20 D19"
            97 -> "T19 D20"
            96 -> "T20 D18"
            95 -> "T19 D19"
            94 -> "T18 D20"
            93 -> "T19 D18"
            92 -> "T20 D16"
            91 -> "T17 D20"
            90 -> "T20 D15"
            89 -> "T19 D16"
            88 -> "T16 D20"
            87 -> "T17 D18"
            86 -> "T18 D16"
            85 -> "T15 D20"
            84 -> "T20 D12"
            83 -> "T17 D16"
            82 -> "T14 D20"
            81 -> "T19 D12"
            80 -> "T20 D10"
            79 -> "T13 D20"
            78 -> "T18 D12"
            77 -> "T19 D10"
            76 -> "T20 D8"
            75 -> "T17 D12"
            74 -> "T14 D16"
            73 -> "T19 D8"
            72 -> "T16 D12"
            71 -> "T13 D16"
            70 -> "T10 D20"
            69 -> "T15 D12"
            68 -> "T20 D4"
            67 -> "T17 D8"
            66 -> "T10 D18"
            65 -> "T19 D4"
            64 -> "T16 D8"
            63 -> "T13 D12"
            62 -> "T10 D16"
            61 -> "T15 D8"
            60 -> "20 D20"
            in 2..40 step 2 -> "D${score / 2}"
            50 -> "Bull"
            // Disse scorene kan ikke fullføres på noen lovlig måte (for høye, eller ikke
            // nåbare med en gyldig kombinasjon av kast som ender på en double)
            else -> if (score > 170 || score == 169 || score == 168 || score == 166 ||
                score == 165 || score == 163 || score == 162 || score == 159) {
                "No out shot"
            } else ""
        }
    }

    // Gjennomsnittlig poengsum per tre kast ("three-dart average"), den vanlige måten
    // å måle en dartspillers nivå på. 501 - score = totalt antall poeng scoret så langt.
    fun recalculateAverage(player: Player): Double {
        if (player.dartsThrown == 0) return 0.0
        val totalScoreThrown = 501 - player.score
        return (totalScoreThrown.toDouble() / player.dartsThrown) * 3
    }

    // Sjekker om et kast (eller kastet som akkurat brakte scoren til 0) er en bust
    // etter 501-reglene:
    //  1. Går scoren under 0 -> alltid bust.
    //  2. Havner du på nøyaktig 1 med double-out aktivert -> alltid bust, siden man da
    //     aldri kan avslutte på en double (laveste double er D1=2).
    //  3. Havner du på nøyaktig 0 med double-out aktivert, men siste kast ikke var en
    //     double (og ikke bullseye, som teller som D25) -> bust.
    // throwTotal sendes inn som 0 her fordi kalleren allerede har trukket fra scoren
    // før checkBust kalles - currentScore er dermed allerede "scoren etter kastet".
    fun checkBust(currentScore: Int, throwTotal: Int, lastDartWasDouble: Boolean, lastDartValue: Int): Pair<Boolean, String> {
        val newScore = currentScore - throwTotal

        if (newScore < 0) {
            return Pair(true, "BUST! Score under 0")
        }

        if (newScore == 1 && doubleOutEnabled) {
            return Pair(true, "BUST! Cannot finish on 1")
        }

        if (newScore == 0 && doubleOutEnabled && !lastDartWasDouble && lastDartValue != 50) {
            return Pair(true, "BUST! Must finish on a double")
        }

        return Pair(false, "")
    }

    // Registrerer det gjeldende tallpad-inputet som spillerens neste kast. Dette er
    // hovedfunksjonen for spilllogikken: den håndterer double-in-krav, trekker fra
    // score, sjekker for bust, oppdaterer statistikk og bytter spiller/runde når runden
    // er ferdig (3 kast) eller kampen er vunnet.
    fun confirmThrow() {
        if (inputValue.isEmpty()) return

        val value = inputValue.toIntOrNull() ?: return
        val throwValue = value * multiplier
        val isDouble = multiplier == 2

        val activePlayer = if (currentPlayer == 1) player1 else player2

        // SPESIELL HÅNDTERING for throw 3 ved double in bust
        // Double-in: spilleren må treffe en double før poengene i det hele tatt begynner
        // å telle. Har spilleren ikke fått en double i løpet av runden (throw1/throw2/dette
        // kastet), er runden bomskutt uansett hva throwValue er - og siden dette er det
        // TREDJE og siste kastet, går runden rett videre til neste spiller (i stedet for
        // å bare hoppe til neste kast, som skjer i grenen under for throw 1/2).
        if (doubleInEnabled && !activePlayer.hasScored && throwValue > 0 && currentThrow == 3) {
            val hasDoubleInRound = throw1WasDouble || throw2WasDouble || isDouble

            if (!hasDoubleInRound) {
                bustMessage = "BUST! Must have a double to start scoring"
                showBustDialog = true

                // Oppdater dartsThrown
                if (currentPlayer == 1) {
                    player1 = player1.copy(dartsThrown = player1.dartsThrown + 1)
                } else {
                    player2 = player2.copy(dartsThrown = player2.dartsThrown + 1)
                }

                // Legg til i dart history
                dartHistory = dartHistory + DartThrow(
                    playerId = currentPlayer,
                    value = 0,
                    wasDouble = false,
                    throwNumber = 3
                )

                // BYTT SPILLER!
                currentPlayer = if (currentPlayer == 1) 2 else 1
                overallRound += 1
                throw1 = null
                throw2 = null
                throw3 = null
                throw1WasDouble = false
                throw2WasDouble = false
                throw3WasDouble = false
                currentThrow = 1
                inputValue = ""
                multiplier = 1
                return
            }
        }

        // Samme double-in-sjekk som over, men for kast 1 og 2: her er ikke runden ferdig
        // enda, så i stedet for å bytte spiller går vi bare videre til neste kast i runden
        // (spilleren får fortsatt prøve å treffe en double på sine gjenværende kast).
        if (doubleInEnabled && !activePlayer.hasScored && throwValue > 0 && currentThrow != 3) {
            val hasDoubleInRound = throw1WasDouble || throw2WasDouble || isDouble

            if (!hasDoubleInRound) {
                bustMessage = "BUST! Must have a double to start scoring"
                showBustDialog = true

                // Oppdater dartsThrown!
                if (currentPlayer == 1) {
                    player1 = player1.copy(dartsThrown = player1.dartsThrown + 1)
                } else {
                    player2 = player2.copy(dartsThrown = player2.dartsThrown + 1)
                }

                // Legg til i dart history
                dartHistory = dartHistory + DartThrow(
                    playerId = currentPlayer,
                    value = 0,
                    wasDouble = false,
                    throwNumber = currentThrow
                )

                // Gå til neste throw!
                if (currentThrow == 1) {
                    throw1 = null
                    throw1WasDouble = false
                    currentThrow = 2
                } else if (currentThrow == 2) {
                    throw1 = null
                    throw2 = null
                    throw1WasDouble = false
                    throw2WasDouble = false
                    currentThrow = 3
                }

                inputValue = ""
                multiplier = 1
                return
            }
        }

        // Selve poeng-registreringen for gjeldende kast. Strukturen er lik for currentThrow
        // 1, 2 og 3, men de skiller seg i to viktige ting:
        //  - Hvor mye av rundens totalsum som allerede er kastet (throw1/throw2 fra
        //    tidligere kast i samme runde legges sammen med dette kastet).
        //  - "Fast checkout": kampen kan avsluttes på ETHVERT av de 3 kastene (så snart
        //    scoren treffer nøyaktig 0 og det ikke er bust), ikke bare på kast 3. Derfor
        //    må dartsThrown/average/roundsPlayed oppdateres og winner/showWinDialog settes
        //    i alle tre grenene under, ikke bare i throw==3-grenen.
        when (currentThrow) {
            1 -> {
                throw1 = throwValue
                throw1WasDouble = isDouble

                if (currentPlayer == 1) {
                    val newScore = player1.score - throwValue
                    player1 = player1.copy(
                        score = newScore,
                        dartsThrown = player1.dartsThrown + 1
                    )

                    // hasScored markerer at spilleren har "åpnet" scoringen sin (kun
                    // relevant når double-in er aktivert - da må første poenggivende
                    // kast være en double)
                    if (throwValue > 0 && (!doubleInEnabled || isDouble)) {
                        player1 = player1.copy(hasScored = true)
                    }

                    if (newScore == 0) {
                        // Scoren er nøyaktig 0 allerede på første kast - sjekk om dette er
                        // et gyldig avslutningskast (double-out-krav)
                        val (isBust, message) = checkBust(newScore, 0, throw1WasDouble, throwValue)
                        if (isBust) {
                            // Bust: gi tilbake poengene og gå videre til kast 2 i samme runde
                            bustMessage = message
                            showBustDialog = true
                            player1 = player1.copy(score = player1.score + throwValue)
                            throw1 = null
                            throw1WasDouble = false
                            currentThrow = 2
                            inputValue = ""
                            multiplier = 1
                            return
                        } else {
                            // Gyldig checkout på første kast - kampen er vunnet med bare
                            // ett kast i runden. dartsThrown ble allerede talt med over.
                            player1 = player1.copy(
                                lastThrow = throwValue,
                                highestScore = maxOf(player1.highestScore, throwValue),
                                roundsPlayed = player1.roundsPlayed + 1,
                                average = recalculateAverage(player1)
                            )
                            player1RoundHistory = player1RoundHistory + throwValue
                            winner = player1
                            showWinDialog = true
                            return
                        }
                    }
                } else {
                    // Speilbilde av player1-grenen over, samme logikk for player2
                    val newScore = player2.score - throwValue
                    player2 = player2.copy(
                        score = newScore,
                        dartsThrown = player2.dartsThrown + 1
                    )

                    if (throwValue > 0 && (!doubleInEnabled || isDouble)) {
                        player2 = player2.copy(hasScored = true)
                    }

                    if (newScore == 0) {
                        val (isBust, message) = checkBust(newScore, 0, throw1WasDouble, throwValue)
                        if (isBust) {
                            bustMessage = message
                            showBustDialog = true
                            player2 = player2.copy(score = player2.score + throwValue)
                            throw1 = null
                            throw1WasDouble = false
                            currentThrow = 2
                            inputValue = ""
                            multiplier = 1
                            return
                        } else {
                            player2 = player2.copy(
                                lastThrow = throwValue,
                                highestScore = maxOf(player2.highestScore, throwValue),
                                roundsPlayed = player2.roundsPlayed + 1,
                                average = recalculateAverage(player2)
                            )
                            player2RoundHistory = player2RoundHistory + throwValue
                            winner = player2
                            showWinDialog = true
                            return
                        }
                    }
                }

                // Scoren traff ikke 0 (eller var bust) - gå videre til kast 2 og logg kastet
                currentThrow = 2
                dartHistory = dartHistory + DartThrow(
                    playerId = currentPlayer,
                    value = throwValue,
                    wasDouble = isDouble,
                    throwNumber = 1
                )
            }
            2 -> {
                throw2 = throwValue
                throw2WasDouble = isDouble

                if (currentPlayer == 1) {
                    val newScore = player1.score - throwValue
                    player1 = player1.copy(
                        score = newScore,
                        dartsThrown = player1.dartsThrown + 1
                    )

                    if (throwValue > 0 && (!doubleInEnabled || throw1WasDouble || isDouble)) {
                        player1 = player1.copy(hasScored = true)
                    }

                    if (newScore == 0) {
                        val (isBust, message) = checkBust(newScore, 0, throw2WasDouble, throwValue)
                        if (isBust) {
                            // Bust på kast 2: gi tilbake BEGGE kastene i runden (throw1 + dette),
                            // ikke bare det siste, siden hele runden annulleres
                            bustMessage = message
                            showBustDialog = true
                            player1 = player1.copy(score = player1.score + (throw1 ?: 0) + throwValue)
                            throw1 = null
                            throw2 = null
                            throw1WasDouble = false
                            throw2WasDouble = false
                            currentThrow = 3
                            inputValue = ""
                            multiplier = 1
                            return
                        } else {
                            // Gyldig checkout på kast 2 - lastThrow/highestScore skal telle
                            // summen av begge kastene i runden, ikke bare dette ene kastet
                            val roundTotalNow = (throw1 ?: 0) + throwValue
                            player1 = player1.copy(
                                lastThrow = roundTotalNow,
                                highestScore = maxOf(player1.highestScore, roundTotalNow),
                                roundsPlayed = player1.roundsPlayed + 1,
                                average = recalculateAverage(player1)
                            )
                            player1RoundHistory = player1RoundHistory + roundTotalNow
                            winner = player1
                            showWinDialog = true
                            return
                        }
                    }
                } else {
                    // Speilbilde av player1-grenen over
                    val newScore = player2.score - throwValue
                    player2 = player2.copy(
                        score = newScore,
                        dartsThrown = player2.dartsThrown + 1
                    )

                    if (throwValue > 0 && (!doubleInEnabled || throw1WasDouble || isDouble)) {
                        player2 = player2.copy(hasScored = true)
                    }

                    if (newScore == 0) {
                        val (isBust, message) = checkBust(newScore, 0, throw2WasDouble, throwValue)
                        if (isBust) {
                            bustMessage = message
                            showBustDialog = true
                            player2 = player2.copy(score = player2.score + (throw1 ?: 0) + throwValue)
                            throw1 = null
                            throw2 = null
                            throw1WasDouble = false
                            throw2WasDouble = false
                            currentThrow = 3
                            inputValue = ""
                            multiplier = 1
                            return
                        } else {
                            val roundTotalNow = (throw1 ?: 0) + throwValue
                            player2 = player2.copy(
                                lastThrow = roundTotalNow,
                                highestScore = maxOf(player2.highestScore, roundTotalNow),
                                roundsPlayed = player2.roundsPlayed + 1,
                                average = recalculateAverage(player2)
                            )
                            player2RoundHistory = player2RoundHistory + roundTotalNow
                            winner = player2
                            showWinDialog = true
                            return
                        }
                    }
                }

                currentThrow = 3
                dartHistory = dartHistory + DartThrow(
                    playerId = currentPlayer,
                    value = throwValue,
                    wasDouble = isDouble,
                    throwNumber = 2
                )
            }
            // Siste kast i runden. I motsetning til kast 1 og 2 kalles checkBust her
            // ALLTID (ikke bare når newScore == 0), fordi dette er siste sjanse i runden
            // til å oppdage at spilleren gikk under 0 - hvis det skjedde på kast 1 eller 2
            // fanges det ikke opp der (bust sjekkes kun ved score==0 i de kastene), men
            // siden score kun blir mer negativ utover i runden vil det uansett fanges opp
            // her, etter at alle 3 kastene er registrert.
            3 -> {
                throw3 = throwValue
                throw3WasDouble = isDouble

                val total = (throw1 ?: 0) + (throw2 ?: 0) + (throw3 ?: 0)

                if (currentPlayer == 1) {
                    val newScore = player1.score - throwValue
                    player1 = player1.copy(score = newScore)

                    if (throwValue > 0 && (!doubleInEnabled || throw1WasDouble || throw2WasDouble || isDouble)) {
                        player1 = player1.copy(hasScored = true)
                    }

                    val (isBust, message) = checkBust(newScore, 0, throw3WasDouble, throwValue)

                    if (isBust) {
                        // Bust på siste kast: gi tilbake HELE rundens poengsum (total,
                        // ikke bare throwValue), siden score allerede ble trukket fra bare
                        // for dette ene kastet over
                        bustMessage = message
                        showBustDialog = true
                        val newDartsThrown = player1.dartsThrown + 1
                        player1 = player1.copy(
                            score = player1.score + total,
                            dartsThrown = newDartsThrown
                        )
                    } else {
                        // Gyldig runde (eller gyldig checkout på siste kast). Snittet regnes
                        // ut fra en midlertidig kopi med oppdatert score/dartsThrown, siden
                        // player1 selv ikke er oppdatert med de nye verdiene enda på dette
                        // tidspunktet i .copy()-kallet.
                        val newDartsThrown = player1.dartsThrown + 1

                        player1 = player1.copy(
                            lastThrow = total,
                            highestScore = maxOf(player1.highestScore, total),
                            roundsPlayed = player1.roundsPlayed + 1,
                            dartsThrown = newDartsThrown,
                            average = recalculateAverage(player1.copy(score = newScore, dartsThrown = newDartsThrown))
                        )
                        player1RoundHistory = player1RoundHistory + total

                        if (newScore == 0) {
                            winner = player1
                            showWinDialog = true
                        }
                    }
                } else {
                    // Speilbilde av player1-grenen over
                    val newScore = player2.score - throwValue
                    player2 = player2.copy(score = newScore)

                    if (throwValue > 0 && (!doubleInEnabled || throw1WasDouble || throw2WasDouble || isDouble)) {
                        player2 = player2.copy(hasScored = true)
                    }

                    val (isBust, message) = checkBust(newScore, 0, throw3WasDouble, throwValue)

                    if (isBust) {
                        bustMessage = message
                        showBustDialog = true
                        val newDartsThrown = player2.dartsThrown + 1
                        player2 = player2.copy(
                            score = player2.score + total,
                            dartsThrown = newDartsThrown
                        )
                    } else {
                        val newDartsThrown = player2.dartsThrown + 1

                        player2 = player2.copy(
                            lastThrow = total,
                            highestScore = maxOf(player2.highestScore, total),
                            roundsPlayed = player2.roundsPlayed + 1,
                            dartsThrown = newDartsThrown,
                            average = recalculateAverage(player2.copy(score = newScore, dartsThrown = newDartsThrown))
                        )
                        player2RoundHistory = player2RoundHistory + total

                        if (newScore == 0) {
                            winner = player2
                            showWinDialog = true
                        }
                    }
                }

                dartHistory = dartHistory + DartThrow(
                    playerId = currentPlayer,
                    value = throwValue,
                    wasDouble = isDouble,
                    throwNumber = 3
                )

                // Runden er ferdig (3 kast kastet) uten at kampen ble vunnet - bytt spiller
                // og nullstill kastene for neste runde
                currentPlayer = if (currentPlayer == 1) 2 else 1
                overallRound += 1
                throw1 = null
                throw2 = null
                throw3 = null
                throw1WasDouble = false
                throw2WasDouble = false
                throw3WasDouble = false
                currentThrow = 1
            }
        }

        inputValue = ""
        multiplier = 1
    }

    // Angrer det aller siste registrerte kastet (uansett hvilken spiller det var, og
    // uansett om det var en bust eller ikke - bust-kast med value=0 legges også i
    // dartHistory, se confirmThrow). Bygger opp igjen throw1/throw2/throw3-state ved å
    // lete gjennom resten av dartHistory etter spillerens tidligere kast i samme runde.
    fun undoLastThrow() {
        if (dartHistory.isEmpty()) return

        val lastDart = dartHistory.last()
        dartHistory = dartHistory.dropLast(1)

        val targetPlayer = if (lastDart.playerId == 1) player1 else player2
        val newScore = targetPlayer.score + lastDart.value
        val newDartsThrown = maxOf(0, targetPlayer.dartsThrown - 1)

        if (lastDart.playerId == 1) {
            player1 = player1.copy(
                score = newScore,
                dartsThrown = newDartsThrown,
                average = recalculateAverage(player1.copy(score = newScore, dartsThrown = newDartsThrown))
            )
        } else {
            player2 = player2.copy(
                score = newScore,
                dartsThrown = newDartsThrown,
                average = recalculateAverage(player2.copy(score = newScore, dartsThrown = newDartsThrown))
            )
        }

        // Gjenoppbygger throw1/throw2/throw3-visningen ut fra hvor i runden det angrede
        // kastet var. Hvis det var kast 1, var det ingen tidligere kast i runden å vise -
        // vi hopper rett tilbake til currentThrow = 1. Var det kast 2 eller 3, må vi finne
        // spillerens forrige kast(ene) i samme runde fra det som er igjen i dartHistory.
        when (lastDart.throwNumber) {
            1 -> {
                throw1 = null
                throw1WasDouble = false

                // Kast 1 var starten på runden - hvis motstanderen har kastet etter dette
                // (currentPlayer er ikke lenger lastDart sin spiller), må vi bytte tilbake
                // til denne spilleren og gå én runde tilbake
                if (currentPlayer != lastDart.playerId) {
                    currentPlayer = lastDart.playerId
                    overallRound = maxOf(1, overallRound - 1)
                }
                currentThrow = 1
            }
            2 -> {
                throw2 = null
                throw2WasDouble = false

                // Finn spillerens kast 1 i samme runde (siste treff med throwNumber==1
                // for denne spilleren i det som er igjen av historikken) og gjenopprett det
                val previousThrow1 = dartHistory.lastOrNull {
                    it.playerId == lastDart.playerId && it.throwNumber == 1
                }
                if (previousThrow1 != null) {
                    throw1 = previousThrow1.value
                    throw1WasDouble = previousThrow1.wasDouble
                }

                if (currentPlayer != lastDart.playerId) {
                    currentPlayer = lastDart.playerId
                }
                currentThrow = 2
            }
            3 -> {
                throw3 = null
                throw3WasDouble = false

                // Samme prinsipp som over, men må gjenopprette både kast 1 og kast 2
                val previousThrows = dartHistory.filter { it.playerId == lastDart.playerId }
                val previousThrow1 = previousThrows.lastOrNull { it.throwNumber == 1 }
                val previousThrow2 = previousThrows.lastOrNull { it.throwNumber == 2 }

                if (previousThrow1 != null) {
                    throw1 = previousThrow1.value
                    throw1WasDouble = previousThrow1.wasDouble
                }
                if (previousThrow2 != null) {
                    throw2 = previousThrow2.value
                    throw2WasDouble = previousThrow2.wasDouble
                }

                if (currentPlayer != lastDart.playerId) {
                    currentPlayer = lastDart.playerId
                }
                currentThrow = 3
            }
        }

        // Var det angrede kastet det tredje (siste) i runden, må vi også rulle tilbake
        // roundsPlayed/lastThrow/roundHistory til slik det var FØR den runden - da må vi
        // se på runden før den igjen (takeLast(3) på det som er igjen i historikken)
        if (lastDart.throwNumber == 3) {
            val previousRoundTotal = dartHistory
                .filter { it.playerId == lastDart.playerId }
                .takeLast(3)
                .sumOf { it.value }

            if (lastDart.playerId == 1) {
                player1 = player1.copy(
                    lastThrow = if (previousRoundTotal > 0) previousRoundTotal else 0,
                    roundsPlayed = maxOf(0, player1.roundsPlayed - 1)
                )
                if (player1RoundHistory.isNotEmpty()) {
                    player1RoundHistory = player1RoundHistory.dropLast(1)
                }
            } else {
                player2 = player2.copy(
                    lastThrow = if (previousRoundTotal > 0) previousRoundTotal else 0,
                    roundsPlayed = maxOf(0, player2.roundsPlayed - 1)
                )
                if (player2RoundHistory.isNotEmpty()) {
                    player2RoundHistory = player2RoundHistory.dropLast(1)
                }
            }
        }

        inputValue = ""
        multiplier = 1
    }

    // Bekreftelsesdialog når brukeren trykker tilbake-pilen midt i en kamp - kampen
    // lagres ikke hvis de bekrefter exit
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    "Exit Game?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    "The current game will not be saved. Are you sure you want to exit?",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xDD000000)
        )
    }

    // Vises når confirmThrow() oppdaget en bust (se checkBust/confirmThrow). bustMessage
    // forteller hvilken av de tre bust-reglene som ble brutt.
    if (showBustDialog) {
        AlertDialog(
            onDismissRequest = {
                showBustDialog = false
                inputValue = ""
                multiplier = 1
            },
            title = { Text("BUST!", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text(bustMessage, color = Color.White) },
            confirmButton = {
                Button(
                    onClick = {
                        showBustDialog = false
                        inputValue = ""
                        multiplier = 1
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text("OK")
                }
            },
            containerColor = Color(0xDD000000)
        )
    }

    // Vises når en spiller vinner (score nøyaktig 0 med gyldig checkout). Har ingen
    // "dismiss" (onDismissRequest er tom) - spilleren MÅ velge Main Menu eller Rematch,
    // det finnes ingen vei tilbake til selve spillet herfra.
    if (showWinDialog && winner != null) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🎯 WINNER! 🎯",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        winner!!.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.3f))
                    Text(
                        "Average: ${String.format("%.1f", winner!!.average)}",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        "Rounds: ${winner!!.roundsPlayed}",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        "Darts: ${winner!!.dartsThrown}",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val players = playerViewModel.players.value
                                    val p1 = players.find { it.username == player1Name }
                                    val p2 = players.find { it.username == player2Name }

                                    if (p1 != null && p2 != null) {
                                        val winnerId = if (winner == player1) p1.playerId else p2.playerId

                                        val player1Stats = GameStatsEntity(
                                            gameId = 0,
                                            playerId = p1.playerId,
                                            average = player1.average,
                                            highestScore = player1.highestScore,
                                            dartsThrown = player1.dartsThrown,
                                            roundsPlayed = player1.roundsPlayed,
                                            finalScore = player1.score
                                        )

                                        val player2Stats = GameStatsEntity(
                                            gameId = 0,
                                            playerId = p2.playerId,
                                            average = player2.average,
                                            highestScore = player2.highestScore,
                                            dartsThrown = player2.dartsThrown,
                                            roundsPlayed = player2.roundsPlayed,
                                            finalScore = player2.score
                                        )

                                        gameViewModel.saveGame(
                                            player1Id = p1.playerId,
                                            player2Id = p2.playerId,
                                            winnerId = winnerId,
                                            doubleIn = doubleInEnabled,
                                            doubleOut = doubleOutEnabled,
                                            player1Stats = player1Stats,
                                            player2Stats = player2Stats
                                        )
                                    }
                                }
                                // main_menu blir ny bunn i back-stacken (popUpTo inclusive) -
                                // "tilbake" fra hovedmenyen skal aldri kunne havne tilbake i
                                // den ferdigspilte kampen
                                navController.navigate("main_menu") {
                                    popUpTo("main_menu") { inclusive = true }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFC1E69)
                            )
                        ) {
                            Text("Main Menu")
                        }
                        // "Rematch"-knappen: lagrer kampen (samme som over), men i stedet for
                        // å navigere bort nullstiller den all spill-state lokalt slik at en ny
                        // kamp starter med de samme to spillerne uten å forlate skjermen
                        Button(
                            onClick = {
                                scope.launch {
                                    val players = playerViewModel.players.value
                                    val p1 = players.find { it.username == player1Name }
                                    val p2 = players.find { it.username == player2Name }

                                    if (p1 != null && p2 != null) {
                                        val winnerId = if (winner == player1) p1.playerId else p2.playerId

                                        val player1Stats = GameStatsEntity(
                                            gameId = 0,
                                            playerId = p1.playerId,
                                            average = player1.average,
                                            highestScore = player1.highestScore,
                                            dartsThrown = player1.dartsThrown,
                                            roundsPlayed = player1.roundsPlayed,
                                            finalScore = player1.score
                                        )

                                        val player2Stats = GameStatsEntity(
                                            gameId = 0,
                                            playerId = p2.playerId,
                                            average = player2.average,
                                            highestScore = player2.highestScore,
                                            dartsThrown = player2.dartsThrown,
                                            roundsPlayed = player2.roundsPlayed,
                                            finalScore = player2.score
                                        )

                                        gameViewModel.saveGame(
                                            player1Id = p1.playerId,
                                            player2Id = p2.playerId,
                                            winnerId = winnerId,
                                            doubleIn = doubleInEnabled,
                                            doubleOut = doubleOutEnabled,
                                            player1Stats = player1Stats,
                                            player2Stats = player2Stats
                                        )

                                        // Bytt hvem som starter neste kamp, som i en vanlig
                                        // darts-revansje (taperen/den som ikke startet forrige gang kaster først)
                                        firstPlayer = if (firstPlayer == 1) 2 else 1
                                        currentPlayer = firstPlayer

                                        player1 = Player(player1Name)
                                        player2 = Player(player2Name)
                                        player1RoundHistory = listOf()
                                        player2RoundHistory = listOf()
                                        dartHistory = listOf()
                                        overallRound = 1
                                        throw1 = null
                                        throw2 = null
                                        throw3 = null
                                        throw1WasDouble = false
                                        throw2WasDouble = false
                                        throw3WasDouble = false
                                        currentThrow = 1
                                        winner = null
                                        showWinDialog = false
                                        inputValue = ""
                                        multiplier = 1
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Rematch")
                        }
                    }
                }
            },
            dismissButton = { },
            containerColor = Color(0xDD000000)
        )
    }

    // Selve spillebrettet. BoxWithConstraints gir oss faktisk tilgjengelig plass
    // (maxWidth/maxHeight) etter at f.eks. statusbar/navigasjon er trukket fra, som brukes
    // til å regne ut størrelser proporsjonalt med skjermen (bedre tilpasning enn faste dp-verdier
    // på tvers av forskjellige telefoner). Disse variablene skygger de likelydende fra
    // lenger oppe i funksjonen.
    BoxWithConstraints {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val playerCardHeight = (screenHeight * 0.28f).coerceAtMost(180.dp)
        val throwButtonHeight = (screenHeight * 0.08f).coerceAtMost(70.dp)
        val scoreDisplayHeight = (screenHeight * 0.09f).coerceAtMost(90.dp)
        val actionButtonHeight = (screenHeight * 0.08f).coerceAtMost(58.dp)
        val numberButtonHeight = (screenHeight * 0.1f).coerceAtMost(60.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .padding((screenWidth*0.02f).coerceAtLeast(8.dp)),
            verticalArrangement = Arrangement.Top
        ) {
            IconButton(
                onClick = { showExitDialog = true },
                modifier = Modifier
                    .align(Alignment.Start)
                    .size(48.dp)
                    .shadow(8.dp, CircleShape)
                    .background(Color(0xCC000000), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerCard(
                    player = player1,
                    isActive = currentPlayer == 1,
                    backgroundColor = Color(0xFF505050),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    checkout = calculateCheckout(player1.score),
                    roundNumber = overallRound
                )

                PlayerCard(
                    player = player2,
                    isActive = currentPlayer == 2,
                    backgroundColor = Color(0xFF505050),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    checkout = calculateCheckout(player2.score),
                    roundNumber = overallRound
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.05f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThrowButton(
                    label = "THROW 1:",
                    value = throw1,
                    isActive = currentThrow == 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .fillMaxHeight()
                )
                ThrowButton(
                    label = "THROW 2:",
                    value = throw2,
                    isActive = currentThrow == 2,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .fillMaxHeight()
                )
                ThrowButton(
                    label = "THROW 3:",
                    value = throw3,
                    isActive = currentThrow == 3,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .fillMaxHeight()
                )
            }

            // Viser tallpad-inputet live etter hvert som brukeren taster, med multiplikatoren
            // ("× 2"/"× 3") lagt til hvis Double/Triple er valgt. "..." vises som placeholder
            // når feltet er tomt.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.10f)
                    .padding(8.dp)
                    .shadow(12.dp, RoundedCornerShape(15.dp), spotColor = Color(0xFFF5DF20))
                    .background(Color(0xFF0A0A0A), RoundedCornerShape(15.dp))
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color(0xFF333333),
                            cornerRadius = CornerRadius(15.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (inputValue.isNotEmpty()) {
                        "$inputValue ${if (multiplier > 1) "× $multiplier" else ""}"
                    } else {
                        "..."
                    },
                    fontSize = (scoreDisplayFontSize.value * 1.6f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (inputValue.isNotEmpty()) Color(0xFFE7D325) else Color(0xFFE1CD1B),

                    )
            }

            // Undo / Double / Triple-raden. Double og Triple er "toggle"-knapper (trykk igjen
            // for å slå av) som styrer multiplier-verdien som currentInputScore ganges med.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.06f)
                    .background(Color(0xEBF148E8), RoundedCornerShape(8.dp))
                    .padding(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                val undoInteraction = remember { MutableInteractionSource() }
                val isUndoPressed by undoInteraction.collectIsPressedAsState()

                Button(
                    onClick = { undoLastThrow() },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF505050)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    border = if (isUndoPressed) BorderStroke(3.dp, Color(0xFFB2073F)) else null,
                    interactionSource = undoInteraction
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "↺",
                            fontSize = (actionButtonFontSize.value * 1.8f).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            softWrap = false,
                            maxLines = 1,
                            modifier=Modifier.offset(y=(-4).dp)

                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Undo",
                            fontSize = actionButtonFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }

                val doubleInteraction = remember { MutableInteractionSource() }
                val isDoublePressed by doubleInteraction.collectIsPressedAsState()

                Button(
                    onClick = { multiplier = if (multiplier == 2) 1 else 2 },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF505050)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    border = if (multiplier == 2 || isDoublePressed) BorderStroke(3.dp, Color(
                        0xFFDECA2A
                    )
                    ) else null,
                    interactionSource = doubleInteraction
                ) {
                    Text(
                        "Double",
                        fontSize = actionButtonFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                val tripleInteraction = remember { MutableInteractionSource() }
                val isTriplePressed by tripleInteraction.collectIsPressedAsState()

                Button(
                    onClick = { multiplier = if (multiplier == 3) 1 else 3 },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF505050)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    border = if (multiplier == 3 || isTriplePressed) BorderStroke(3.dp, Color(
                        0xFFDECA2A
                    )
                    ) else null,
                    interactionSource = tripleInteraction
                ) {
                    Text(
                        "Triple",
                        fontSize = actionButtonFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Runder kun av de ytre hjørnene til talltastaturet (venstre knapp i raden får
            // rundet venstre hjørne, høyre knapp får rundet høyre hjørne, midtre knapp
            // forblir firkantet) slik at hele tastaturet ser ut som én sammenhengende,
            // avrundet blokk selv om den består av mange enkeltknapper med 1dp mellomrom.
            fun getCornerShape(position: Int, totalInRow: Int, includeBottom: Boolean = false): RoundedCornerShape {
                return when (position) {
                    0 -> RoundedCornerShape(
                        topStart = 6.dp,
                        bottomStart = if (includeBottom) 6.dp else 0.dp
                    )
                    totalInRow - 1 -> RoundedCornerShape(
                        topEnd = 6.dp,
                        bottomEnd = if (includeBottom) 6.dp else 0.dp
                    )
                    else -> RoundedCornerShape(0.dp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.30f)
                    .background(Color(0xEBF148E8), RoundedCornerShape(8.dp))
                    .padding(1.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    for (i in 1..3) {
                        NumberButton(
                            number = i,
                            onClick = { inputValue += i.toString() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            shape = getCornerShape(i-1,3, includeBottom=true)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    for (i in 4..6) {
                        NumberButton(
                            number = i,
                            onClick = { inputValue += i.toString() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            shape = getCornerShape(i-4,3,includeBottom=true)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    for (i in 7..9) {
                        NumberButton(
                            number = i,
                            onClick = { inputValue += i.toString() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            shape = getCornerShape(i-7,3,includeBottom=true)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    val clearInteraction = remember { MutableInteractionSource() }
                    val isClearPressed by clearInteraction.collectIsPressedAsState()

                    Button(
                        onClick = {
                            inputValue = ""
                            multiplier = 1
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF505050)),
                        shape = RoundedCornerShape(bottomStart = 6.dp, topStart = 6.dp, bottomEnd = 6.dp),
                        border = if (isClearPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
                        interactionSource = clearInteraction
                    ) {
                        Text(
                            "CLR",
                            fontSize = numberButtonFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier

                        )
                    }

                    NumberButton(
                        number = 0,
                        onClick = { inputValue += "0" },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                    )

                    // Bekreft-knappen ("✓"/"✗"): grønn og aktiv når inputet er tomt (ingenting
                    // å bekrefte enda) eller gyldig (0-60), rød når spilleren har tastet inn
                    // noe som er for høyt til å være et lovlig enkeltkast - gir umiddelbar
                    // visuell tilbakemelding før de i det hele tatt trykker
                    val confirmInteraction = remember { MutableInteractionSource() }
                    val isConfirmPressed by confirmInteraction.collectIsPressedAsState()

                    Button(
                        onClick = {
                            if (isValidInput) {
                                confirmThrow()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inputValue.isEmpty()) Color(0xFF4CAF50)
                            else if (isValidInput) Color(0xFF4CAF50)
                            else Color(0xFFFF0000),
                            disabledContainerColor = if (!isValidInput && inputValue.isNotEmpty()) Color(0xFFFF0000)
                            else Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(bottomEnd = 6.dp, bottomStart = 6.dp, topEnd = 6.dp),
                        border = if (isConfirmPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
                        interactionSource = confirmInteraction
                    ) {
                        Text(
                            text = if (inputValue.isEmpty()) "✓" else if (isValidInput) "✓" else "✗",
                            fontSize = numberButtonFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}

// Statuskortet for én spiller (navn, score, siste kast, checkout-forslag, snitt/runde/darts).
// isActive styrer highlighten (glow-ramme + lysere gradient) som viser hvem sin tur det er.
// currentRoundTotal er deklarert men brukes ikke i selve visningen her.
@Composable
fun PlayerCard(
    player: Player,
    isActive: Boolean,
    backgroundColor: Color,
    checkout: String,
    roundNumber: Int,
    currentRoundTotal: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .shadow(
                elevation = if (isActive) 16.dp else 8.dp,
                shape = RoundedCornerShape(8.dp),
                spotColor = Color(0xEBF148E8)
            )
            .then(
                if (isActive) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color(0xEBF148E8),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints {
            val cardHeight = maxHeight
            with(LocalDensity.current) {
                val fontSize = (cardHeight * 0.3f).toSp().coerceAtMost(32.sp)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (isActive) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF505050),
                                        Color(0xFF3A3A3A),
                                        Color(0xFF505050)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(listOf(backgroundColor, backgroundColor))
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding((cardHeight.value * 0.055f).dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isActive) {
                                    Text(
                                        text = "→",
                                        fontSize = (fontSize.value * 0.25f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                    )
                                }
                                Text(
                                    text = player.name,
                                    fontSize = (fontSize.value * 0.35f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = "${player.score}",
                            fontSize = (fontSize.value * 1.5f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "LAST: ${player.lastThrow}",
                                fontSize = (fontSize.value * 0.35f).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = checkout,
                                fontSize = (fontSize.value * 0.35f).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "AVG\n${String.format("%.1f", player.average)}",
                                    fontSize = (fontSize.value * 0.35f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "ROUND\n$roundNumber",
                                    fontSize = (fontSize.value * 0.35f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "DARTS\n${player.dartsThrown}",
                                    fontSize = (fontSize.value * 0.35f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Viser ett av de tre kastene i inneværende runde ("THROW 1: 60" osv). Er ikke faktisk
// klikkbar (onClick er tom) - dette er kun en visningsboks, ikke et input-element.
// isActive markerer hvilket av de tre kastene som er "på tur" med en glow-kant.
@Composable
fun ThrowButton(
    label: String,
    value: Int?,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val buttonHeight = maxHeight
        val cornerRadius = buttonHeight * 0.5f

        with(LocalDensity.current) {
            val fontSize = (buttonHeight * 0.24f).toSp().coerceAtMost(13.sp)

            Button(
                onClick = { },
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B6B6B)
                ),
                shape = RoundedCornerShape(cornerRadius),
                border = if (isActive) {
                    BorderStroke(1.5.dp, Color(0xEBF148E8))
                } else null,
                contentPadding = PaddingValues(horizontal = 1.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$label ${value ?: ""}",
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// Gjenbrukbar tastaturknapp for tallpaden (0-9). shape sendes inn utenfra via
// getCornerShape() slik at kun de ytre knappene i rutenettet får avrundede hjørner.
@Composable
fun NumberButton(
    number: Int,
    onClick: () -> Unit,
    modifier: Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp)
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF505050)
        ),
        border = if (isPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
        interactionSource = interaction
    ) {
        Text(
            text = number.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = Color.White
        )
    }
}