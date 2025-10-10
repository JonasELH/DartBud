package com.group1.dartbud.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group1.dartbud.data.PlayerEntity
import com.group1.dartbud.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePlayersScreen(
    navController: NavController,
    viewModel: PlayerViewModel = viewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var playerToDelete by remember { mutableStateOf<PlayerEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<PlayerEntity?>(null) }
    var editedPlayerName by remember { mutableStateOf("") }
    var showEditError by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlayerName by remember { mutableStateOf("") }
    var showCreateError by remember { mutableStateOf(false) }

    // Hent spillere fra database
    val players by viewModel.players.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Players", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    newPlayerName = ""
                    showCreateError = false
                    showCreateDialog = true
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
                    "CREATE NEW PLAYER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (players.isEmpty()) {
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
                Text(
                    "Saved Players (${players.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(players) { player ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
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
                                    text = player.username,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            playerToEdit = player
                                            editedPlayerName = player.username
                                            showEditDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit ${player.username}",
                                            tint = Color(0xFFFC1E69)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            playerToDelete = player
                                            showDeleteDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete ${player.username}",
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

    // Create Player Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newPlayerName = ""
                showCreateError = false
            },
            title = { Text("Create New Player", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPlayerName,
                        onValueChange = {
                            newPlayerName = it
                            showCreateError = false
                        },
                        label = { Text("Player Name") },
                        singleLine = true,
                        isError = showCreateError,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp)
                    )
                    if (showCreateError) {
                        Text("Please enter a valid name", color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = newPlayerName.trim()
                        if (trimmedName.isBlank() || players.any { it.username == trimmedName }) {
                            showCreateError = true
                        } else {
                            viewModel.addPlayer(trimmedName)
                            newPlayerName = ""
                            showCreateError = false
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newPlayerName = ""
                        showCreateError = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog && playerToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                playerToDelete = null
            },
            title = { Text("Delete Player?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${playerToDelete?.username}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        playerToDelete?.let { viewModel.deletePlayer(it) }
                        showDeleteDialog = false
                        playerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
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

    // Edit Dialog
    if (showEditDialog && playerToEdit != null) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                playerToEdit = null
                editedPlayerName = ""
                showEditError = false
            },
            title = { Text("Edit Player Name", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Text("Please enter a valid name", color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = editedPlayerName.trim()
                        if (trimmedName.isBlank() ||
                            (trimmedName != playerToEdit?.username && players.any { it.username == trimmedName })) {
                            showEditError = true
                        } else {
                            playerToEdit?.let {
                                viewModel.updatePlayer(it.copy(username = trimmedName))
                            }
                            showEditDialog = false
                            playerToEdit = null
                            editedPlayerName = ""
                            showEditError = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC1E69))
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
}