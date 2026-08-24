package com.group1.dartbud.screens


import androidx.compose.runtime.*
import com.group1.dartbud.viewmodel.PlayerViewModel
import com.group1.dartbud.viewmodel.GameViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.group1.dartbud.R
import com.group1.dartbud.viewmodel.AuthViewModel

// Hovedmenyen: enkel navigasjonshub til Play Game, Game History og Rules, pluss logg ut.
@Composable
fun MainMenuScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    gameViewModel: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Bildet som fyller hele skjermen
        Image(
            painter = painterResource(id = R.drawable.mainmenuu),
            contentDescription = "DartBud Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )


        // Logout-knapp
        IconButton(
            onClick = {
                authViewModel.signOut(context)
                // Nullstill google-bruker-id i begge viewmodels slik at de faller tilbake
                // til å vise kun lokale (gjeste-)profiler/kamper etter utlogging
                playerViewModel.setGoogleUserId(null)
                gameViewModel.setGoogleUserId(null)
                // popUpTo(0) tømmer HELE back-stacken - forhindrer at "tilbake" fra login
                // kan ta brukeren inn igjen i en økt de nettopp logget ut av
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(48.dp)
                .shadow(8.dp, CircleShape)
                .background(Color(0xCC000000), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Logg ut",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(180f)
            )
        }

        // Knapper over bildet nederst
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Play Game knapp
            val playGameInteraction = remember { MutableInteractionSource() }
            val isPlayGamePressed by playGameInteraction.collectIsPressedAsState()

            Button(
                onClick = { navController.navigate("game_settings") },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x66000000)
                ),
                shape = RoundedCornerShape(28.dp),
                border = if (isPlayGamePressed) {
                    BorderStroke(3.dp, Color(0xFFFFD700))
                } else null,
                interactionSource = playGameInteraction
            ) {
                Text("Play Game", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

            }

            // Game History knapp
            val gameHistoryInteraction = remember { MutableInteractionSource() }
            val isGameHistoryPressed by gameHistoryInteraction.collectIsPressedAsState()

            Button(
                onClick = { navController.navigate("game_history") },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x66000000)
                ),
                shape = RoundedCornerShape(28.dp),
                border = if (isGameHistoryPressed) {
                    BorderStroke(3.dp, Color(0xFFFFD700))
                } else null,
                interactionSource = gameHistoryInteraction
            ) {
                Text("Game History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

            }

            // Rules knapp
            val rulesInteraction = remember { MutableInteractionSource() }
            val isRulesPressed by rulesInteraction.collectIsPressedAsState()

            Button(
                onClick = { navController.navigate("rules") },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x66000000)
                ),
                shape = RoundedCornerShape(28.dp),
                border = if (isRulesPressed) {
                    BorderStroke(3.dp, Color(0xFFFFD700))
                } else null,
                interactionSource = rulesInteraction
            ) {
                Text("Rules", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

            }

            // Options knapp
            val optionsInteraction = remember { MutableInteractionSource() }
            val isOptionsPressed by optionsInteraction.collectIsPressedAsState()

            Button(
                onClick = { navController.navigate("options") },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x66000000)
                ),
                shape = RoundedCornerShape(28.dp),
                border = if (isOptionsPressed) {
                    BorderStroke(3.dp, Color(0xFFFFD700))
                } else null,
                interactionSource = optionsInteraction
            ) {
                Text("Options", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

            }
        }
    }
}