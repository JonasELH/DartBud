package com.group1.dartbud

import com.group1.dartbud.screens.ManagePlayersScreen
import com.group1.dartbud.screens.CreatePlayerScreen
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DartBudTheme {
                DartBudApp()
            }
        }
    }
}

@Composable
fun DartBudApp() {
    val navController = rememberNavController()
    // Opprett ViewModel én gang på topp-nivå så den deles mellom skjermer
    val playerViewModel: PlayerViewModel = viewModel()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(navController = navController)
            }
            composable("main_menu") {
                MainMenuScreen(navController = navController)
            }
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

            composable(
                "createPlayer",
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
                CreatePlayerScreen(navController = navController)
            }

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
                val doubleIn = backStackEntry.arguments?.getBoolean("doubleIn") ?: false
                val doubleOut = backStackEntry.arguments?.getBoolean("doubleOut") ?: true
                val player1Name = backStackEntry.arguments?.getString("player1Name") ?: "PLAYER 1"
                val player2Name = backStackEntry.arguments?.getString("player2Name") ?: "PLAYER 2"
                GameScreen(
                    navController = navController,
                    doubleInEnabled = doubleIn,
                    doubleOutEnabled = doubleOut,
                    player1Name = player1Name,
                    player2Name = player2Name
                )
            }
        }
    }
}