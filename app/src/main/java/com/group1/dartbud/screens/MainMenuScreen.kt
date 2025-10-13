package com.group1.dartbud.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group1.dartbud.R
import com.group1.dartbud.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DartBud", fontWeight = FontWeight.Bold)
                        currentUser?.let { user ->
                            Text(
                                "Logget inn som ${user.displayName ?: user.email}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        } ?: Text(
                            "Gjest-modus",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            authViewModel.signOut(context)
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logg ut"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dartlogo),
                    contentDescription = "DartBud Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f),
                    contentScale = ContentScale.Fit
                )

                Button(
                    onClick = { navController.navigate("game_settings") },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp)
                ) {
                    Text("Play Game")
                }

                Button(
                    onClick = { navController.navigate("game_history") },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp)
                ) {
                    Text("Game History")
                }

                Button(
                    onClick = { navController.navigate("rules") },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp)
                ) {
                    Text("Rules")
                }
            }
        }
    }
}