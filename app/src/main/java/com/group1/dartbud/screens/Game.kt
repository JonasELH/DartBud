package com.group1.dartbud.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    doubleOutEnabled: Boolean = true
) {
    var player1 by remember { mutableStateOf(Player("PLAYER 1")) }
    var player2 by remember { mutableStateOf(Player("PLAYER 2")) }
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

    // Komplett checkout-tabell basert på PDF
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

        // Bust hvis score går under 0
        if (newScore < 0) {
            return Pair(true, "BUST! Score under 0")
        }

        // Bust hvis score blir 1 (kan ikke checke ut)
        if (newScore == 1) {
            return Pair(true, "BUST! Cannot finish on 1")
        }

        // Bust hvis score blir 0 men double out er påkrevd og siste dart ikke var double
        // Bull (50) teller som double
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

        // Double In sjekk - første scoring må være en double
        val activePlayer = if (currentPlayer == 1) player1 else player2
        if (doubleInEnabled && !activePlayer.hasScored && !isDouble && throwValue > 0) {
            bustMessage = "BUST! Must start with a double"
            showBustDialog = true
            inputValue = ""
            multiplier = 1
            return
        }

        when (currentThrow) {
            1 -> {
                throw1 = throwValue
                throw1WasDouble = isDouble

                // Oppdater score etter første kast
                if (currentPlayer == 1) {
                    if (throwValue > 0) {
                        player1 = player1.copy(hasScored = true)
                    }
                    val newScore = player1.score - throwValue
                    player1 = player1.copy(score = newScore)

                    // Sjekk for vinner eller bust etter første kast
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
                    if (throwValue > 0) {
                        player2 = player2.copy(hasScored = true)
                    }
                    val newScore = player2.score - throwValue
                    player2 = player2.copy(score = newScore)

                    // Sjekk for vinner eller bust etter første kast
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

                // Oppdater score etter andre kast
                if (currentPlayer == 1) {
                    val newScore = player1.score - throwValue
                    player1 = player1.copy(score = newScore)

                    // Sjekk for vinner eller bust etter andre kast
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

                    // Sjekk for vinner eller bust etter andre kast
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
                    // Oppdater score etter tredje kast
                    val newScore = player1.score - throwValue
                    player1 = player1.copy(score = newScore)

                    val (isBust, message) = checkBust(newScore, 0, throw3WasDouble, throwValue)

                    if (isBust) {
                        bustMessage = message
                        showBustDialog = true
                        // Tilbakestill score til før runden
                        player1 = player1.copy(score = player1.score + total)
                    } else {
                        val newDartsThrown = player1.dartsThrown + 3

                        player1 = player1.copy(
                            lastThrow = total,
                            roundsPlayed = player1.roundsPlayed + 1,
                            dartsThrown = newDartsThrown,
                            average = recalculateAverage(player1.copy(score = newScore, dartsThrown = newDartsThrown))
                        )

                        // Sjekk for vinner
                        if (newScore == 0) {
                            winner = player1
                            showWinDialog = true
                        }
                    }
                } else {
                    // Oppdater score etter tredje kast
                    val newScore = player2.score - throwValue
                    player2 = player2.copy(score = newScore)

                    val (isBust, message) = checkBust(newScore, 0, throw3WasDouble, throwValue)

                    if (isBust) {
                        bustMessage = message
                        showBustDialog = true
                        // Tilbakestill score til før runden
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

                // Bytt spiller og reset
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
                    // Gå tilbake til forrige spiller
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
                // Angre kast 1 - legg tilbake scoren
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
                // Angre kast 2 - legg tilbake scoren
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
                // Reset throws
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
            title = { Text("BUST!", fontWeight = FontWeight.Bold) },
            text = { Text(bustMessage) },
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
                    }
                ) {
                    Text("OK")
                }
            }
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
                        textAlign = TextAlign.Center
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Average: ${String.format("%.1f", winner!!.average)}",
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Rounds: ${winner!!.roundsPlayed}",
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Darts: ${winner!!.dartsThrown}",
                        textAlign = TextAlign.Center
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
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("New Game")
                        }
                        Button(
                            onClick = {
                                // Rematch - bytt hvem som starter
                                firstPlayer = if (firstPlayer == 1) 2 else 1
                                currentPlayer = firstPlayer

                                // Reset game
                                player1 = Player("PLAYER 1")
                                player2 = Player("PLAYER 2")
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rematch")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showWinDialog = false
                                winner = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Continue")
                        }

                        Button(
                            onClick = {
                                // Close app
                                android.os.Process.killProcess(android.os.Process.myPid())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close App")
                        }
                    }
                }
            },
            dismissButton = { }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF1A1A1A)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerCard(
                player = player1,
                isActive = currentPlayer == 1,
                backgroundColor = if (currentPlayer == 1) Color(0xFFFC1E69) else Color(0xFF1A1A1A),
                modifier = Modifier.weight(1f),
                checkout = calculateCheckout(player1.score),
                roundNumber = overallRound
            )

            PlayerCard(
                player = player2,
                isActive = currentPlayer == 2,
                backgroundColor = if (currentPlayer == 2) Color(0xFFFC1E69) else Color(0xFF1A1A1A),
                modifier = Modifier.weight(1f),
                checkout = calculateCheckout(player2.score),
                roundNumber = overallRound
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThrowButton(
                label = "THROW 1:",
                value = throw1,
                isActive = currentThrow == 1,
                modifier = Modifier.weight(1f)
            )
            ThrowButton(
                label = "THROW 2:",
                value = throw2,
                isActive = currentThrow == 2,
                modifier = Modifier.weight(1f)
            )
            ThrowButton(
                label = "THROW 3:",
                value = throw3,
                isActive = currentThrow == 3,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(25.dp))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (inputValue.isNotEmpty()) {
                    "Score: $inputValue ${if (multiplier > 1) "×$multiplier" else ""}"
                } else {
                    "Score: "
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { undoLastThrow() },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFAA4C9E)
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    "↺ UNDO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = { multiplier = if (multiplier == 2) 1 else 2 },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .then(
                        if (multiplier == 2) {
                            Modifier.drawBehind {
                                drawRoundRect(
                                    color = Color(0xFFFC1E69),
                                    cornerRadius = CornerRadius(30.dp.toPx()),
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                        } else Modifier
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFDB735)
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    "Double",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = { multiplier = if (multiplier == 3) 1 else 3 },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .then(
                        if (multiplier == 3) {
                            Modifier.drawBehind {
                                drawRoundRect(
                                    color = Color(0xFFFC1E69),
                                    cornerRadius = CornerRadius(30.dp.toPx()),
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                        } else Modifier
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCDDC39)
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    "Triple",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..3) {
                    NumberButton(
                        number = i,
                        onClick = { inputValue += i.toString() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 4..6) {
                    NumberButton(
                        number = i,
                        onClick = { inputValue += i.toString() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 7..9) {
                    NumberButton(
                        number = i,
                        onClick = { inputValue += i.toString() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        inputValue = ""
                        multiplier = 1
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF808080)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "CLR",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                NumberButton(
                    number = 0,
                    onClick = { inputValue += "0" },
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (isValidInput) {
                            confirmThrow()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inputValue.isEmpty()) Color(0xFF4CAF50)
                        else if (isValidInput) Color(0xFF4CAF50)
                        else Color(0xFFFF0000),
                        disabledContainerColor = if (!isValidInput && inputValue.isNotEmpty()) Color(0xFFFF0000)
                        else Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (inputValue.isEmpty()) "✓"
                        else if (isValidInput) "✓"
                        else "✗",
                        fontSize = 32.sp,
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(220.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .offset(y = (-4).dp)
                        )
                    }
                    Text(
                        text = player.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = "${player.score}",
                fontSize = 56.sp,
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = checkout,
                    fontSize = 11.sp,
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "ROUND\n$roundNumber",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "DARTS\n${player.dartsThrown}",
                        fontSize = 10.sp,
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
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { },
        modifier = modifier
            .height(50.dp)
            .then(
                if (isActive) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = Color(0xFFFC1E69),
                            cornerRadius = CornerRadius(25.dp.toPx()),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                } else Modifier
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6B6B6B)
        ),
        shape = RoundedCornerShape(25.dp)
    ) {
        Text(
            text = "$label ${value ?: ""}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun NumberButton(
    number: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(70.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFC1E69)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = number.toString(),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}