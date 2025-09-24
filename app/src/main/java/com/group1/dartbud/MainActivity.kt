package com.group1.dartbud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.group1.dartbud.screens.MainMenuScreen
import com.group1.dartbud.screens.RulesScreen
import com.group1.dartbud.screens.LoginScreen
import com.group1.dartbud.ui.theme.DartBudTheme

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
            composable("rules") {
                RulesScreen(navController = navController)
            }
        }
    }
}
