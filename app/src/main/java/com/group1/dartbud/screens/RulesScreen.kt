package com.group1.dartbud.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Rules") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "DartBud Rules",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = """
                
                🎯 OBJECTIVE
                Score points by throwing darts at the dartboard and reduce your score from 501 to exactly 0.

                🎮 GAMEPLAY
                • Each player starts with 501 points
                • Take turns throwing 3 darts per round
                • Subtract your score from 501
                • First player to reach exactly 0 wins

                🏆 WINNING
                • Usually, you must finish with a double (outer ring) or bullseye. You get to choose this when you create a new game.
                • If you go below 0 or don't finish on a double, your turn is over and your score resets to what it was at the start of that turn. This is called a bust.

                📊 SCORING
                • Outer ring = Double points
                • Inner ring = Triple points  
                • Outer bullseye = 25 points
                • Inner bullseye = 50 points

                Good luck and have fun! 🎯
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
