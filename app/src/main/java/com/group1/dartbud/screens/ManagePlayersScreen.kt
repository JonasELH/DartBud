package com.group1.dartbud.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePlayersScreen(
    navController: NavController,
    savedPlayers: List<String>
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var playerToDelete by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<String?>(null) }
    var editedPlayerName by remember { mutableStateOf("") }
    var showEditError by remember { mutableStateOf(false) }

    // Hold liste av spillere lokalt slik at vi kan oppdatere den
    var currentPlayers by remember { mutableStateOf(savedPlayers) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Players", fontWeight = FontWeight.Bold) },
                navigationIcon = {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CREATE PLAYER knapp alltid synlig
            Button(
                onClick = { navController.navigate("createPlayer") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFC1E69)
                )
            ) {
                Text(
                    "CREATE NEW PLAYER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (currentPlayers.isEmpty()) {
                // Vis melding hvis ingen spillere
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "No players yet",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            "Create a player to get started",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Vis liste med spillere
                Text(
                    "Saved Players (${currentPlayers.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentPlayers) { playerName ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = playerName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            playerToEdit = playerName
                                            editedPlayerName = playerName
                                            showEditDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit $playerName",
                                            tint = Color(0xFFFC1E69)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            playerToDelete = playerName
                                            showDeleteDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete $playerName",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && playerToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                playerToDelete = null
            },
            title = {
                Text(
                    "Delete Player?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete \"$playerToDelete\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Send spillernavn tilbake via savedStateHandle
                        playerToDelete?.let { name ->
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("deletePlayer", name)
                        }
                        showDeleteDialog = false
                        playerToDelete = null
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        playerToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit dialog
    if (showEditDialog && playerToEdit != null) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                playerToEdit = null
                editedPlayerName = ""
                showEditError = false
            },
            title = {
                Text(
                    "Edit Player Name",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editedPlayerName,
                        onValueChange = {
                            editedPlayerName = it
                            showEditError = false
                        },
                        label = { Text("Player Name") },
                        singleLine = true,
                        isError = showEditError,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp)
                    )

                    if (showEditError) {
                        Text(
                            "Please enter a valid name",
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = editedPlayerName.trim()
                        if (trimmedName.isBlank()) {
                            showEditError = true
                        } else if (trimmedName != playerToEdit && trimmedName in currentPlayers) {
                            showEditError = true
                        } else {
                            // Oppdater spillerlisten
                            currentPlayers = currentPlayers.map {
                                if (it == playerToEdit) trimmedName else it
                            }

                            // Send oppdatert liste tilbake til GameSettings
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("savedPlayersList", currentPlayers.joinToString(","))

                            // Send edit-info tilbake
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("editedPlayer", "$playerToEdit|$trimmedName")

                            showEditDialog = false
                            playerToEdit = null
                            editedPlayerName = ""
                            showEditError = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFC1E69)
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        playerToEdit = null
                        editedPlayerName = ""
                        showEditError = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Lytt etter nye spillere fra CreatePlayerScreen
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow<String?>("newPlayer", null)
            ?.collect { newPlayerName ->
                if (newPlayerName != null && newPlayerName !in currentPlayers) {
                    currentPlayers = currentPlayers + newPlayerName
                    // Oppdater også savedPlayersList som GameSettings bruker
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("savedPlayersList", currentPlayers.joinToString(","))
                    // Send også newPlayer videre til GameSettings
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("newPlayer", newPlayerName)
                    // Clear state
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("newPlayer", null as String?)
                }
            }
    }
}