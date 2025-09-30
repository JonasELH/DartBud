package com.group1.dartbud.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
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

            // Player Selection Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Select Player Button
                Button(
                    onClick = { /* TODO: Show player selection */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "SELECT PLAYER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Create Player Button
                Button(
                    onClick = { /* TODO: Navigate to create player */ },
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
            }

            // Game Settings Section
            Column(
                horizontalAlignment = Alignment.Start,
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

                    Button(
                        onClick = { doubleIn = !doubleIn },
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (doubleIn) "✓" else "✗",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
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

                    Button(
                        onClick = { doubleOut = !doubleOut },
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (doubleOut) "✓" else "✗",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // VS Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✗", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { },
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text(
                            player1 ?: "PLAYER 1",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // VS Text
                Text(
                    "VS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Player 2
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✗", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { },

                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text(
                            player2 ?: "PLAYER 2",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    navController.navigate("game")
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
}
