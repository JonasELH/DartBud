package com.group1.dartbud.screens

import com.group1.dartbud.viewmodel.GameViewModel
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.group1.dartbud.R
import com.group1.dartbud.viewmodel.AuthState
import com.group1.dartbud.viewmodel.AuthViewModel
import com.group1.dartbud.viewmodel.PlayerViewModel
import com.group1.dartbud.utils.NetworkUtils
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    gameViewModel: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    // ⬇️ NY STATE FOR NETWORK ERROR ⬇️
    var showNetworkError by remember { mutableStateOf(false) }

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
                val hasPrimary = playerViewModel.hasPrimaryProfile(user.uid)

                if (!hasPrimary) {
                    playerViewModel.createPrimaryProfileForGoogleUser(
                        googleUserId = user.uid,
                        displayName = user.displayName ?: "Player",
                        email = user.email ?: "",
                        photoUrl = user.photoUrl?.toString()
                    )
                }

                playerViewModel.setGoogleUserId(user.uid)
                gameViewModel.setGoogleUserId(user.uid)

                navController.navigate("main_menu") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    // ⬇️ NY NETWORK ERROR DIALOG ⬇️
    if (showNetworkError) {
        AlertDialog(
            onDismissRequest = { showNetworkError = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "📡",
                        fontSize = 28.sp
                    )
                    Text(
                        "No Internet Connection",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Google Sign-In requires an internet connection.",
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    )

                    Text(
                        "Please:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "• Connect to WiFi\n• Enable mobile data\n• Or continue as guest to play offline",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNetworkError = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFC1E69)
                    )
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xDD000000),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Bakgrunnsbilde (samme som main menu eller annet bilde)
        Image(
            painter = painterResource(id = R.drawable.mainmenuu),
            contentDescription = "Login Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Knapper nederst
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Loading indicator eller error message
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = Color(0xFFFFD700)
                )
            }

            if (authState is AuthState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF5252)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp
                    )
                }
            }

            // Google Sign-In knapp
            val googleInteraction = remember { MutableInteractionSource() }
            val isGooglePressed by googleInteraction.collectIsPressedAsState()

            Button(
                onClick = {
                    // ⬇️ SJEKK NETWORK FØRST! ⬇️
                    if (!NetworkUtils.isNetworkAvailable(context)) {
                        // Vis error dialog
                        showNetworkError = true
                        android.util.Log.w("LoginScreen", "Google Sign-In blocked: No internet connection")
                    } else {
                        // Fortsett med sign-in
                        val signInIntent = authViewModel.getGoogleSignInClient(context).signInIntent
                        launcher.launch(signInIntent)
                        android.util.Log.i("LoginScreen", "Google Sign-In started: Internet available")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x66000000)
                ),
                shape = RoundedCornerShape(28.dp),
                border = if (isGooglePressed) {
                    BorderStroke(3.dp, Color(0xFFFFD700))
                } else null,
                interactionSource = googleInteraction,
                enabled = authState !is AuthState.Loading
            ) {
                Text(
                    "Log in with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Guest knapp
            val guestInteraction = remember { MutableInteractionSource() }
            val isGuestPressed by guestInteraction.collectIsPressedAsState()

            OutlinedButton(
                onClick = {
                    // Guest mode krever IKKE internett! ✅
                    navController.navigate("main_menu") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0x66000000),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                border = if (isGuestPressed) {
                    BorderStroke(3.dp, Color(0xFFFFD700))
                } else null,
                interactionSource = guestInteraction
            ) {
                Text(
                    "Continue as Guest",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}