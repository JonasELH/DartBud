package com.group1.dartbud.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    var dartsThrown: Int = 0
)

@Composable
fun GameScreen(navController: NavController) {
    var player1 by remember { mutableStateOf(Player("PLAYER 1")) }
    var player2 by remember { mutableStateOf(Player("PLAYER 2")) }
    var currentPlayer by remember { mutableStateOf(1) }

    var throw1 by remember { mutableStateOf<Int?>(null) }
    var throw2 by remember { mutableStateOf<Int?>(null) }
    var throw3 by remember { mutableStateOf<Int?>(null) }
    var currentThrow by remember { mutableStateOf(1) }

    var inputValue by remember { mutableStateOf("") }
    var multiplier by remember { mutableStateOf(1) }

    val roundTotal = (throw1 ?: 0) + (throw2 ?: 0) + (throw3 ?: 0)

    fun calculateCheckout(score: Int): String {
        // Simplified checkout suggestions
        return when {
            score == 170 -> "T20 T20 Bull"
            score == 167 -> "T20 T19 Bull"
            score == 164 -> "T20 T18 Bull"
            score == 161 -> "T20 T17 Bull"
            score == 160 -> "T20 T20 D20"
            score == 158 -> "T20 T20 D19"
            score == 157 -> "T20 T19 D20"
            score in 100..156 -> "T20 T20 D${(score - 120) / 2}"
            score == 50 -> "Bull"
            score in 40..99 -> "T${score / 3} D${(score % 3) + 10}"
            score in 2..40 && score % 2 == 0 -> "D${score / 2}"
            else -> ""
        }
    }

    fun confirmThrow() {
        if (inputValue.isEmpty()) return

        val value = inputValue.toIntOrNull() ?: return
        val throwValue = value * multiplier

        when (currentThrow) {
            1 -> {
                throw1 = throwValue
                currentThrow = 2
            }
            2 -> {
                throw2 = throwValue
                currentThrow = 3
            }
            3 -> {
                throw3 = throwValue
                // Apply round total to current player
                val total = (throw1 ?: 0) + (throw2 ?: 0) + (throw3 ?: 0)
                if (currentPlayer == 1) {
                    player1 = player1.copy(
                        score = player1.score - total,
                        lastThrow = total,
                        roundsPlayed = player1.roundsPlayed + 1,
                        dartsThrown = player1.dartsThrown + 3
                    )
                } else {
                    player2 = player2.copy(
                        score = player2.score - total,
                        lastThrow = total,
                        roundsPlayed = player2.roundsPlayed + 1,
                        dartsThrown = player2.dartsThrown + 3
                    )
                }
                // Switch player
                currentPlayer = if (currentPlayer == 1) 2 else 1
                // Reset throws
                throw1 = null
                throw2 = null
                throw3 = null
                currentThrow = 1
            }
        }

        inputValue = ""
        multiplier = 1
    }

    fun undoLastThrow() {
        when (currentThrow) {
            2 -> {
                throw1 = null
                currentThrow = 1
            }
            3 -> {
                throw2 = null
                currentThrow = 2
            }
        }
        inputValue = ""
        multiplier = 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Player Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Player 1 Card
            PlayerCard(
                player = player1,
                isActive = currentPlayer == 1,
                backgroundColor = if (currentPlayer == 1) Color(0xFFFC1E69) else Color(0xFF1A1A1A),
                modifier = Modifier.weight(1f),
                checkout = calculateCheckout(player1.score)
            )

            // Player 2 Card
            PlayerCard(
                player = player2,
                isActive = currentPlayer == 2,
                backgroundColor = if (currentPlayer == 2) Color(0xFFFC1E69) else Color(0xFF1A1A1A),
                modifier = Modifier.weight(1f),
                checkout = calculateCheckout(player2.score)
            )
        }

        // Throw Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThrowButton(
                label = "THROW 1:",
                value = throw1,
                modifier = Modifier.weight(1f)
            )
            ThrowButton(
                label = "THROW 2:",
                value = throw2,
                modifier = Modifier.weight(1f)
            )
            ThrowButton(
                label = "THROW 3:",
                value = throw3,
                modifier = Modifier.weight(1f)
            )
        }

        // Round Total
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), RoundedCornerShape(25.dp))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ROUND TOTAL: $roundTotal",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Undo and Multiplier Buttons
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
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (multiplier == 2) Color(0xFFFF6B35) else Color(0xFFFFB6C1)
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    "X2",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = { multiplier = if (multiplier == 3) 1 else 3 },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (multiplier == 3) Color(0xFFFF6B35) else Color(0xFFFFB6C1)
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    "X3",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Number Pad
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1-3
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

            // Row 4-6
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

            // Row 7-9
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

            // Bottom row: CLR, 0, Confirm
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
                    onClick = { confirmThrow() },
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "✓",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Current input display (optional)
        if (inputValue.isNotEmpty() || multiplier > 1) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Input: $inputValue ${if (multiplier > 1) "×$multiplier" else ""}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isActive) "→ ${player.name}" else player.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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
                    color = Color.White
                )

                Text(
                    text = checkout,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
                        text = "ROUND\n${player.roundsPlayed}",
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
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { },
        modifier = modifier.height(50.dp),
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