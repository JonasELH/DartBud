package com.group1.dartbud.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.group1.dartbud.R

@Composable
fun LoginScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(0.dp)
        ) {
            // Image is now INSIDE the Column
            Image(
                painter = painterResource(id = R.drawable.dartlogo),
                contentDescription = "DartBud Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Fixed height instead of weight for login screen
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineMedium
            )

            Button(
                onClick = { /* Google login */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Log in with Google")
            }

            Button(
                onClick = { /* Username/password login */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Log in with username and password")
            }

            OutlinedButton(
                onClick = { navController.navigate("main_menu") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Continue as Guest (will disappear)")
            }
        }
    }
}
