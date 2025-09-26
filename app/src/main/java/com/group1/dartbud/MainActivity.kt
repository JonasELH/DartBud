package com.group1.dartbud


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
import com.group1.dartbud.ui.theme.DartBudTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.group1.dartbud.screens.GameSettingsScreen
import com.group1.dartbud.screens.MainMenuScreen
import com.group1.dartbud.screens.RulesScreen
import com.group1.dartbud.screens.LoginScreen



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
                    GameSettingsScreen(navController = navController)
            }

        }
    }
}
