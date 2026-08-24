package com.group1.dartbud.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Fargetema for SPILLSKJERMEN - rammene rundt spillerkortene, rutenettet bak
 * talltastaturet og kantene på handlingsknappene.
 *
 * Gjelder bevisst kun spillskjermen. Menyene ligger oppå et knallrosa bakgrunnsbilde,
 * og et grønt eller blått tema der ville skurret mot kunsten. Spillskjermen har en
 * nøytral mørk bakgrunn (0xFF1E1E1E) og tåler derfor hvilken som helst aksentfarge.
 *
 * De tre rollene svarer til de tre aksentfargene som faktisk fantes i GameScreen fra før:
 *  - [outline]: rammer og rutenett (den "rosa outlinen")
 *  - [outlinePressed]: kanten på Undo mens den holdes inne, en dypere variant
 *  - [button]: fyllet på Main Menu-knappen i vinner-dialogen, en mettet variant
 */
data class GameColors(
    val id: String,
    val displayName: String,
    val outline: Color,
    val outlinePressed: Color,
    val button: Color
)

// Alfaverdien 0xEB på outline er arvet fra den opprinnelige rosa fargen - rammene er
// litt gjennomskinnelige, slik at de legger seg over bakgrunnen i stedet for å skjære
// gjennom den.
val PinkGameColors = GameColors(
    id = "pink",
    displayName = "Pink",
    outline = Color(0xEBF148E8),
    outlinePressed = Color(0xFFB2073F),
    button = Color(0xFFFC1E69)
)

val DeepPurpleGameColors = GameColors(
    id = "deep_purple",
    displayName = "Deep Purple",
    outline = Color(0xEB9B5DE5),
    outlinePressed = Color(0xFF5B21B6),
    button = Color(0xFF7C3AED)
)

val EmeraldGameColors = GameColors(
    id = "emerald",
    displayName = "Emerald",
    outline = Color(0xEB10D982),
    outlinePressed = Color(0xFF047857),
    button = Color(0xFF059669)
)

val FireOrangeGameColors = GameColors(
    id = "fire_orange",
    displayName = "Fire Orange",
    outline = Color(0xEBFF7A18),
    outlinePressed = Color(0xFFB23A00),
    button = Color(0xFFEA580C)
)

// Ekte svart. Rammene forsvinner ikke selv om bakgrunnen er mørk: spillerkortene og
// tallknappene er lysegrå (0xFF505050), så en svart kant leser som en skarp skygge mot
// dem. Full alfa her og ikke 0xEB som de andre - en gjennomskinnelig svart ville blitt
// vasket ut av bakgrunnen i stedet for å stå som en ren linje.
val MatteBlackGameColors = GameColors(
    id = "matte_black",
    displayName = "Matte Black",
    outline = Color(0xFF000000),
    // Undo-kanten trenger å skille seg fra den svarte hvilekanten for å gi
    // trykk-tilbakemelding, derfor mørk grafitt og ikke svart på svart.
    outlinePressed = Color(0xFF5A5A5A),
    button = Color(0xFF1A1A1A)
)

val TurquoiseGameColors = GameColors(
    id = "turquoise",
    displayName = "Turquoise",
    outline = Color(0xEB2DE2E2),
    outlinePressed = Color(0xFF0E7490),
    button = Color(0xFF0891B2)
)

val DeepBlueGameColors = GameColors(
    id = "deep_blue",
    displayName = "Deep Blue",
    outline = Color(0xEB4C8DFF),
    outlinePressed = Color(0xFF1E3A8A),
    button = Color(0xFF2563EB)
)

// Rekkefølgen her er rekkefølgen valgene vises i på Options-skjermen.
val allGameColors = listOf(
    PinkGameColors,
    DeepPurpleGameColors,
    EmeraldGameColors,
    FireOrangeGameColors,
    TurquoiseGameColors,
    DeepBlueGameColors,
    MatteBlackGameColors
)

// Slår opp et tema fra lagret id. Faller tilbake på rosa hvis id-en ikke finnes -
// f.eks. hvis et tema fjernes i en senere versjon og noen har det lagret fra før.
fun gameColorsById(id: String?): GameColors =
    allGameColors.firstOrNull { it.id == id } ?: PinkGameColors

// Gjør det valgte temaet tilgjengelig for spillskjermen uten å tre det gjennom hver
// eneste composable som parameter. Settes i DartBudApp.
val LocalGameColors = compositionLocalOf { PinkGameColors }
