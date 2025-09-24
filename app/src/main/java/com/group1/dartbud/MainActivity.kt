package com.group1.dartbud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.group1.dartbud.ui.theme.DartBudTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DartBudTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainMenu(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainMenu(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DartBud",
                    style = MaterialTheme.typography.headlineMedium
                )

                Button(
                    onClick = { /* TODO: Navigate to game */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Play Game")
                }

                Button(
                    onClick = { /* TODO: Navigate to statistics */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Statistics")
                }

                Button(
                    onClick = { /* TODO: Navigate to settings */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Settings")
                }

                OutlinedButton(
                    onClick = { /* TODO: Show about dialog */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("About")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    DartBudTheme {
        MainMenu()
    }
}
