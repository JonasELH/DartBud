package com.group1.dartbud.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group1.dartbud.ui.theme.GameColors
import com.group1.dartbud.ui.theme.allGameColors
import com.group1.dartbud.viewmodel.ThemeViewModel

/**
 * Innstillinger for appen. Foreløpig kun fargetema for spillskjermen.
 *
 * Skjermen har med vilje ingen bakgrunnskunst som de andre menyene - her skal blikket
 * ligge på fargeprøvene, ikke konkurrere med et rosa dartbord.
 */
@Composable
fun OptionsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel = viewModel()
) {
    val selected by themeViewModel.gameColors.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 68.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Options",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Colour theme",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Changes the outlines on the game screen",
                fontSize = 12.sp,
                color = Color(0xFFAAAAAA),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            allGameColors.forEach { colors ->
                ThemeOption(
                    colors = colors,
                    isSelected = colors.id == selected.id,
                    onClick = { themeViewModel.selectTheme(colors) }
                )
            }
        }

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
                .shadow(8.dp, CircleShape)
                .background(Color(0xCC000000), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Ett temavalg. Selve raden er tegnet i temaets egne farger og på samme mørke bakgrunn
 * som spillskjermen, så det man ser her er hvordan rammene faktisk kommer til å se ut
 * i en kamp - ikke bare en fargeprikk ved siden av et navn.
 */
@Composable
private fun ThemeOption(
    colors: GameColors,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Miniatyr av et spillerkort i temaets farge. Haken settes inne i selve ruten,
        // ikke ute til høyre - da leser den som "denne ruten er valgt" i stedet for å
        // bli et løsrevet merke i motsatt ende av raden.
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 36.dp)
                .background(Color(0xFF505050), RoundedCornerShape(6.dp))
                .border(BorderStroke(2.dp, colors.outline), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text(
                    "✓",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.outline
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            colors.displayName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}
