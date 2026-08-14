package com.group1.dartbud

import androidx.compose.runtime.LaunchedEffect
import com.group1.dartbud.screens.ManagePlayersScreen
import com.group1.dartbud.screens.GameHistoryScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import com.group1.dartbud.ui.theme.DartBudTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.group1.dartbud.screens.GameSettingsScreen
import com.group1.dartbud.screens.MainMenuScreen
import com.group1.dartbud.screens.RulesScreen
import com.group1.dartbud.screens.LoginScreen
import com.group1.dartbud.screens.GameScreen
import com.group1.dartbud.viewmodel.PlayerViewModel
import com.group1.dartbud.viewmodel.GameViewModel
import com.group1.dartbud.viewmodel.AuthViewModel

// Appens eneste Activity (single-activity-app). All navigasjon skjer internt
// via Compose Navigation (NavHost) i DartBudApp, ikke via egne Activities.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lar appen tegne bak system-bars (status-/navigasjonslinjen) for
        // et mer moderne, kant-til-kant utseende.
        enableEdgeToEdge()
        setContent {
            DartBudTheme {
                DartBudApp()
            }
        }
    }
}

// Rot-composable som setter opp navigasjon og de delte ViewModelene.
@Composable
fun DartBudApp() {
    val navController = rememberNavController()
    // ViewModelene opprettes her, på toppnivå, og sendes ned til hver skjerm
    // som trenger dem. Siden de er scopet til DartBudApp (som lever så lenge
    // Activity-en gjør), overlever de navigasjon mellom skjermer - f.eks.
    // beholder gameViewModel spilltilstand når man går fra game_settings til
    // game og videre til game_history.
    val playerViewModel: PlayerViewModel = viewModel()
    val gameViewModel: GameViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    // Holder gameViewModel oppdatert med hvilken bruker som er innlogget,
    // slik at spilldata lagres/hentes for riktig Google-bruker i Firestore.
    LaunchedEffect(Unit) {
        authViewModel.googleUserId.collect { userId ->
            gameViewModel.setGoogleUserId(userId)
        }
    }

    Scaffold { innerPadding ->
        // Navigasjonsgrafen for hele appen. Alle ruter deler samme
        // navController og de samme ViewModelene definert over.
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Innloggingsskjerm (Google Sign-In). Første skjerm brukeren ser.
            composable("login") {
                LoginScreen(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
            // Hovedmeny etter innlogging: herfra navigerer man videre til
            // regler, spilloppsett, historikk og spilleradministrasjon.
            composable("main_menu") {
                MainMenuScreen(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
            // Regler for 501-spillet. Egne enter/exit-transisjoner (glid inn/ut
            // fra høyre) brukes på denne og rutene under for en "drill down"-følelse.
            composable(
                "rules",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { 1000 },
                        animationSpec = tween(400)
                    ) + fadeIn(animationSpec = tween(400))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { 1000 },
                        animationSpec = tween(400)
                    ) + fadeOut(animationSpec = tween(400))
                }
            ) {
                RulesScreen(navController = navController)
            }

            // Skjerm for å sette opp en ny kamp: double in/out og valg av
            // spillere (bruker playerViewModel for spillerlisten).
            composable(
                "game_settings",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { 1000 },
                        animationSpec = tween(200)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { 1000 },
                        animationSpec = tween(200)
                    ) + fadeOut(animationSpec = tween(200))
                }
            ) {
                GameSettingsScreen(
                    navController = navController,
                    viewModel = playerViewModel
                )
            }

            // Historikk over tidligere spilte kamper, hentet via gameViewModel
            // (og playerViewModel for å slå opp spillernavn).
            composable(
                "game_history",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { 1000 },
                        animationSpec = tween(200)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { 1000 },
                        animationSpec = tween(200)
                    ) + fadeOut(animationSpec = tween(200))
                }
            ) {
                GameHistoryScreen(
                    navController = navController,
                    gameViewModel = gameViewModel,
                    playerViewModel = playerViewModel
                )
            }

            // Administrasjon av spillere (legge til/fjerne/redigere).
            composable(
                "managePlayers",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { 1000 },
                        animationSpec = tween(200)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { 1000 },
                        animationSpec = tween(200)
                    ) + fadeOut(animationSpec = tween(200))
                }
            ) {
                ManagePlayersScreen(
                    navController = navController,
                    viewModel = playerViewModel
                )
            }

            // Selve spillskjermen. Spillinnstillingene sendes som navigasjons-
            // argumenter i ruten (ikke via ViewModel), slik at de er en del av
            // navController sin backstack og overlever f.eks. prosessgjenoppretting.
            composable(
                route = "game/{doubleIn}/{doubleOut}/{player1Name}/{player2Name}",
                arguments = listOf(
                    navArgument("doubleIn") { type = NavType.BoolType },
                    navArgument("doubleOut") { type = NavType.BoolType },
                    navArgument("player1Name") { type = NavType.StringType },
                    navArgument("player2Name") { type = NavType.StringType }
                ),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { 1000 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { 1000 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) { backStackEntry ->
                // Fallback-verdier her dekker tilfeller der argumentene av en
                // eller annen grunn mangler (bør normalt ikke skje siden alle
                // er markert som påkrevde typer over).
                val doubleIn = backStackEntry.arguments?.getBoolean("doubleIn") ?: false
                val doubleOut = backStackEntry.arguments?.getBoolean("doubleOut") ?: true
                val player1Name = backStackEntry.arguments?.getString("player1Name") ?: "PLAYER 1"
                val player2Name = backStackEntry.arguments?.getString("player2Name") ?: "PLAYER 2"
                GameScreen(
                    navController = navController,
                    doubleInEnabled = doubleIn,
                    doubleOutEnabled = doubleOut,
                    player1Name = player1Name,
                    player2Name = player2Name,
                    gameViewModel = gameViewModel,
                    playerViewModel = playerViewModel
                )
            }
        }
    }
}