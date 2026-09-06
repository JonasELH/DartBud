package com.group1.dartbud.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.group1.dartbud.R
import com.group1.dartbud.viewmodel.PlayerViewModel
import com.group1.dartbud.viewmodel.AuthViewModel

// Skjermen for å sette opp en ny kamp: velge spiller 1 og 2, og skru på/av
// double-in/double-out reglene, før man navigerer videre til selve GameScreen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSettingsScreen(
    navController: NavController,
    viewModel: PlayerViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var player1 by remember { mutableStateOf<String?>(null) }
    var player2 by remember { mutableStateOf<String?>(null) }
    var doubleIn by remember { mutableStateOf(false) }
    var doubleOut by remember { mutableStateOf(true) }
    var calculatorMode by remember { mutableStateOf(false) } // Standard: rundetotal
    var quickScores by remember { mutableStateOf(true) } // Standard pa. Kun relevant nar calculatorMode er av
    var totalLegs by remember { mutableIntStateOf(1) } // Kampformat: best av 1/3/5/7/9 legs
    var expandedPlayer1 by remember { mutableStateOf(false) } // Styrer om dropdown for spiller 1 er åpen
    var expandedPlayer2 by remember { mutableStateOf(false) }
    var showSamePlayerWarning by remember { mutableStateOf(false) }

    val currentUser by authViewModel.currentUser.collectAsState()
    val userProfiles by viewModel.userProfiles.collectAsState() // Google-innloggede profiler
    val localProfiles by viewModel.localProfiles.collectAsState() // Lokale (gjeste-)profiler uten konto
    val allPlayers by viewModel.players.collectAsState()

    // Synkroniser viewModel med gjeldende innlogget bruker, slik at "My Profiles"-listen
    // viser riktige profiler når brukeren logger inn/ut eller skjermen gjenbrukes
    LaunchedEffect(currentUser) {
        viewModel.setGoogleUserId(currentUser?.uid)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Bakgrunnsbilde
        Image(
            painter = painterResource(id = R.drawable.mainmenuu),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Tilbake-knapp øverst til venstre
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

        // Hovedinnhold. START GAME ligger sist i denne scrollen, ikke festet nederst på
        // skjermen: da må man scrolle forbi alle innstillingene for å nå den, og oppdager
        // dermed valgene (bl.a. Legs) underveis. En festet knapp gjør det lett å starte
        // kampen uten å ha sett dem i det hele tatt.
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 68dp klarerer tilbake-knappen (16dp padding + 48dp knapp) med litt margin.
                .padding(top = 68.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp),
            // Strammet fra 20dp: med sju mellomrom utgjorde de alene over 140dp, som var
            // en stor del av grunnen til at skjermen ikke fikk plass uten scrolling.
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Klarerer DARTBUD-logoen i bakgrunnsbildet - uten denne legger "Game Setup"
            // seg oppå den. Bakgrunnen scroller ikke, så avstanden må ligge her.
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Game Setup",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier
                    .padding(bottom = 8.dp)
            )

            // Player selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Player 1
                Box(modifier = Modifier.weight(1f)) {
                    val player1Interaction = remember { MutableInteractionSource() }
                    val isPlayer1Pressed by player1Interaction.collectIsPressedAsState()

                    Button(
                        onClick = { expandedPlayer1 = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(6.dp, RoundedCornerShape(30.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xCC000000)
                        ),
                        shape = RoundedCornerShape(30.dp),
                        enabled = allPlayers.isNotEmpty(),
                        border = if (isPlayer1Pressed) {
                            BorderStroke(3.dp, Color(0xFFFFD700))
                        } else null,
                        interactionSource = player1Interaction
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Person, null, Modifier.size(20.dp), tint = Color.White)
                            Text(
                                "CHOOSE PLAYER 1",
                                fontWeight = FontWeight.Bold, color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedPlayer1,
                        onDismissRequest = { expandedPlayer1 = false }
                    ) {
                        if (userProfiles.isNotEmpty()) {
                            Text(
                                "👤 My Profiles",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            userProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(profile.username)
                                            if (profile.isPrimaryProfile) {
                                                Spacer(Modifier.width(4.dp))
                                                Text("⭐", fontSize = 12.sp)
                                            }
                                        }
                                    },
                                    onClick = {
                                        player1 = profile.username
                                        expandedPlayer1 = false
                                    }
                                )
                            }
                            if (localProfiles.isNotEmpty()) {
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            }
                        }

                        if (localProfiles.isNotEmpty()) {
                            Text(
                                "🎯 Local Players",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold, color = Color.White,
                                fontSize = 14.sp
                            )
                            localProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.username) },
                                    onClick = {
                                        player1 = profile.username
                                        expandedPlayer1 = false
                                    }
                                )
                            }
                        }

                        if (userProfiles.isEmpty() && localProfiles.isEmpty()) {
                            Text(
                                "No players yet",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Player 2
                Box(modifier = Modifier.weight(1f)) {
                    val player2Interaction = remember { MutableInteractionSource() }
                    val isPlayer2Pressed by player2Interaction.collectIsPressedAsState()

                    Button(
                        onClick = { expandedPlayer2 = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(6.dp, RoundedCornerShape(30.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xCC000000)
                        ),
                        shape = RoundedCornerShape(30.dp),
                        enabled = allPlayers.isNotEmpty(),
                        border = if (isPlayer2Pressed) {
                            BorderStroke(3.dp, Color(0xFFFFD700))
                        } else null,
                        interactionSource = player2Interaction
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Person, null, Modifier.size(20.dp), tint = Color.White)
                            Text(
                                "CHOOSE PLAYER 2",
                                fontWeight = FontWeight.Bold, color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedPlayer2,
                        onDismissRequest = { expandedPlayer2 = false }
                    ) {
                        if (userProfiles.isNotEmpty()) {
                            Text(
                                "👤 My Profiles",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            userProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(profile.username)
                                            if (profile.isPrimaryProfile) {
                                                Spacer(Modifier.width(4.dp))
                                                Text("⭐", fontSize = 12.sp)
                                            }
                                        }
                                    },
                                    onClick = {
                                        player2 = profile.username
                                        expandedPlayer2 = false
                                    }
                                )
                            }
                            if (localProfiles.isNotEmpty()) {
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            }
                        }

                        if (localProfiles.isNotEmpty()) {
                            Text(
                                "🎯 Local Players",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            localProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.username) },
                                    onClick = {
                                        player2 = profile.username
                                        expandedPlayer2 = false
                                    }
                                )
                            }
                        }

                        if (userProfiles.isEmpty() && localProfiles.isEmpty()) {
                            Text(
                                "No players yet",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Selected players display - KUN SYNLIG NÅR SPILLERE ER VALGT
            if (player1 != null || player2 != null) { // ⬅️ KUN VIS NÅR MINST ÉN ER VALGT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (player1 != null) Color(0xCC000000) else Color(0x66000000)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (player1 != null) 8.dp else 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                player1 ?: "PLAYER 1",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            if (player1 != null) {
                                TextButton(onClick = { player1 = null }) {
                                    Text("✗ Remove", fontSize = 12.sp, color = Color(0xFFF80808))
                                }
                            }
                        }
                    }

                    Text(
                        "VS",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (player2 != null) Color(0xCC000000) else Color(0x66000000)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (player2 != null) 8.dp else 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                player2 ?: "PLAYER 2",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            if (player2 != null) {
                                TextButton(onClick = { player2 = null }) {
                                    Text("✗ Remove", fontSize = 12.sp, color = Color(0xFFF80808))
                                }
                            }
                        }
                    }
                }
            }

            // Manage Players button. Column-en her har allerede spacedBy, så denne Spaceren
            // kommer på toppen av mellomrommet over og under - 80dp her ga til sammen
            // 120dp dødplass mellom spillerkortene og knappen.
            Spacer(modifier = Modifier.height(4.dp))
            val manageInteraction = remember { MutableInteractionSource() }
            val isManagePressed by manageInteraction.collectIsPressedAsState()

            Button(
                onClick = { navController.navigate("managePlayers") },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xCC000000)
                ),
                shape = RoundedCornerShape(28.dp),
                border = if (isManagePressed) {
                    BorderStroke(3.dp, Color(0xFFFFD700))
                } else null,
                interactionSource = manageInteraction
            ) {
                Text(
                    "MANAGE PLAYERS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, color = Color.White
                )
            }

            // Game Settings

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xCC000000)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🎯 GAME SETTINGS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Double In:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = doubleIn,
                            onCheckedChange = { doubleIn = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFD84FF8),
                                checkedTrackColor = Color(0x88C506D3)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Double Out:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = doubleOut,
                            onCheckedChange = { doubleOut = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFD84FF8),
                                checkedTrackColor = Color(0x88C506D3)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Calculator Mode:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            // PÅ: dagens dart-for-dart-inntasting med 2x/3x-knapper.
                            // AV: spilleren har allerede regnet ut summen selv og taster
                            // inn hele rundens total i stedet, se GameEngine.applyRoundTotal.
                            Text(
                                "(Enter each throw instead of the round total)",
                                fontSize = 11.sp,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                        Switch(
                            checked = calculatorMode,
                            onCheckedChange = { calculatorMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFD84FF8),
                                checkedTrackColor = Color(0x88C506D3)
                            )
                        )
                    }

                    // Quick Scores gir kun mening i Round Total-modus (det er ingen
                    // "vanlig rundetotal" å snarveie til når man taster kast for kast),
                    // så bryteren skjules helt - ikke bare deaktiveres - når Calculator
                    // Mode er på, i stedet for å ligge der uvirksom og ta plass.
                    if (!calculatorMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Quick Scores:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    "(Shortcut buttons for common round totals)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFAAAAAA)
                                )
                            }
                            Switch(
                                checked = quickScores,
                                onCheckedChange = { quickScores = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFD84FF8),
                                    checkedTrackColor = Color(0x88C506D3)
                                )
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Legs:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        // Ingen forklarende undertekst her: dartspillere vet hva legs er.
                        // Kampen er ferdig så snart en spiller har vunnet flertallet av
                        // dem - se GameEngine.legsNeededToWinMatch (best av 3 -> 2, osv.)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 3, 5, 7, 9).forEach { legs ->
                                val isSelected = totalLegs == legs
                                Button(
                                    onClick = { totalLegs = legs },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFD84FF8) else Color(0xFF505050)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text("$legs", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Start Game button - MED GRADIENT OG OUTLINE
            val startInteraction = remember { MutableInteractionSource() }
            val isStartPressed by startInteraction.collectIsPressedAsState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(16.dp, RoundedCornerShape(35.dp), spotColor = Color(0xFFFFD700))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1A1A1A),
                                Color(0xFF3A3A3A),
                                Color(0xFF1A1A1A)
                            )
                        ),
                        shape = RoundedCornerShape(35.dp)
                    )
                    .drawWithContent { //
                        drawContent()
                        drawRoundRect(
                            color = if (isStartPressed) Color(0xFF76E331) else Color(0xFFFFD700), // ⬅️ Skifter farge når trykket
                            cornerRadius = CornerRadius(35.dp.toPx()),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    .clickable(
                        interactionSource = startInteraction,
                        indication = null
                    ) {
                        // To spillere med samme navn ville gjort det umulig å skille dem i
                        // GameScreen (spillere identifiseres ofte på navn der), så det blokkeres her
                        if (player1 != null && player2 != null && player1 == player2) {
                            showSamePlayerWarning = true
                        } else {
                            showSamePlayerWarning = false
                            // Uvalgte spillere får default-navn - lar folk spille en rask kamp
                            // uten å måtte opprette profiler først
                            val p1Name = player1 ?: "PLAYER 1"
                            val p2Name = player2 ?: "PLAYER 2"
                            // Alle innstillinger sendes som del av navigasjonsruten (ikke delt
                            // ViewModel-state), så GameScreen kan starte helt uavhengig
                            navController.navigate("game/$doubleIn/$doubleOut/$calculatorMode/$quickScores/$totalLegs/$p1Name/$p2Name")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "START GAME!!!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            }

            if (showSamePlayerWarning) {
                Text(
                    "Player 1 and Player 2 must be different",
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}