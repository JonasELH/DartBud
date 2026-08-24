package com.group1.dartbud.screens

import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group1.dartbud.viewmodel.GameViewModel
import com.group1.dartbud.viewmodel.PlayerViewModel
import com.group1.dartbud.data.GameStatsEntity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalDensity
import com.group1.dartbud.game.GameEngine
import com.group1.dartbud.game.GameState
import com.group1.dartbud.game.Player
import com.group1.dartbud.game.calculateCheckout
import com.group1.dartbud.game.calculateCheckoutAlternatives
import com.group1.dartbud.game.MULTIPLY_SYMBOL
import com.group1.dartbud.game.PLUS_SYMBOL
import com.group1.dartbud.game.formatExpressionForDisplay
import com.group1.dartbud.game.roundTotalFromExpression
import com.group1.dartbud.game.isValidThrowInput


// Hjelpefunksjon for å skalere ned skriftstørrelse uten å gå over en maks-grense
fun TextUnit.coerceAtMost(maximumValue: TextUnit): TextUnit {
    return if (this.value > maximumValue.value) maximumValue else this
}

// Selve spillskjermen for en 501-kamp. All spillogikk (bust, checkout, undo, snitt)
// ligger nested her inne som lokale funksjoner, siden de kun gir mening sammen med
// denne skjermens state (throw1/2/3, currentPlayer, osv.).
@Composable
fun GameScreen(
    navController: NavController,
    doubleInEnabled: Boolean = false, // Krever double for å "åpne" scoring (double-in)
    doubleOutEnabled: Boolean = true, // Krever double (eller bullseye) for å avslutte kampen
    calculatorModeEnabled: Boolean = false, // PÅ: dart for dart. AV: tast inn hele rundetotalen
    quickScoresEnabled: Boolean = false, // Snarveisknapper for vanlige rundetotaler (26/41/45/60/85/100)
    totalLegs: Int = 1, // Kampformat: best av 1/3/5/7/9 legs
    player1Name: String = "PLAYER 1",
    player2Name: String = "PLAYER 2",
    gameViewModel: GameViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {

    // Skjermstørrelse basert på device-konfigurasjon, brukt til å regne ut skriftstørrelser
    // lenger ned. Merk: playerCardHeight/throwButtonHeight/osv. her blir skygget av
    // nye variabler med samme navn inne i BoxWithConstraints under (målt fra faktisk
    // tilgjengelig plass), så det er kun font-størrelsene herfra som faktisk brukes i layoutet.
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    val playerCardHeight = (screenHeight * 0.25f).coerceIn(220.dp, 320.dp)
    val throwButtonHeight = (screenHeight * 0.045f).coerceIn(35.dp, 55.dp)
    val scoreDisplayHeight = (screenHeight * 0.07f).coerceIn(45.dp, 55.dp)
    val actionButtonHeight = (screenHeight * 0.065f).coerceIn(48.dp,62.dp)
    val numberButtonHeight = (screenHeight * 0.078f).coerceIn(52.dp,70.dp)

    val playerCardFontSize = (playerCardHeight.value * 0.20f).coerceIn(44f, 60f).sp
    val throwButtonFontSize = (throwButtonHeight.value * 0.24f).coerceIn(10f, 13f).sp
    val scoreDisplayFontSize = (scoreDisplayHeight.value * 0.35f).coerceIn(20f, 28f).sp
    val actionButtonFontSize = (actionButtonHeight.value * 0.26f).coerceIn(14f, 18f).sp
    val numberButtonFontSize = (numberButtonHeight.value * 0.4f).coerceIn(24f, 32f).sp

    // All spillogikk (bust, double-in, turskifte, undo) ligger i GameEngine - ren Kotlin
    // uten Compose, dekket av enhetstester i GameEngineTest. Skjermen holder bare én
    // tilstand og sender kast inn i motoren. Reglene bodde tidligere her i UI-laget, der
    // de var umulige å teste automatisk - og det var nettopp der bust-regelen rakk å bli
    // ulik mellom kast 1, 2 og 3.
    val engine = remember(doubleInEnabled, doubleOutEnabled) {
        GameEngine(doubleInEnabled = doubleInEnabled, doubleOutEnabled = doubleOutEnabled)
    }
    var gameState by remember {
        mutableStateOf(GameState.new(player1Name, player2Name, totalLegsInMatch = totalLegs))
    }
    var firstPlayer by remember { mutableStateOf(1) } // Brukes ved rematch for å bytte hvem som starter

    // Utpakking av tilstanden, slik at UI-koden under leser de samme navnene som før
    val player1 = gameState.player1
    val player2 = gameState.player2
    val currentPlayer = gameState.currentPlayer
    val currentThrow = gameState.currentThrow
    val overallRound = gameState.overallRound
    val throw1 = gameState.throw1
    val throw2 = gameState.throw2
    val throw3 = gameState.throw3
    val winner = gameState.winner
    // Legen kan være vunnet (winnerNumber != 0) uten at KAMPEN er ferdig, hvis
    // totalLegsInMatch > 1 (best av 3/5/7/9). Skiller derfor mellom to dialoger:
    // en kort "Leg won, ready for next leg?"-dialog mens kampen fortsetter, og den
    // vanlige vinner-dialogen først når flertallet av legs faktisk er vunnet - se
    // matchWinnerNumber på GameState.
    val legJustWon = gameState.winnerNumber != 0
    val matchIsWon = gameState.matchWinnerNumber != 0
    val showLegWinDialog = legJustWon && !matchIsWon
    val showWinDialog = matchIsWon

    // Samme dialog brukes til bust og til "kastet ga ingen poeng enda" ved double-in,
    // så tittelen må følge med på hvilken av de to det er.
    val showBustDialog = gameState.message != null
    val bustTitle = gameState.message?.title ?: ""
    val bustMessage = gameState.message?.text ?: ""

    // Tallpad-input: tallet brukeren har skrevet inn og hvilken multiplikator som er valgt
    var inputValue by remember { mutableStateOf("") }
    var multiplier by remember { mutableStateOf(1) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Calculator Mode av: spørsmålet "hvor mange piler brukte du?" vises kun når en
    // rundetotal treffer nøyaktig 0 - se confirmRoundTotal(). pendingRoundTotal holder
    // på summen mens spilleren svarer, siden den ikke skal sendes til motoren før vi
    // vet antall piler (trengs for et riktig snitt).
    var showCheckoutDartsDialog by remember { mutableStateOf(false) }
    var pendingRoundTotal by remember { mutableStateOf(0) }

    // "Veksle mellom utganger"-knappen (kun Round Total-modus): hvilket alternativ i
    // calculateCheckoutAlternatives() som vises for den AKTIVE spilleren akkurat nå.
    // Nullstilles hver gang det er en ny score å vise utgang for - ellers kunne man
    // f.eks. stå igjen på alternativ 2 av 3 fra forrige runde på en helt annen score.
    var checkoutAltIndex by remember { mutableStateOf(0) }
    LaunchedEffect(gameState.currentPlayer, gameState.activePlayer.score) {
        checkoutAltIndex = 0
    }
    val activeCheckoutAlternatives = calculateCheckoutAlternatives(gameState.activePlayer.score)
    val canCycleCheckout = activeCheckoutAlternatives.size > 1
    val activeCheckoutSuggestion = activeCheckoutAlternatives[checkoutAltIndex % activeCheckoutAlternatives.size]

    // Verdien det aktuelle tallpad-inputet representerer, med multiplikator tatt hensyn til
    val currentInputScore = (inputValue.toIntOrNull() ?: 0) * multiplier
    // Calculator Mode på: bare summer én enkelt pil faktisk kan gi godtas (isValidThrowInput).
    // Calculator Mode av: hele rundetotalen skrives inn direkte, gyldig område er 0-180
    // (maks er tre trippel-20). Motoren stoler på spilleren for double-in/double-out -
    // se GameEngine.applyRoundTotal.
    // I Round Total-modus kan inputet være et helt regnestykke ("17×3+13×3+19×3"), ikke
    // bare et tall - se roundTotalFromExpression. Den håndterer også det enkle tilfellet
    // der spilleren har regnet selv og bare taster "147".
    val isValidInput = if (calculatorModeEnabled) {
        inputValue.toIntOrNull()?.let { isValidThrowInput(it, multiplier) } ?: false
    } else {
        roundTotalFromExpression(inputValue) != null
    }

    fun clearInput() {
        inputValue = ""
        multiplier = 1
    }

    // × og + i Round Total-modus. To operatorer på rad ville gitt et uttrykk som aldri
    // kan regnes ut, så en ny operator erstatter den forrige i stedet - slik en vanlig
    // kalkulator oppfører seg. En operator som aller første tegn ignoreres.
    fun appendOperator(symbol: String) {
        if (inputValue.isEmpty()) return
        val endsWithOperator = inputValue.endsWith(MULTIPLY_SYMBOL) || inputValue.endsWith(PLUS_SYMBOL)
        inputValue = if (endsWithOperator) inputValue.dropLast(1) + symbol else inputValue + symbol
    }

    // Lukker bust-/no-score-dialogen ved å fjerne meldingen fra tilstanden
    fun dismissMessage() {
        gameState = gameState.copy(message = null)
        clearInput()
    }

    fun confirmThrow() {
        val value = inputValue.toIntOrNull() ?: return
        gameState = engine.applyThrow(gameState, value, multiplier)
        clearInput()
    }

    // Calculator Mode av: bekrefter en innskrevet rundetotal. Bringer den scoren til
    // nøyaktig 0, må vi vite hvor mange piler som ble brukt (for snittet) FØR vi sender
    // noe til motoren - så her stopper vi opp og spør i stedet for å kalle
    // applyRoundTotal med en gang.
    fun confirmRoundTotal(quickValue: Int? = null) {
        val value = quickValue ?: roundTotalFromExpression(inputValue) ?: return
        val wouldFinish = gameState.activePlayer.score - value == 0
        if (wouldFinish) {
            pendingRoundTotal = value
            showCheckoutDartsDialog = true
        } else {
            gameState = engine.applyRoundTotal(gameState, value)
            clearInput()
        }
    }

    // "No Score"-knappen: en snarvei for å taste inn 0. Kan aldri utløse
    // checkout-spørsmålet over, siden en spiller ikke kan stå på 0 poeng igjen
    // midt i en kamp.
    fun confirmNoScore() {
        gameState = engine.applyRoundTotal(gameState, 0)
        clearInput()
    }

    // Svar på "hvor mange piler brukte du til å avslutte?"-dialogen.
    fun confirmCheckoutDarts(darts: Int) {
        gameState = engine.applyRoundTotal(gameState, pendingRoundTotal, dartsUsedForCheckout = darts)
        showCheckoutDartsDialog = false
        clearInput()
    }

    fun undoLastThrow() {
        gameState = engine.undo(gameState)
        clearInput()
    }

    // Lagrer den ferdigspilte kampen. Kalles direkte, ikke via rememberCoroutineScope:
    // saveGame kjører selv videre på viewModelScope, mens skjermens eget scope kanselleres
    // i det vi navigerer bort. Tidligere lå kallet inne i scope.launch { } rett før en
    // navigate(), og rakk derfor ofte ikke å kjøre - kampen ble stille aldri lagret.
    fun saveFinishedGame() {
        val players = playerViewModel.players.value
        val p1 = players.find { it.username == player1Name }
        val p2 = players.find { it.username == player2Name }
        if (p1 == null || p2 == null) {
            android.util.Log.w(
                "SaveDebug",
                "Lagring hoppet over. player1Name='$player1Name' (funnet=${p1 != null}), " +
                    "player2Name='$player2Name' (funnet=${p2 != null}), " +
                    "kjente brukernavn i Room: ${players.map { "'${it.username}'" }}"
            )
            return
        }
        android.util.Log.i("SaveDebug", "Fant begge spillerne, kaller saveGame() na")

        gameViewModel.saveGame(
            player1Id = p1.playerId,
            player2Id = p2.playerId,
            winnerId = if (gameState.matchWinnerNumber == 1) p1.playerId else p2.playerId,
            doubleIn = doubleInEnabled,
            doubleOut = doubleOutEnabled,
            player1LegsWon = gameState.player1LegsWon,
            player2LegsWon = gameState.player2LegsWon,
            totalLegsInMatch = gameState.totalLegsInMatch,
            player1Stats = GameStatsEntity(
                gameId = 0,
                playerId = p1.playerId,
                average = player1.average,
                highestScore = player1.highestScore,
                dartsThrown = player1.dartsThrown,
                roundsPlayed = player1.roundsPlayed,
                finalScore = player1.score
            ),
            player2Stats = GameStatsEntity(
                gameId = 0,
                playerId = p2.playerId,
                average = player2.average,
                highestScore = player2.highestScore,
                dartsThrown = player2.dartsThrown,
                roundsPlayed = player2.roundsPlayed,
                finalScore = player2.score
            )
        )
    }

    // Nullstiller til en ny kamp med de samme to spillerne. Denne nullstillingen lå
    // tidligere inne i den samme if-en som sjekket at begge spillerprofilene fantes i
    // databasen - fant den dem ikke, ble ingenting nullstilt, og vinner-dialogen (som
    // ikke kan lukkes med tilbake eller trykk utenfor) ble stående for alltid.
    fun startRematch() {
        firstPlayer = if (firstPlayer == 1) 2 else 1
        gameState = engine.rematch(gameState, firstPlayer)
        clearInput()
    }

    // "Ready for next leg?"-dialogens OK-knapp: friske 501-poengsummer, men legs-
    // stillingen (og kampens format) tas med videre - se GameEngine.startNextLeg.
    fun startNextLeg() {
        gameState = engine.startNextLeg(gameState)
        clearInput()
    }

    // Bekreftelsesdialog når brukeren trykker tilbake-pilen midt i en kamp - kampen
    // lagres ikke hvis de bekrefter exit
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    "Exit Game?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    "The current game will not be saved. Are you sure you want to exit?",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xDD000000)
        )
    }

    // Round Total-modus: vises når en innskrevet sum bringer scoren til nøyaktig 0.
    // Motoren stoler på at spilleren faktisk traff en gyldig avslutning (se
    // GameEngine.applyRoundTotal), men trenger antall piler for å regne riktig snitt -
    // det er alt dette spørsmålet handler om, ikke om avslutningen var gyldig.
    if (showCheckoutDartsDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    "Checkout!",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    "How many darts did you use to checkout?",
                    color = Color.White
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3).forEach { darts ->
                        Button(
                            onClick = { confirmCheckoutDarts(darts) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("$darts", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            containerColor = Color(0xDD000000)
        )
    }

    // Vises når motoren meldte bust, eller at kastet ikke ga poeng fordi spilleren
    // ikke har åpnet scoringen enda (double-in). bustTitle skiller de to.
    if (showBustDialog) {
        AlertDialog(
            onDismissRequest = { dismissMessage() },
            title = { Text(bustTitle, fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text(bustMessage, color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { dismissMessage() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text("OK")
                }
            },
            containerColor = Color(0xDD000000)
        )
    }

    // Vises når en leg er vunnet, men kampen fortsetter (best av 3/5/7/9 og ingen har
    // nådd flertallet av legs enda). Kort dialog, kun én OK-knapp - motsatt av
    // vinner-dialogen under er dette ikke et endelig stoppunkt, bare en pause mellom
    // to legs.
    if (showLegWinDialog && winner != null) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    "Leg won!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${winner!!.name} wins the leg!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        "Leg score: ${gameState.player1LegsWon} - ${gameState.player2LegsWon}",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        "Ready for next leg?",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { startNextLeg() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF388E3C)
                    )
                ) {
                    Text("OK")
                }
            },
            dismissButton = { },
            containerColor = Color(0xDD000000)
        )
    }

    // Vises når en spiller vinner (score nøyaktig 0 med gyldig checkout). Har ingen
    // "dismiss" (onDismissRequest er tom) - spilleren MÅ velge Main Menu eller Rematch,
    // det finnes ingen vei tilbake til selve spillet herfra.
    if (showWinDialog && winner != null) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🎯 WINNER! 🎯",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        winner!!.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    // Kun relevant for kamper med flere legs (best av 3/5/7/9) - et
                    // enkelt-leg-format har ingenting å vise her utover selve seieren.
                    if (gameState.totalLegsInMatch > 1) {
                        Text(
                            "Legs: ${gameState.player1LegsWon} - ${gameState.player2LegsWon}",
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.3f))
                    Text(
                        "Average: ${String.format("%.1f", winner!!.average)}",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        "Rounds: ${winner!!.roundsPlayed}",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        "Darts: ${winner!!.dartsThrown}",
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                saveFinishedGame()
                                // main_menu blir ny bunn i back-stacken (popUpTo inclusive) -
                                // "tilbake" fra hovedmenyen skal aldri kunne havne tilbake i
                                // den ferdigspilte kampen
                                navController.navigate("main_menu") {
                                    popUpTo("main_menu") { inclusive = true }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFC1E69)
                            )
                        ) {
                            Text("Main Menu")
                        }
                        // "Rematch"-knappen: lagrer kampen (samme som over), men i stedet for
                        // å navigere bort nullstiller den all spill-state lokalt slik at en ny
                        // kamp starter med de samme to spillerne uten å forlate skjermen
                        Button(
                            onClick = {
                                saveFinishedGame()
                                // Nullstillingen skjer uansett om kampen lot seg lagre eller
                                // ikke - ellers blir vinner-dialogen stående uten vei ut
                                startRematch()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Rematch")
                        }
                    }
                }
            },
            dismissButton = { },
            containerColor = Color(0xDD000000)
        )
    }

    // Selve spillebrettet. BoxWithConstraints gir oss faktisk tilgjengelig plass
    // (maxWidth/maxHeight) etter at f.eks. statusbar/navigasjon er trukket fra, som brukes
    // til å regne ut størrelser proporsjonalt med skjermen (bedre tilpasning enn faste dp-verdier
    // på tvers av forskjellige telefoner). Disse variablene skygger de likelydende fra
    // lenger oppe i funksjonen.
    BoxWithConstraints {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val playerCardHeight = (screenHeight * 0.28f).coerceAtMost(180.dp)
        val throwButtonHeight = (screenHeight * 0.08f).coerceAtMost(70.dp)
        val scoreDisplayHeight = (screenHeight * 0.09f).coerceAtMost(90.dp)
        val actionButtonHeight = (screenHeight * 0.08f).coerceAtMost(58.dp)
        val numberButtonHeight = (screenHeight * 0.1f).coerceAtMost(60.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .padding((screenWidth*0.02f).coerceAtLeast(8.dp)),
            verticalArrangement = Arrangement.Top
        ) {
            IconButton(
                onClick = { showExitDialog = true },
                modifier = Modifier
                    .align(Alignment.Start)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Uten Calculator Mode vises ikke throw 1/2/3-raden under (den har
                    // ingenting meningsfullt å vise når inndata er en rundetotal, ikke
                    // enkeltkast), så spillervinduene arver dens vekt (0,05) og vokser
                    // tilsvarende for å fylle plassen.
                    .weight(if (calculatorModeEnabled) 0.25f else 0.30f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerCard(
                    player = player1,
                    isActive = currentPlayer == 1,
                    backgroundColor = Color(0xFF505050),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    checkout = if (currentPlayer == 1) activeCheckoutSuggestion else calculateCheckout(player1.score),
                    roundNumber = overallRound,
                    legsWon = if (gameState.totalLegsInMatch > 1) gameState.player1LegsWon else null
                )

                PlayerCard(
                    player = player2,
                    isActive = currentPlayer == 2,
                    backgroundColor = Color(0xFF505050),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    checkout = if (currentPlayer == 2) activeCheckoutSuggestion else calculateCheckout(player2.score),
                    roundNumber = overallRound,
                    legsWon = if (gameState.totalLegsInMatch > 1) gameState.player2LegsWon else null
                )
            }

            // Skjules i Round Total-modus - ingen mening å vise "kast 1/2/3" når
            // inndata er én sum for hele runden. Se PlayerCard-raden over for hvor
            // denne vekten (0,05) havner i stedet.
            if (calculatorModeEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.05f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThrowButton(
                        label = "THROW 1:",
                        value = throw1,
                        isActive = currentThrow == 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .fillMaxHeight()
                    )
                    ThrowButton(
                        label = "THROW 2:",
                        value = throw2,
                        isActive = currentThrow == 2,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .fillMaxHeight()
                    )
                    ThrowButton(
                        label = "THROW 3:",
                        value = throw3,
                        isActive = currentThrow == 3,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .fillMaxHeight()
                    )
                }
            }

            // Viser tallpad-inputet live etter hvert som brukeren taster, med multiplikatoren
            // ("× 2"/"× 3") lagt til hvis Double/Triple er valgt. "..." vises som placeholder
            // når feltet er tomt.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.10f)
                    .padding(8.dp)
                    .shadow(12.dp, RoundedCornerShape(15.dp), spotColor = Color(0xFFF5DF20))
                    .background(Color(0xFF0A0A0A), RoundedCornerShape(15.dp))
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color(0xFF333333),
                            cornerRadius = CornerRadius(15.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val displayText = if (inputValue.isNotEmpty()) {
                    // formatExpressionForDisplay setter parentes rundt hvert kast som er
                    // ganget opp ("(19×3) + (17×3)"). I Calculator Mode er inputValue bare
                    // et tall, og da gjør den ingenting.
                    "${formatExpressionForDisplay(inputValue)} ${if (multiplier > 1) "× $multiplier" else ""}"
                } else {
                    "..."
                }
                // Et helt regnestykke ("17×3+13×3+19×3") er mye bredere enn de 1-3
                // sifrene feltet opprinnelig var laget for, så teksten krymper til den
                // får plass - se AutoShrinkText.
                AutoShrinkText(
                    text = displayText,
                    baseFontSize = (scoreDisplayFontSize.value * 1.6f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (inputValue.isNotEmpty()) Color(0xFFE7D325) else Color(0xFFE1CD1B),
                    minFontSize = 12.sp
                )
            }

            // Undo / Double / Triple-raden. Double og Triple er "toggle"-knapper (trykk igjen
            // for å slå av) som styrer multiplier-verdien som currentInputScore ganges med.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.06f)
                    .padding(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                val undoInteraction = remember { MutableInteractionSource() }
                val isUndoPressed by undoInteraction.collectIsPressedAsState()

                Button(
                    onClick = { undoLastThrow() },
                    modifier = Modifier
                        // I Round Total-modus deler Undo raden kun med No Score (ikke
                        // Double/Triple), og skal da være like stor som den - se
                        // weight(2f) på No Score-knappen under.
                        .weight(if (calculatorModeEnabled) 1f else 2f)
                        .fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF505050)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    border = if (isUndoPressed) {
                        BorderStroke(3.dp, Color(0xFFB2073F))
                    } else {
                        BorderStroke(1.5.dp, Color(0xEBF148E8))
                    },
                    interactionSource = undoInteraction
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "↺",
                            fontSize = (actionButtonFontSize.value * 1.8f).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            softWrap = false,
                            maxLines = 1,
                            modifier=Modifier.offset(y=(-4).dp)

                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Undo",
                            fontSize = actionButtonFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }

                if (calculatorModeEnabled) {
                    val doubleInteraction = remember { MutableInteractionSource() }
                    val isDoublePressed by doubleInteraction.collectIsPressedAsState()

                    Button(
                        onClick = { multiplier = if (multiplier == 2) 1 else 2 },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF505050)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = if (multiplier == 2 || isDoublePressed) BorderStroke(3.dp, Color(
                            0xFFDECA2A
                        )
                        ) else null,
                        interactionSource = doubleInteraction
                    ) {
                        Text(
                            "Double",
                            fontSize = actionButtonFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    val tripleInteraction = remember { MutableInteractionSource() }
                    val isTriplePressed by tripleInteraction.collectIsPressedAsState()

                    Button(
                        onClick = { multiplier = if (multiplier == 3) 1 else 3 },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF505050)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = if (multiplier == 3 || isTriplePressed) BorderStroke(3.dp, Color(
                            0xFFDECA2A
                        )
                        ) else null,
                        interactionSource = tripleInteraction
                    ) {
                        Text(
                            "Triple",
                            fontSize = actionButtonFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // Round Total-modus: Double/Triple gir ingen mening (det er ingen
                    // enkeltpil å multiplisere), så plassen deres blir én "No Score"-knapp -
                    // en snarvei for å taste inn 0 uten å måtte bruke tallpaden.

                    // "Veksle mellom utganger"-knappen: bytter hvilket checkout-forslag som
                    // vises for DEN AKTIVE spilleren (se activeCheckoutSuggestion). Grået ut
                    // og ikke trykkbar når det ikke finnes noen reell alternativ rute.
                    val cycleCheckoutInteraction = remember { MutableInteractionSource() }
                    val isCycleCheckoutPressed by cycleCheckoutInteraction.collectIsPressedAsState()

                    Button(
                        onClick = { checkoutAltIndex++ },
                        enabled = canCycleCheckout,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3A3A3A),
                            disabledContainerColor = Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = if (isCycleCheckoutPressed) {
                            BorderStroke(3.dp, Color(0xFFFFD700))
                        } else {
                            BorderStroke(1.5.dp, Color(0xEBF148E8).copy(alpha = if (canCycleCheckout) 1f else 0.35f))
                        },
                        interactionSource = cycleCheckoutInteraction
                    ) {
                        Text(
                            "⇌",
                            fontSize = (actionButtonFontSize.value * 1.6f).sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canCycleCheckout) Color.White else Color(0xFF6B6B6B)
                        )
                    }

                    val noScoreInteraction = remember { MutableInteractionSource() }
                    val isNoScorePressed by noScoreInteraction.collectIsPressedAsState()

                    Button(
                        onClick = { confirmNoScore() },
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3A3A3A)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = if (isNoScorePressed) {
                            BorderStroke(3.dp, Color(0xFFFFD700))
                        } else {
                            BorderStroke(1.5.dp, Color(0xEBF148E8))
                        },
                        interactionSource = noScoreInteraction
                    ) {
                        Text(
                            "No Score",
                            fontSize = actionButtonFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Runder kun av de ytre hjørnene til talltastaturet (venstre knapp i raden får
            // rundet venstre hjørne, høyre knapp får rundet høyre hjørne, midtre knapp
            // forblir firkantet) slik at hele tastaturet ser ut som én sammenhengende,
            // avrundet blokk selv om den består av mange enkeltknapper med 1dp mellomrom.
            fun getCornerShape(position: Int, totalInRow: Int, includeBottom: Boolean = false): RoundedCornerShape {
                return when (position) {
                    0 -> RoundedCornerShape(
                        topStart = 6.dp,
                        bottomStart = if (includeBottom) 6.dp else 0.dp
                    )
                    totalInRow - 1 -> RoundedCornerShape(
                        topEnd = 6.dp,
                        bottomEnd = if (includeBottom) 6.dp else 0.dp
                    )
                    else -> RoundedCornerShape(0.dp)
                }
            }

            // Liten luke her, ellers ligger Undo/No Score sin rosa kant og talltastaturets
            // rosa bakgrunn rett inntil hverandre og smelter sammen til en tykk, ujevn
            // dobbel-linje langs underkanten av Undo/No Score.
            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.30f)
                    .background(Color(0xEBF148E8), RoundedCornerShape(8.dp))
                    .padding(1.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Quick Scores er kun relevant i Round Total-modus (det er ingen
                // "vanlig rundetotal" å snarveie til når man taster kast for kast), og
                // vises derfor kun når begge er sant. Radene får to ekstra kolonner
                // (5 i stedet for 3) når snarveiene er med, som avgjør hjørneavrundingen
                // via getCornerShape sitt totalInRow-argument.
                val showQuickScores = quickScoresEnabled && !calculatorModeEnabled
                val digitCols = if (showQuickScores) 5 else 3

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (showQuickScores) {
                        NumberButton(
                            number = 26,
                            onClick = { confirmRoundTotal(26) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            shape = getCornerShape(0, digitCols, includeBottom = true),
                            containerColor = Color(0xFF3A3A3A)
                        )
                    }
                    for (i in 1..3) {
                        NumberButton(
                            number = i,
                            onClick = { inputValue += i.toString() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            shape = getCornerShape(if (showQuickScores) i else i-1, digitCols, includeBottom=true)
                        )
                    }
                    if (showQuickScores) {
                        NumberButton(
                            number = 60,
                            onClick = { confirmRoundTotal(60) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            shape = getCornerShape(4, digitCols, includeBottom = true),
                            containerColor = Color(0xFF3A3A3A)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (showQuickScores) {
                        NumberButton(
                            number = 41,
                            onClick = { confirmRoundTotal(41) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            shape = getCornerShape(0, digitCols, includeBottom = true),
                            containerColor = Color(0xFF3A3A3A)
                        )
                    }
                    for (i in 4..6) {
                        NumberButton(
                            number = i,
                            onClick = { inputValue += i.toString() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            shape = getCornerShape(if (showQuickScores) i-3 else i-4, digitCols, includeBottom=true)
                        )
                    }
                    if (showQuickScores) {
                        NumberButton(
                            number = 85,
                            onClick = { confirmRoundTotal(85) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            shape = getCornerShape(4, digitCols, includeBottom = true),
                            containerColor = Color(0xFF3A3A3A)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (showQuickScores) {
                        NumberButton(
                            number = 45,
                            onClick = { confirmRoundTotal(45) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            shape = getCornerShape(0, digitCols, includeBottom = true),
                            containerColor = Color(0xFF3A3A3A)
                        )
                    }
                    for (i in 7..9) {
                        NumberButton(
                            number = i,
                            onClick = { inputValue += i.toString() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            shape = getCornerShape(if (showQuickScores) i-6 else i-7, digitCols, includeBottom=true)
                        )
                    }
                    if (showQuickScores) {
                        NumberButton(
                            number = 100,
                            onClick = { confirmRoundTotal(100) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            shape = getCornerShape(4, digitCols, includeBottom = true),
                            containerColor = Color(0xFF3A3A3A)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    val clearInteraction = remember { MutableInteractionSource() }
                    val isClearPressed by clearInteraction.collectIsPressedAsState()

                    Button(
                        onClick = {
                            inputValue = ""
                            multiplier = 1
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF505050)),
                        shape = RoundedCornerShape(bottomStart = 6.dp, topStart = 6.dp, bottomEnd = 6.dp),
                        border = if (isClearPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
                        interactionSource = clearInteraction
                    ) {
                        AutoShrinkText(
                            text = "CLR",
                            baseFontSize = numberButtonFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // × og + finnes kun i Round Total-modus: der taster man hele rundens sum,
                    // og da er det nyttig å kunne regne den ut på stedet ("17×3+13×3+19×3")
                    // i stedet for i hodet. I Calculator Mode taster man én pil av gangen,
                    // så + gir ingen mening, og Double/Triple dekker allerede ×.
                    if (!calculatorModeEnabled) {
                        OperatorButton(
                            symbol = MULTIPLY_SYMBOL,
                            onClick = { appendOperator(MULTIPLY_SYMBOL) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            fontSize = numberButtonFontSize
                        )
                    }

                    NumberButton(
                        number = 0,
                        onClick = { inputValue += "0" },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                    )

                    if (!calculatorModeEnabled) {
                        OperatorButton(
                            symbol = PLUS_SYMBOL,
                            onClick = { appendOperator(PLUS_SYMBOL) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            fontSize = numberButtonFontSize
                        )
                    }

                    // Bekreft-knappen ("✓"/"✗"): grønn og aktiv når inputet er tomt (ingenting
                    // å bekrefte enda) eller gyldig (0-60), rød når spilleren har tastet inn
                    // noe som er for høyt til å være et lovlig enkeltkast - gir umiddelbar
                    // visuell tilbakemelding før de i det hele tatt trykker
                    val confirmInteraction = remember { MutableInteractionSource() }
                    val isConfirmPressed by confirmInteraction.collectIsPressedAsState()

                    Button(
                        onClick = {
                            if (isValidInput) {
                                if (calculatorModeEnabled) confirmThrow() else confirmRoundTotal()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inputValue.isEmpty()) Color(0xFF388E3C)
                            else if (isValidInput) Color(0xFF388E3C)
                            else Color(0xFFFF0000),
                            disabledContainerColor = if (!isValidInput && inputValue.isNotEmpty()) Color(0xFFFF0000)
                            else Color(0xFF388E3C)
                        ),
                        shape = RoundedCornerShape(bottomEnd = 6.dp, bottomStart = 6.dp, topEnd = 6.dp),
                        border = if (isConfirmPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
                        interactionSource = confirmInteraction
                    ) {
                        Text(
                            text = if (inputValue.isEmpty()) "✓" else if (isValidInput) "✓" else "✗",
                            fontSize = numberButtonFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}

// Statuskortet for én spiller (navn, score, siste kast, checkout-forslag, snitt/runde/darts).
// isActive styrer highlighten (glow-ramme + lysere gradient) som viser hvem sin tur det er.
// currentRoundTotal er deklarert men brukes ikke i selve visningen her.
@Composable
fun PlayerCard(
    player: Player,
    isActive: Boolean,
    backgroundColor: Color,
    checkout: String,
    roundNumber: Int,
    currentRoundTotal: Int = 0,
    // Antall legs spilleren har vunnet i kampen. null i kamper som kun er én leg -
    // da finnes det ingen stilling å vise, og raden droppes helt.
    legsWon: Int? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .shadow(
                elevation = if (isActive) 16.dp else 8.dp,
                shape = RoundedCornerShape(8.dp),
                spotColor = Color(0xEBF148E8)
            )
            .then(
                if (isActive) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color(0xEBF148E8),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints {
            val cardHeight = maxHeight
            with(LocalDensity.current) {
                val fontSize = (cardHeight * 0.3f).toSp().coerceAtMost(32.sp)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (isActive) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF505050),
                                        Color(0xFF3A3A3A),
                                        Color(0xFF505050)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(listOf(backgroundColor, backgroundColor))
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding((cardHeight.value * 0.055f).dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Navn og legs-stilling ligger i en egen Column her, ikke som
                            // hvert sitt barn av Column-en utenfor - den bruker SpaceBetween
                            // til å fordele topp/score/bunn, og et fjerde barn ville brutt
                            // den fordelingen.
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isActive) {
                                        Text(
                                            text = "→",
                                            // Holder samme forhold til navnet som før (ca. 0,7x)
                                            fontSize = (fontSize.value * 0.32f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier
                                                .padding(end = 4.dp)
                                                .offset(y = (-0.5f).dp)
                                        )
                                    }
                                    Text(
                                        // 0,45 og ikke 0,35: fontSize er allerede kappet på
                                        // 32sp, så navnet lå på ~11sp - mindre enn resten av
                                        // teksten på skjermen, og for spinkelt mot LEGS-linjen
                                        // under.
                                        text = player.name,
                                        fontSize = (fontSize.value * 0.45f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                if (legsWon != null) {
                                    Text(
                                        text = "LEGS: $legsWon",
                                        fontSize = (fontSize.value * 0.28f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE7D325),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${player.score}",
                            fontSize = (fontSize.value * 1.5f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "LAST: ${player.lastThrow}",
                                fontSize = (fontSize.value * 0.35f).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = checkout,
                                fontSize = (fontSize.value * 0.35f).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "AVG\n${String.format("%.1f", player.average)}",
                                    fontSize = (fontSize.value * 0.35f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "ROUND\n$roundNumber",
                                    fontSize = (fontSize.value * 0.35f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "DARTS\n${player.dartsThrown}",
                                    fontSize = (fontSize.value * 0.35f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Viser ett av de tre kastene i inneværende runde ("THROW 1: 60" osv). Er ikke faktisk
// klikkbar (onClick er tom) - dette er kun en visningsboks, ikke et input-element.
// isActive markerer hvilket av de tre kastene som er "på tur" med en glow-kant.
@Composable
fun ThrowButton(
    label: String,
    value: Int?,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val buttonHeight = maxHeight
        val cornerRadius = buttonHeight * 0.5f

        with(LocalDensity.current) {
            val fontSize = (buttonHeight * 0.24f).toSp().coerceAtMost(13.sp)

            Button(
                onClick = { },
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B6B6B)
                ),
                shape = RoundedCornerShape(cornerRadius),
                border = if (isActive) {
                    BorderStroke(1.5.dp, Color(0xEBF148E8))
                } else null,
                contentPadding = PaddingValues(horizontal = 1.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$label ${value ?: ""}",
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Tekst som krymper til den faktisk får plass i bredden.
 *
 * Flere steder på denne skjermen varierer innholdet mye mer enn plassen: en knapp kan
 * inneholde "1" eller "100", og score-displayet alt fra ett siffer til et helt
 * regnestykke. En fast størrelse blir da enten for stor (teksten brekker eller klippes)
 * eller unødig liten. Måler derfor faktisk overflow og skalerer ned 1sp av gangen.
 *
 * remember(text, baseFontSize) nullstiller ved hver endring - ellers ville størrelsen
 * bare gått nedover og aldri kommet tilbake når innholdet blir kortere igjen.
 */
@Composable
fun AutoShrinkText(
    text: String,
    baseFontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    minFontSize: TextUnit = 10.sp
) {
    var fontSize by remember(text, baseFontSize) { mutableStateOf(baseFontSize) }

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        color = color,
        softWrap = false,
        maxLines = 1,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize > minFontSize) {
                fontSize = (fontSize.value - 1f).sp
            }
        }
    )
}

// Regneoperator (× og +) i Round Total-modus. Samme mørkere grå som Quick Scores-
// knappene, siden begge deler er "noe annet enn et siffer" og bør lese sånn.
@Composable
fun OperatorButton(
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier,
    fontSize: TextUnit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A)),
        border = if (isPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
        interactionSource = interaction
    ) {
        AutoShrinkText(
            text = symbol,
            baseFontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

// Gjenbrukbar tastaturknapp for tallpaden (0-9). shape sendes inn utenfra via
// getCornerShape() slik at kun de ytre knappene i rutenettet får avrundede hjørner.
@Composable
fun NumberButton(
    number: Int,
    onClick: () -> Unit,
    modifier: Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp),
    // Litt mørkere enn vanlig for Quick Scores-snarveiene (26/41/45/60/85/100), slik at
    // de skiller seg fra de vanlige sifferknappene uten å kopiere fargebruken i
    // referanseappen rett av.
    containerColor: Color = Color(0xFF505050)
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor
        ),
        border = if (isPressed) BorderStroke(3.dp, Color(0xFFFFD700)) else null,
        interactionSource = interaction
    ) {
        // Ett siffer (0-9) og en tresifret snarvei (f.eks. 100) deler samme knapp, og
        // Quick Scores gjør i tillegg kolonnene smalere. Lar derfor teksten krympe til
        // den passer i stedet for å gjette en fast størrelse - se AutoShrinkText.
        AutoShrinkText(
            text = number.toString(),
            baseFontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = Color.White
        )
    }
}
