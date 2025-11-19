package com.group1.dartbud.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group1.dartbud.data.GameEntity
import com.group1.dartbud.data.GameStatsEntity
import com.group1.dartbud.viewmodel.GameViewModel
import com.group1.dartbud.viewmodel.PlayerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHistoryScreen(
    navController: NavController,
    gameViewModel: GameViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    val games by gameViewModel.games.collectAsState()
    val players by playerViewModel.players.collectAsState()

    println("📊 DEBUG GameHistory: games.size = ${games.size}")
    println("📊 DEBUG GameHistory: players.size = ${players.size}")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game History", fontWeight = FontWeight.Bold) },
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
                .background(Color(0xFF737171))
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (games.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "No games played yet",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            "Play a game to see history here!",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Text(
                    "Total Games: ${games.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(games) { game ->
                        GameHistoryCard(
                            game = game,
                            players = players,
                            gameViewModel = gameViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameHistoryCard(
    game: GameEntity,
    players: List<com.group1.dartbud.data.PlayerEntity>,
    gameViewModel: GameViewModel
) {
    var statsLoaded by remember { mutableStateOf(false) }
    var player1Stats by remember { mutableStateOf<GameStatsEntity?>(null) }
    var player2Stats by remember { mutableStateOf<GameStatsEntity?>(null) }

    // Load stats when card is created
    LaunchedEffect(game.gameId) {
        val stats = gameViewModel.getStatsByGame(game.gameId)
        player1Stats = stats.find { it.playerId == game.player1Id }
        player2Stats = stats.find { it.playerId == game.player2Id }
        statsLoaded = true
    }

    val player1 = players.find { it.playerId == game.player1Id }
    val player2 = players.find { it.playerId == game.player2Id }
    val winner = players.find { it.playerId == game.winnerId }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(game.datePlayed))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateString,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (game.doubleIn) {
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Double In",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                    if (game.doubleOut) {
                        Surface(
                            color = Color(0xFFFCE4EC),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Double Out",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color(0xFFC2185B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Winner announcement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "🏆 ${winner?.username ?: "Unknown"} won!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFC1E69)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Player stats
            if (statsLoaded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Player 1
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            player1?.username ?: "Unknown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        player1Stats?.let { stats ->
                            StatsColumn(stats)
                        }
                    }

                    VerticalDivider(modifier = Modifier.height(100.dp))

                    // Player 2
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            player2?.username ?: "Unknown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        player2Stats?.let { stats ->
                            StatsColumn(stats)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFFC1E69)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsColumn(stats: GameStatsEntity) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StatRow("Avg", String.format("%.1f", stats.average))
        StatRow("High", "${stats.highestScore}")
        StatRow("Darts", "${stats.dartsThrown}")
        StatRow("Rounds", "${stats.roundsPlayed}")
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$label:",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}