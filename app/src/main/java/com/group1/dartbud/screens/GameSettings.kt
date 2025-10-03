package com.group1.dartbud.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSettingsScreen(navController: NavController) {
    // State variables
    var player1 by remember { mutableStateOf<String?>(null) }
    var player2 by remember { mutableStateOf<String?>(null) }
    var doubleIn by remember { mutableStateOf(false) }
    var doubleOut by remember { mutableStateOf(true) }

    // Liste med lagrede spillere - bruk rememberSaveable med custom saver fordi Database skal vi implementere senere
    var savedPlayers by rememberSaveable(
        stateSaver = listSaver<List<String>, String>(
            save = { stateList -> stateList.toList() },
            restore = { savedList -> savedList.toMutableList() }
        )
    ) { mutableStateOf(listOf<String>()) }

    // Dropdown states
    var expandedPlayer1 by remember { mutableStateOf(false) }
    var expandedPlayer2 by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Setup", fontWeight = FontWeight.Bold)  },
                navigationIcon ={
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Player Selection Dropdowns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Select Player 1 Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { expandedPlayer1 = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(30.dp),
                        enabled = savedPlayers.isNotEmpty()
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
                        savedPlayers.forEach { playerName ->
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

                // Select Player 2 Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { expandedPlayer2 = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(30.dp),
                        enabled = savedPlayers.isNotEmpty()
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
                        savedPlayers.forEach { playerName ->
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

            // Player Management Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Create Player Button
                Button(
                    onClick = {
                        navController.navigate("createPlayer")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "CREATE PLAYER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Manage Players Button
                Button(
                    onClick = {
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("savedPlayersList", savedPlayers.joinToString(","))
                        navController.navigate("managePlayers")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text(
                        "MANAGE PLAYERS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Game Settings Section
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

                // Double In Setting
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
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(25.dp),
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
                            fontSize = 32.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Double Out Setting
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
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(25.dp),
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
                            fontSize = 32.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // VS Section
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
                    // Player 1
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

                        // Remove button under player name
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

                    // VS Text
                    Box(
                        modifier = Modifier
                            .height(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "VS",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Player 2
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

                        // Remove button under player name
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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val p1Name = player1 ?: "PLAYER 1"
                    val p2Name = player2 ?: "PLAYER 2"
                    navController.navigate("game/$doubleIn/$doubleOut/$p1Name/$p2Name")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp)
                    .drawBehind {
                        // Border
                        drawRoundRect(
                            color = Color(0x80FC1E69),
                            cornerRadius = CornerRadius(35.dp.toPx()),
                            style = Stroke(width = 10.dp.toPx())
                        )
                    },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(35.dp)
            ) {
                Text(
                    "START GAME!!!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
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

    // Håndter navigasjon tilbake fra CreatePlayerScreen
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow<String?>("newPlayer", null)
            ?.collect { newPlayerName ->
                if (newPlayerName != null) {
                    // Sjekk om spilleren allerede finnes
                    if (newPlayerName !in savedPlayers) {
                        savedPlayers = savedPlayers + newPlayerName
                    }
                    // Clear state etter å ha lagt til
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("newPlayer", null as String?)
                }
            }
    }

    // Håndter sletting av spillere fra ManagePlayersScreen
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow<String?>("deletePlayer", null)
            ?.collect { playerToDelete ->
                if (playerToDelete != null) {
                    savedPlayers = savedPlayers.filter { it != playerToDelete }
                    // Fjern spilleren fra player1/player2 hvis valgt
                    if (player1 == playerToDelete) player1 = null
                    if (player2 == playerToDelete) player2 = null
                    // Clear state
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("deletePlayer", null as String?)
                }
            }
    }
}