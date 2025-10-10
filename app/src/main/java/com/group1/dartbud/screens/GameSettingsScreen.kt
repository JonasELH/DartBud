package com.group1.dartbud.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group1.dartbud.ui.theme.DarkSecondary
import com.group1.dartbud.ui.theme.LightPrimary
import com.group1.dartbud.ui.theme.LightSecondary
import com.group1.dartbud.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSettingsScreen(
    navController: NavController,
    viewModel: PlayerViewModel = viewModel()
) {

    // Values for same screen size dp config
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // Calculate responsive sizes based on screen dimension
    val buttonHeight = (screenHeight * 0.09f).coerceIn(50.dp, 80.dp)
    val buttonCornerRadius = buttonHeight / 2
    val buttonStrokeWidth = (buttonHeight * 0.14f).coerceIn(6.dp, 12.dp)
    val buttonFontSize = (buttonHeight.value * 0.28f).coerceIn(16f, 22f).sp

    var player1 by remember { mutableStateOf<String?>(null) }
    var player2 by remember { mutableStateOf<String?>(null) }
    var doubleIn by remember { mutableStateOf(false) }
    var doubleOut by remember { mutableStateOf(true) }
    var expandedPlayer1 by remember { mutableStateOf(false) }
    var expandedPlayer2 by remember { mutableStateOf(false) }

    // Hent spillere fra database
    val players by viewModel.players.collectAsState()
    val playerNames = players.map { it.username }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { expandedPlayer1 = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(30.dp),
                        enabled = playerNames.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SELECT PLAYER 1",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expandedPlayer1,
                        onDismissRequest = { expandedPlayer1 = false }
                    ) {
                        Text(
                            "Select Player 1",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        HorizontalDivider()
                        playerNames.forEach { playerName ->
                            DropdownMenuItem(
                                text = { Text(playerName) },
                                onClick = {
                                    player1 = playerName
                                    expandedPlayer1 = false
                                }
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { expandedPlayer2 = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(30.dp),
                        enabled = playerNames.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SELECT PLAYER 2",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expandedPlayer2,
                        onDismissRequest = { expandedPlayer2 = false }
                    ) {
                        Text(
                            "Select Player 2",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        HorizontalDivider()
                        playerNames.forEach { playerName ->
                            DropdownMenuItem(
                                text = { Text(playerName) },
                                onClick = {
                                    player2 = playerName
                                    expandedPlayer2 = false
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    navController.navigate("managePlayers")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFC1E69)
                )
            ) {
                Text(
                    "MANAGE PLAYERS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "🎯 IN GAME SETTINGS:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            "DOUBLE IN:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { doubleIn = !doubleIn },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = if (doubleIn) Color(0xFFFC1E69) else Color.Gray
                        ),
                        border = BorderStroke(2.dp, Color(0xFFFC1E69)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (doubleIn) "✓" else "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            "DOUBLE OUT:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { doubleOut = !doubleOut },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = if (doubleOut) Color(0xFFFC1E69) else Color.Gray
                        ),
                        border = BorderStroke(2.dp, Color(0xFFFC1E69)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (doubleOut) "✓" else "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            // Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 25.dp)
                    ) {
                        Card(
                            modifier = Modifier.width(if (player1 != null) 140.dp else 120.dp),
                            shape = RoundedCornerShape(25.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (player1 != null) 8.dp else 2.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (player1 != null) Color(0xFFFC1E69) else Color(0xFF6B6B6B)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    player1 ?: "PLAYER 1",
                                    fontWeight = if (player1 != null) FontWeight.ExtraBold else FontWeight.Bold,
                                    fontSize = if (player1 != null) 16.sp else 12.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                        TextButton(
                            onClick = { player1 = null },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            enabled = player1 != null
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "✗",
                                    fontSize = 20.sp,
                                    color = if (player1 != null) Color.Red else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.offset(y = (-1).dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Remove Player 1",
                                    fontSize = 11.sp,
                                    color = if (player1 != null) Color.Red else Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.height(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "VS",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 25.dp)
                    ) {
                        Card(
                            modifier = Modifier.width(if (player2 != null) 140.dp else 120.dp),
                            shape = RoundedCornerShape(25.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (player2 != null) 8.dp else 2.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (player2 != null) Color(0xFFFC1E69) else Color(0xFF6B6B6B)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    player2 ?: "PLAYER 2",
                                    fontWeight = if (player2 != null) FontWeight.ExtraBold else FontWeight.Bold,
                                    fontSize = if (player2 != null) 16.sp else 12.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                        TextButton(
                            onClick = { player2 = null },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            enabled = player2 != null
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "✗",
                                    fontSize = 20.sp,
                                    color = if (player2 != null) Color.Red else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.offset(y = (-1).dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Remove Player 2",
                                    fontSize = 11.sp,
                                    color = if (player2 != null) Color.Red else Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            // Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val p1Name = player1 ?: "PLAYER 1"
                    val p2Name = player2 ?: "PLAYER 2"
                    navController.navigate("game/$doubleIn/$doubleOut/$p1Name/$p2Name")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
                    .drawBehind {
                        drawRoundRect(
                            color = Color.DarkGray,
                            cornerRadius = CornerRadius(buttonCornerRadius.toPx()),
                            style = Stroke(width = buttonStrokeWidth.toPx())
                        )
                    },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(buttonCornerRadius)
            ) {
                Text(
                    "START GAME!!!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = buttonFontSize,
                    color = Color.White,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}