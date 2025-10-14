package com.group1.dartbud.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group1.dartbud.R
import com.group1.dartbud.viewmodel.PlayerViewModel
import com.group1.dartbud.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSettingsScreen(
    navController: NavController,
    viewModel: PlayerViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var player1 by remember { mutableStateOf<String?>(null) }
    var player2 by remember { mutableStateOf<String?>(null) }
    var doubleIn by remember { mutableStateOf(false) }
    var doubleOut by remember { mutableStateOf(true) }
    var expandedPlayer1 by remember { mutableStateOf(false) }
    var expandedPlayer2 by remember { mutableStateOf(false) }

    val currentUser by authViewModel.currentUser.collectAsState()
    val userProfiles by viewModel.userProfiles.collectAsState()
    val localProfiles by viewModel.localProfiles.collectAsState()
    val allPlayers by viewModel.players.collectAsState()

    LaunchedEffect(currentUser) {
        viewModel.setGoogleUserId(currentUser?.uid)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Bakgrunnsbilde
        Image(
            painter = painterResource(id = R.drawable.mainmenuu),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Tilbake-knapp øverst til venstre
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
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

        // Hovedinnhold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tittel
            Spacer(modifier = Modifier.height(25.dp))
            Text(
                "Game Setup",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .shadow(4.dp)
                    .padding(bottom = 8.dp)
            )

            // Player selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Player 1
                Box(modifier = Modifier.weight(1f)) {
                    val player1Interaction = remember { MutableInteractionSource() }
                    val isPlayer1Pressed by player1Interaction.collectIsPressedAsState()

                    Button(
                        onClick = { expandedPlayer1 = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(6.dp, RoundedCornerShape(30.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x88000000)
                        ),
                        shape = RoundedCornerShape(30.dp),
                        enabled = allPlayers.isNotEmpty(),
                        border = if (isPlayer1Pressed) {
                            BorderStroke(3.dp, Color(0xFFFFD700))
                        } else null,
                        interactionSource = player1Interaction
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Person, null, Modifier.size(20.dp))
                            Text(
                                "CHOOSE PLAYER 1",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedPlayer1,
                        onDismissRequest = { expandedPlayer1 = false }
                    ) {
                        if (userProfiles.isNotEmpty()) {
                            Text(
                                "👤 My Profiles",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            userProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(profile.username)
                                            if (profile.isPrimaryProfile) {
                                                Spacer(Modifier.width(4.dp))
                                                Text("⭐", fontSize = 12.sp)
                                            }
                                        }
                                    },
                                    onClick = {
                                        player1 = profile.username
                                        expandedPlayer1 = false
                                    }
                                )
                            }
                            if (localProfiles.isNotEmpty()) {
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            }
                        }

                        if (localProfiles.isNotEmpty()) {
                            Text(
                                "🎯 Local Players",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            localProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.username) },
                                    onClick = {
                                        player1 = profile.username
                                        expandedPlayer1 = false
                                    }
                                )
                            }
                        }

                        if (userProfiles.isEmpty() && localProfiles.isEmpty()) {
                            Text(
                                "No players yet",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Player 2
                Box(modifier = Modifier.weight(1f)) {
                    val player2Interaction = remember { MutableInteractionSource() }
                    val isPlayer2Pressed by player2Interaction.collectIsPressedAsState()

                    Button(
                        onClick = { expandedPlayer2 = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(6.dp, RoundedCornerShape(30.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x88000000)
                        ),
                        shape = RoundedCornerShape(30.dp),
                        enabled = allPlayers.isNotEmpty(),
                        border = if (isPlayer2Pressed) {
                            BorderStroke(3.dp, Color(0xFFFFD700))
                        } else null,
                        interactionSource = player2Interaction
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Person, null, Modifier.size(20.dp))
                            Text(
                                "CHOOSE PLAYER 2",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedPlayer2,
                        onDismissRequest = { expandedPlayer2 = false }
                    ) {
                        if (userProfiles.isNotEmpty()) {
                            Text(
                                "👤 My Profiles",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            userProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(profile.username)
                                            if (profile.isPrimaryProfile) {
                                                Spacer(Modifier.width(4.dp))
                                                Text("⭐", fontSize = 12.sp)
                                            }
                                        }
                                    },
                                    onClick = {
                                        player2 = profile.username
                                        expandedPlayer2 = false
                                    }
                                )
                            }
                            if (localProfiles.isNotEmpty()) {
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            }
                        }

                        if (localProfiles.isNotEmpty()) {
                            Text(
                                "🎯 Local Players",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            localProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.username) },
                                    onClick = {
                                        player2 = profile.username
                                        expandedPlayer2 = false
                                    }
                                )
                            }
                        }

                        if (userProfiles.isEmpty() && localProfiles.isEmpty()) {
                            Text(
                                "No players yet",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Selected players display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (player1 != null) Color(0xCC000000) else Color(0x66000000)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (player1 != null) 8.dp else 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            player1 ?: "PLAYER 1",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        if (player1 != null) {
                            TextButton(onClick = { player1 = null }) {
                                Text("✗ Remove", fontSize = 10.sp, color = Color(0xFFFFD700))
                            }
                        }
                    }
                }

                Text(
                    "VS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (player2 != null) Color(0xCC000000) else Color(0x66000000)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (player2 != null) 8.dp else 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            player2 ?: "PLAYER 2",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        if (player2 != null) {
                            TextButton(onClick = { player2 = null }) {
                                Text("✗ Remove", fontSize = 10.sp, color = Color(0xFFFFD700))
                            }
                        }
                    }
                }
            }

            // Manage Players button
            Spacer(modifier = Modifier.height(80.dp))
            val manageInteraction = remember { MutableInteractionSource() }
            val isManagePressed by manageInteraction.collectIsPressedAsState()

            Button(
                onClick = { navController.navigate("managePlayers") },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x88000000)
                ),
                shape = RoundedCornerShape(28.dp),
                border = if (isManagePressed) {
                    BorderStroke(3.dp, Color(0xFFFFD700))
                } else null,
                interactionSource = manageInteraction
            ) {
                Text(
                    "MANAGE PLAYERS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Game Settings

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xCC000000)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "🎯 GAME SETTINGS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Double In:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = doubleIn,
                            onCheckedChange = { doubleIn = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFD700),
                                checkedTrackColor = Color(0x88FFD700)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Double Out:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = doubleOut,
                            onCheckedChange = { doubleOut = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFD700),
                                checkedTrackColor = Color(0x88FFD700)
                            )
                        )
                    }
                }
            }

            // Start Game button
            val startInteraction = remember { MutableInteractionSource() }
            val isStartPressed by startInteraction.collectIsPressedAsState()

            Button(
                onClick = {
                    val p1Name = player1 ?: "PLAYER 1"
                    val p2Name = player2 ?: "PLAYER 2"
                    navController.navigate("game/$doubleIn/$doubleOut/$p1Name/$p2Name")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .shadow(12.dp, RoundedCornerShape(35.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xCC000000)
                ),
                shape = RoundedCornerShape(35.dp),
                border = if (isStartPressed) {
                    BorderStroke(4.dp, Color(0xFFFFD700))
                } else {
                    BorderStroke(3.dp, Color.White)
                },
                interactionSource = startInteraction
            ) {
                Text(
                    "START GAME!!!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }
    }
}