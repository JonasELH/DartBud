package com.group1.dartbud.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group1.dartbud.R
import com.group1.dartbud.data.PlayerEntity
import com.group1.dartbud.viewmodel.PlayerViewModel
import com.group1.dartbud.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePlayersScreen(
    navController: NavController,
    viewModel: PlayerViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
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

        // Tilbake-knapp
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tittel
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Manage Players",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp),
                textAlign = TextAlign.Center
            )

            // Create button
            val createInteraction = remember { MutableInteractionSource() }
            val isCreatePressed by createInteraction.collectIsPressedAsState()

            Button(
                onClick = {
                    newPlayerName = ""
                    showCreateError = false
                    showCreateDialog = true
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .wrapContentWidth()
                    .height(48.dp)
                    .shadow(6.dp, RoundedCornerShape(24.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x88000000)
                ),
                shape = RoundedCornerShape(28.dp),
                border = if (isCreatePressed) {
                    BorderStroke(3.dp, Color(0xFFFFD700))
                } else null,
                interactionSource = createInteraction
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color(0xFF4CAF50))) { //
                            append("➕ ")
                        }
                        withStyle(style = SpanStyle(color = Color.White)) {
                            append("CREATE NEW PLAYER")
                        }
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (allPlayers.isEmpty()) {
                // Tom state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xCC000000)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "🎯",
                                fontSize = 48.sp
                            )
                            Text(
                                "No players yet",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Create a player to get started",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                // Spillerliste
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mine Profiler seksjon - ALT i én card
                    if (userProfiles.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xCC000000)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "👤 My Profiles",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Surface(
                                            color = Color(0x44FFD700),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                "${userProfiles.size}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(
                                                    horizontal = 12.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                                    // Alle user profiles
                                    userProfiles.forEach { player ->
                                        PlayerCardInline(
                                            player = player,
                                            onEdit = {
                                                playerToEdit = player
                                                editedPlayerName = player.username
                                                showEditDialog = true
                                            },
                                            onDelete = {
                                                playerToDelete = player
                                                showDeleteDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Lokale Spillere seksjon - ALT i én card
                    if (localProfiles.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xCC000000)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Local Players",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Surface(
                                            color = Color(0x44FFD700),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                "${localProfiles.size}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(
                                                    horizontal = 12.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                                    // Alle local profiles
                                    localProfiles.forEach { player ->
                                        PlayerCardInline(
                                            player = player,
                                            onEdit = {
                                                playerToEdit = player
                                                editedPlayerName = player.username
                                                showEditDialog = true
                                            },
                                            onDelete = {
                                                playerToDelete = player
                                                showDeleteDialog = true
                                            }
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

    // Create Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newPlayerName = ""
                showCreateError = false
            },
            title = {
                Text(
                    "Create New Player",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
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
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFFFD700)
                        )
                    )
                    if (showCreateError) {
                        Text(
                            "Please enter a valid name",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = newPlayerName.trim()
                        if (trimmedName.isBlank() || allPlayers.any { it.username == trimmedName }) {
                            showCreateError = true
                        } else {
                            if (currentUser != null) {
                                viewModel.addUserProfile(
                                    googleUserId = currentUser!!.uid,
                                    username = trimmedName,
                                    email = currentUser!!.email ?: ""
                                )
                            } else {
                                viewModel.addLocalPlayer(trimmedName)
                            }
                            newPlayerName = ""
                            showCreateError = false
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
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
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xDD000000)
        )
    }

    // Delete Dialog
    if (showDeleteDialog && playerToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                playerToDelete = null
            },
            title = {
                Text(
                    "Delete Player?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${playerToDelete?.username}\"? This action cannot be undone.",
                    color = Color.White.copy(alpha = 0.9f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        playerToDelete?.let { viewModel.deletePlayer(it) }
                        showDeleteDialog = false
                        playerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
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
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xDD000000)
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
            title = {
                Text(
                    "Edit Player Name",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
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
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFFFD700)
                        )
                    )
                    if (showEditError) {
                        Text(
                            "Please enter a valid name",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = editedPlayerName.trim()
                        if (trimmedName.isBlank() ||
                            (trimmedName != playerToEdit?.username && allPlayers.any { it.username == trimmedName })) {
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
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xDD000000)
        )
    }
}

@Composable
fun PlayerCardInline(
    player: PlayerEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val editInteraction = remember { MutableInteractionSource() }
    val deleteInteraction = remember { MutableInteractionSource() }
    val isEditPressed by editInteraction.collectIsPressedAsState()
    val isDeletePressed by deleteInteraction.collectIsPressedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = player.username,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (player.isPrimaryProfile) {
                Text("⭐", fontSize = 14.sp)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        if (isEditPressed) {
                            Modifier.background(
                                Color(0x44FFD700),
                                RoundedCornerShape(18.dp)
                            )
                        } else Modifier
                    ),
                interactionSource = editInteraction
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        if (isDeletePressed) {
                            Modifier.background(
                                Color(0x44FF5252),
                                RoundedCornerShape(18.dp)
                            )
                        } else Modifier
                    ),
                interactionSource = deleteInteraction
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}