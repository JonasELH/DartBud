package com.group1.dartbud.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.group1.dartbud.R
import com.group1.dartbud.viewmodel.AuthState
import com.group1.dartbud.viewmodel.AuthViewModel
import com.group1.dartbud.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    // Google Sign-In launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                authViewModel.signInWithGoogle(account)
            } catch (e: ApiException) {
                println("Google Sign-In failed: ${e.message}")
            }
        }
    }

    // Håndter innlogging og profil-opprettelse
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            scope.launch {
                // Sjekk om bruker allerede har primærprofil
                val hasPrimary = playerViewModel.hasPrimaryProfile(user.uid)

                if (!hasPrimary) {
                    // Opprett primærprofil automatisk
                    playerViewModel.createPrimaryProfileForGoogleUser(
                        googleUserId = user.uid,
                        displayName = user.displayName ?: "Player",
                        email = user.email ?: "",
                        photoUrl = user.photoUrl?.toString()
                    )
                }

                // Sett Google User ID i PlayerViewModel
                playerViewModel.setGoogleUserId(user.uid)

                // Naviger til hovedmeny
                navController.navigate("main_menu") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 40.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.dartlogo),
                contentDescription = "DartBud Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineMedium
            )

            // Loading indicator
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Error message
            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Button(
                onClick = {
                    val signInIntent = authViewModel.getGoogleSignInClient(context).signInIntent
                    launcher.launch(signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = authState !is AuthState.Loading
            ) {
                Text("Log in with Google")
            }

            OutlinedButton(
                onClick = {
                    navController.navigate("main_menu") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Continue as Guest")
            }
        }
    }
}