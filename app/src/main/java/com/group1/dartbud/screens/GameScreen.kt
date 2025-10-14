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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class Player(
    val name: String,
    var score: Int = 501,
    var lastThrow: Int = 0,
    var average: Double = 0.0,
    var roundsPlayed: Int = 0,
    var dartsThrown: Int = 0,
    var hasScored: Boolean = false
)

@Composable
fun GameScreen(
    navController: NavController,
    doubleInEnabled: Boolean = false,
    doubleOutEnabled: Boolean = true,
    player1Name: String = "PLAYER 1",
    player2Name: String = "PLAYER 2",
    gameViewModel: GameViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {

    // Responsive sizing
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
    var player1 by remember { mutableStateOf(Player(player1Name)) }
    var player2 by remember { mutableStateOf(Player(player2Name)) }
    var currentPlayer by remember { mutableStateOf(1) }
    var firstPlayer by remember { mutableStateOf(1) }

    var throw1 by remember { mutableStateOf<Int?>(null) }
    var throw2 by remember { mutableStateOf<Int?>(null) }
    var throw3 by remember { mutableStateOf<Int?>(null) }
    var throw1WasDouble by remember { mutableStateOf(false) }
    var throw2WasDouble by remember { mutableStateOf(false) }
    var throw3WasDouble by remember { mutableStateOf(false) }
    var currentThrow by remember { mutableStateOf(1) }
    var overallRound by remember { mutableStateOf(1) }

    var inputValue by remember { mutableStateOf("") }
    var multiplier by remember { mutableStateOf(1) }

    var showBustDialog by remember { mutableStateOf(false) }
    var bustMessage by remember { mutableStateOf("") }
    var showWinDialog by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<Player?>(null) }

    val currentInputScore = if (inputValue.isNotEmpty()) {
        (inputValue.toIntOrNull() ?: 0) * multiplier
    } else {
        0
    }
    val isValidInput = if (inputValue.isEmpty()) {
        false
    } else {
        currentInputScore in 0..60
    }

    val roundTotal = (throw1 ?: 0) + (throw2 ?: 0) + (throw3 ?: 0)

    // Checkout-tabell
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
            else -> if (score > 170 || score == 169 || score == 168 || score == 166 ||
                score == 165 || score == 163 || score == 162 || score == 159) {
                "No out shot"
            } else ""
        }
    }

    fun recalculateAverage(player: Player): Double {
        if (player.dartsThrown == 0) return 0.0
        val totalScoreThrown = 501 - player.score
        return (totalScoreThrown.toDouble() / player.dartsThrown) * 3
    }

    fun checkBust(currentScore: Int, throwTotal: Int, lastDartWasDouble: Boolean, lastDartValue: Int): Pair<Boolean, String> {
        val newScore = currentScore - throwTotal

        if (newScore < 0) {
            return Pair(true, "BUST! Score under 0")
        }

        if (newScore == 1) {
            return Pair(true, "BUST! Cannot finish on 1")
        }

        if (newScore == 0 && doubleOutEnabled && !lastDartWasDouble && lastDartValue != 50) {
            return Pair(true, "BUST! Must finish on a double")
        }

        return Pair(false, "")
    }

    fun confirmThrow() {
        if (inputValue.isEmpty()) return

        val value = inputValue.toIntOrNull() ?: return
        val throwValue = value * multiplier
        val isDouble = multiplier == 2

        val activePlayer = if (currentPlayer == 1) player1 else player2

        if (doubleInEnabled && !activePlayer.hasScored && throwValue > 0) {
            val hasDoubleInRound = throw1WasDouble || throw2WasDouble || isDouble

            if (!hasDoubleInRound) {
                bustMessage = "BUST! Must have a double to start scoring"
                showBustDialog = true
                inputValue = ""
                multiplier = 1
                return
            }
        }

        when (currentThrow) {
            1 -> {
                throw1 = throwValue
                throw1WasDouble = isDouble

                if (currentPlayer == 1) {
                    val newScore = player1.score - throwValue
                    player1 = player1.copy(score = newScore)

                    if (throwValue > 0 && (!doubleInEnabled || isDouble)) {
                        player1 = player1.copy(hasScored = true)
                    }

                    if (newScore == 0) {
                        val (isBust, message) = checkBust(newScore, 0, throw1WasDouble, throwValue)
                        if (isBust) {
                            bustMessage = message
                            showBustDialog = true
                            player1 = player1.copy(score = player1.score + throwValue)
                            throw1 = null
                            throw1WasDouble = false
                            inputValue = ""
                            multiplier = 1
                            return
                        } else {
                            winner = player1
                            showWinDialog = true
                            return
                        }
                    }
                } else {
                    val newScore = player2.score - throwValue
                    player2 = player2.copy(score = newScore)

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
                            inputValue = ""
                            multiplier = 1
                            return
                        } else {
                            winner = player2
                            showWinDialog = true
                            return
                        }
                    }
                }

                currentThrow = 2
            }
            2 -> {
                throw2 = throwValue
                throw2WasDouble = isDouble

                if (currentPlayer == 1) {
                    val newScore = player1.score - throwValue
                    player1 = player1.copy(score = newScore)

                    if (throwValue > 0 && (!doubleInEnabled || throw1WasDouble || isDouble)) {
                        player1 = player1.copy(hasScored = true)
                    }

                    if (newScore == 0) {
                        val (isBust, message) = checkBust(newScore, 0, throw2WasDouble, throwValue)
                        if (isBust) {
                            bustMessage = message
                            showBustDialog = true
                            player1 = player1.copy(score = player1.score + (throw1 ?: 0) + throwValue)
                            throw1 = null
                            throw2 = null
                            throw1WasDouble = false
                            throw2WasDouble = false
                            currentThrow = 1
                            inputValue = ""
                            multiplier = 1
                            return
                        } else {
                            winner = player1
                            showWinDialog = true
                            return
                        }
                    }
                } else {
                    val newScore = player2.score - throwValue
                    player2 = player2.copy(score = newScore)

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
                            currentThrow = 1
                            inputValue = ""
                            multiplier = 1
                            return
                        } else {
                            winner = player2
                            showWinDialog = true
                            return
                        }
                    }
                }

                currentThrow = 3
            }
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
                        bustMessage = message
                        showBustDialog = true
                        player1 = player1.copy(score = player1.score + total)
                    } else {
                        val newDartsThrown = player1.dartsThrown + 3

                        player1 = player1.copy(
                            lastThrow = total,
                            roundsPlayed = player1.roundsPlayed + 1,
                            dartsThrown = newDartsThrown,
                            average = recalculateAverage(player1.copy(score = newScore, dartsThrown = newDartsThrown))
                        )

                        if (newScore == 0) {
                            winner = player1
                            showWinDialog = true
                        }
                    }
                } else {
                    val newScore = player2.score - throwValue
                    player2 = player2.copy(score = newScore)

                    if (throwValue > 0 && (!doubleInEnabled || throw1WasDouble || throw2WasDouble || isDouble)) {
                        player2 = player2.copy(hasScored = true)
                    }

                    val (isBust, message) = checkBust(newScore, 0, throw3WasDouble, throwValue)

                    if (isBust) {
                        bustMessage = message
                        showBustDialog = true
                        player2 = player2.copy(score = player2.score + total)
                    } else {
                        val newDartsThrown = player2.dartsThrown + 3

                        player2 = player2.copy(
                            lastThrow = total,
                            roundsPlayed = player2.roundsPlayed + 1,
                            dartsThrown = newDartsThrown,
                            average = recalculateAverage(player2.copy(score = newScore, dartsThrown = newDartsThrown))
                        )

                        if (newScore == 0) {
                            winner = player2
                            showWinDialog = true
                        }
                    }
                }

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

    fun undoLastThrow() {
        when (currentThrow) {
            1 -> {
                if (throw1 == null && throw2 == null && throw3 == null) {
                    currentPlayer = if (currentPlayer == 1) 2 else 1

                    if (currentPlayer == 1 && player1.lastThrow > 0) {
                        val newScore = player1.score + player1.lastThrow
                        val newDartsThrown = maxOf(0, player1.dartsThrown - 3)
                        player1 = player1.copy(
                            score = newScore,
                            lastThrow = 0,
                            roundsPlayed = maxOf(0, player1.roundsPlayed - 1),
                            dartsThrown = newDartsThrown,
                            average = recalculateAverage(player1.copy(score = newScore, dartsThrown = newDartsThrown))
                        )
                    } else if (currentPlayer == 2 && player2.lastThrow > 0) {
                        val newScore = player2.score + player2.lastThrow
                        val newDartsThrown = maxOf(0, player2.dartsThrown - 3)
                        player2 = player2.copy(
                            score = newScore,
                            lastThrow = 0,
                            roundsPlayed = maxOf(0, player2.roundsPlayed - 1),
                            dartsThrown = newDartsThrown,
                            average = recalculateAverage(player2.copy(score = newScore, dartsThrown = newDartsThrown))
                        )
                    }

                    overallRound = maxOf(1, overallRound - 1)
                }
            }
            2 -> {
                if (throw1 != null) {
                    if (currentPlayer == 1) {
                        player1 = player1.copy(score = player1.score + (throw1 ?: 0))
                    } else {
                        player2 = player2.copy(score = player2.score + (throw1 ?: 0))
                    }
                }
                throw1 = null
                throw1WasDouble = false
                currentThrow = 1
            }
            3 -> {
                if (throw2 != null) {
                    if (currentPlayer == 1) {
                        player1 = player1.copy(score = player1.score + (throw2 ?: 0))
                    } else {
                        player2 = player2.copy(score = player2.score + (throw2 ?: 0))
                    }
                }
                throw2 = null
                throw2WasDouble = false
                currentThrow = 2
            }
        }
        inputValue = ""
        multiplier = 1
    }

    // Bust Dialog
    if (showBustDialog) {
        AlertDialog(
            onDismissRequest = {
                showBustDialog = false
                throw1 = null
                throw2 = null
                throw3 = null
                throw1WasDouble = false
                throw2WasDouble = false
                throw3WasDouble = false
                currentThrow = 1
                inputValue = ""
                multiplier = 1
            },
            title = { Text("BUST!", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text(bustMessage, color = Color.White) },
            confirmButton = {
                Button(
                    onClick = {
                        showBustDialog = false
                        throw1 = null
                        throw2 = null
                        throw3 = null
                        throw1WasDouble = false
                        throw2WasDouble = false
                        throw3WasDouble = false
                        currentThrow = 1
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

    // Win Dialog
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
                                            highestScore = player1.lastThrow,
                                            dartsThrown = player1.dartsThrown,
                                            roundsPlayed = player1.roundsPlayed,
                                            finalScore = player1.score
                                        )

                                        val player2Stats = GameStatsEntity(
                                            gameId = 0,
                                            playerId = p2.playerId,
                                            average = player2.average,
                                            highestScore = player2.lastThrow,
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
                                            highestScore = player1.lastThrow,
                                            dartsThrown = player1.dartsThrown,
                                            roundsPlayed = player1.roundsPlayed,
                                            finalScore = player1.score
                                        )

                                        val player2Stats = GameStatsEntity(
                                            gameId = 0,
                                            playerId = p2.playerId,
                                            average = player2.average,
                                            highestScore = player2.lastThrow,
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

                                firstPlayer = if (firstPlayer == 1) 2 else 1
                                currentPlayer = firstPlayer

                                player1 = Player(player1Name)
                                player2 = Player(player2Name)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2A2A2A)) // MØRK GRÅ BAKGRUNN
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Back button
        IconButton(
            onClick = { navController.popBackStack() },
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerCard(
                player = player1,
                isActive = currentPlayer == 1,
                backgroundColor = Color(0xFF505050), // ⬅️ ALLTID MØRK GRÅ
                modifier = Modifier.weight(1f),
                checkout = calculateCheckout(player1.score),
                roundNumber = overallRound,
                height = playerCardHeight,
                fontSize = playerCardFontSize
            )

            PlayerCard(
                player = player2,
                isActive = currentPlayer == 2,
                backgroundColor = Color(0xFF505050), // ⬅️ ALLTID MØRK GRÅ
                modifier = Modifier.weight(1f),
                checkout = calculateCheckout(player2.score),
                roundNumber = overallRound,
                height = playerCardHeight,
                fontSize = playerCardFontSize
            )
        }

        // Throw buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThrowButton(
                label = "THROW 1:",
                value = throw1,
                isActive = currentThrow == 1,
                modifier = Modifier.weight(1f),
                height = throwButtonHeight,
                fontSize = throwButtonFontSize
            )
            ThrowButton(
                label = "THROW 2:",
                value = throw2,
                isActive = currentThrow == 2,
                modifier = Modifier.weight(1f),
                height = throwButtonHeight,
                fontSize = throwButtonFontSize
            )
            ThrowButton(
                label = "THROW 3:",
                value = throw3,
                isActive = currentThrow == 3,
                modifier = Modifier.weight(1f),
                height = throwButtonHeight,
                fontSize = throwButtonFontSize
            )
        }

        // Score display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(scoreDisplayHeight)
                .padding(horizontal = 32.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(25.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (inputValue.isNotEmpty()) {
                    "Score: $inputValue ${if (multiplier > 1) "×$multiplier" else ""}"
                } else {
                    "Score: "
                },
                fontSize = scoreDisplayFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Action buttons (Undo, Double, Triple) - SAMME STIL SOM NUM PAD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFD700), RoundedCornerShape(8.dp)) // GUL BAKGRUNN
                .padding(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp) // TYNN GUL LINJE
        ) {
            val undoInteraction = remember { MutableInteractionSource() }
            val isUndoPressed by undoInteraction.collectIsPressedAsState()

            Button(
                onClick = { undoLastThrow() },
                modifier = Modifier
                    .weight(1f)
                    .height(actionButtonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF505050) // MØRK GRÅ
                ),
                shape = RoundedCornerShape(6.dp),
                border = if (isUndoPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
                interactionSource = undoInteraction
            ) {
                Text(
                    "↺ UNDO",
                    fontSize = actionButtonFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            val doubleInteraction = remember { MutableInteractionSource() }
            val isDoublePressed by doubleInteraction.collectIsPressedAsState()

            Button(
                onClick = { multiplier = if (multiplier == 2) 1 else 2 },
                modifier = Modifier
                    .weight(1f)
                    .height(actionButtonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF505050) // MØRK GRÅ
                ),
                shape = RoundedCornerShape(6.dp),
                border = if (multiplier == 2 || isDoublePressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
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
                    .height(actionButtonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF505050) // MØRK GRÅ
                ),
                shape = RoundedCornerShape(6.dp),
                border = if (multiplier == 3 || isTriplePressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
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

        // Number pad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFD700), RoundedCornerShape(8.dp)) // GUL BAKGRUNN
                .padding(1.dp), // Padding gir gule linjer mellom knappene
            verticalArrangement = Arrangement.spacedBy(1.dp) // GUL LINJE mellom rader
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp) // GUL LINJE mellom knapper
            ) {
                for (i in 1..3) {
                    NumberButton(
                        number = i,
                        onClick = { inputValue += i.toString() },
                        modifier = Modifier.weight(1f),
                        height = numberButtonHeight,
                        fontSize = numberButtonFontSize
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                for (i in 4..6) {
                    NumberButton(
                        number = i,
                        onClick = { inputValue += i.toString() },
                        modifier = Modifier.weight(1f),
                        height = numberButtonHeight,
                        fontSize = numberButtonFontSize
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                for (i in 7..9) {
                    NumberButton(
                        number = i,
                        onClick = { inputValue += i.toString() },
                        modifier = Modifier.weight(1f),
                        height = numberButtonHeight,
                        fontSize = numberButtonFontSize
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        .height(numberButtonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF505050) // MØRK GRÅ
                    ),
                    shape = RoundedCornerShape(6.dp),
                    border = if (isClearPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
                    interactionSource = clearInteraction
                ) {
                    Text(
                        "CLR",
                        fontSize = numberButtonFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                NumberButton(
                    number = 0,
                    onClick = { inputValue += "0" },
                    modifier = Modifier.weight(1f),
                    height = numberButtonHeight,
                    fontSize = numberButtonFontSize
                )

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
                        .height(numberButtonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inputValue.isEmpty()) Color(0xFF4CAF50)
                        else if (isValidInput) Color(0xFF4CAF50)
                        else Color(0xFFFF0000),
                        disabledContainerColor = if (!isValidInput && inputValue.isNotEmpty()) Color(0xFFFF0000)
                        else Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    border = if (isConfirmPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
                    interactionSource = confirmInteraction
                ) {
                    Text(
                        text = if (inputValue.isEmpty()) "✓"
                        else if (isValidInput) "✓"
                        else "✗",
                        fontSize = (numberButtonFontSize.value * 1.14f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerCard(
    player: Player,
    isActive: Boolean,
    backgroundColor: Color,
    checkout: String,
    roundNumber: Int,
    currentRoundTotal: Int = 0,
    height: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(height)
            .shadow(8.dp, RoundedCornerShape((height.value * 0.09f).dp))
            .then(
                if (isActive) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color(0xFFFFD700), // GUL OUTLINE
                            cornerRadius = CornerRadius((height.value * 0.09f).dp.toPx()),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape((height.value * 0.09f).dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding((height.value * 0.055f).dp),
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
                            modifier = Modifier.padding(end = 4.dp)
                                .offset(y = (-2).dp)
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
                fontSize = fontSize,
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
                    fontSize = (fontSize.value * 0.22f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = checkout,
                    fontSize = (fontSize.value * 0.22f).sp,
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
                        fontSize = (fontSize.value * 0.28f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "ROUND\n$roundNumber",
                        fontSize = (fontSize.value * 0.28f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "DARTS\n${player.dartsThrown}",
                        fontSize = (fontSize.value * 0.28f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ThrowButton(
    label: String,
    value: Int?,
    isActive: Boolean = false,
    height: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val cornerRadius = height * 0.5f

    Button(
        onClick = { },
        modifier = modifier
            .height(height)
            .then(
                if (isActive) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color(0xFFFFD700),
                            cornerRadius = CornerRadius(cornerRadius.toPx()),
                            style = Stroke(width = 1.5.dp.toPx()) // ⬅️ ENDRET fra 1.dp til 1.5.dp
                        )
                    }
                } else Modifier
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6B6B6B)
        ),
        shape = RoundedCornerShape(cornerRadius),
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

@Composable
fun NumberButton(
    number: Int,
    onClick: () -> Unit,
    height: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF505050) // MØRK GRÅ
        ),
        shape = RoundedCornerShape(6.dp), // RUNDE HJØRNER
        border = if (isPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
        interactionSource = interaction
    ) {
        Text(
            text = number.toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}