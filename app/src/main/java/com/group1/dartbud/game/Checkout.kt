package com.group1.dartbud.game

/**
 * Checkout-forslag: hvilken kombinasjon av kast som tar spilleren fra gjenstaende
 * score til null. Ligger her og ikke i GameScreen slik at tabellen kan testes
 * automatisk (se CheckoutTest) - den er handskrevet og lett a skrive feil i.
 */

// Slår opp anbefalt utgangs-kombinasjon (checkout) for en gjenstående score, vist
// på PlayerCard slik at spilleren ser hvordan de kan avslutte kampen. Tabellen dekker
// alle scorer som faktisk er mulig å fullføre på 1-3 kast (maks er 170 = T20 T20 Bull).
// 2..40 (partall) dekkes generisk med "D{score/2}" siden alle disse er en ren double.
fun calculateCheckout(score: Int): String {
    return when (score) {
        170 -> "T20 T20 Bull"
        167 -> "T20 T19 Bull"
        164 -> "T20 T18 Bull"
        161 -> "T20 T17 Bull"
        160 -> "T20 T20 D20"
        159 -> "No out shot"
        158 -> "T20 T20 D19"
        157 -> "T20 T19 D20"
        156 -> "T20 T20 D18"
        155 -> "T20 T19 D19"
        154 -> "T20 T18 D20"
        153 -> "T20 T19 D18"
        152 -> "T20 T20 D16"
        151 -> "T20 T17 D20"
        150 -> "T20 T18 D18"
        149 -> "T20 T19 D16"
        148 -> "T20 T16 D20"
        147 -> "T20 T17 D18"
        146 -> "T20 T18 D16"
        145 -> "T20 T15 D20"
        144 -> "T20 T20 D12"
        143 -> "T20 T17 D16"
        142 -> "T20 T14 D20"
        141 -> "T20 T19 D12"
        140 -> "T20 T16 D16"
        139 -> "T19 T14 D20"
        138 -> "T20 T18 D12"
        137 -> "T19 T16 D16"
        136 -> "T20 T20 D8"
        135 -> "T20 T17 D12"
        134 -> "T20 T14 D16"
        133 -> "T20 T19 D8"
        132 -> "Bull Bull D16"
        131 -> "T20 T13 D16"
        130 -> "T20 20 Bull"
        129 -> "T19 T16 D12"
        128 -> "T18 T14 D16"
        127 -> "T20 T17 D8"
        126 -> "T19 T19 D6"
        125 -> "25 T20 D20"
        124 -> "T20 T16 D8"
        123 -> "T19 T16 D9"
        122 -> "T18 T20 D4"
        121 -> "T17 T10 D20"
        120 -> "T20 20 D20"
        119 -> "T19 T10 D16"
        118 -> "T20 18 D20"
        117 -> "T20 17 D20"
        116 -> "T20 16 D20"
        115 -> "T20 15 D20"
        114 -> "T20 14 D20"
        113 -> "T20 13 D20"
        112 -> "T20 12 D20"
        111 -> "T20 19 D16"
        110 -> "T20 Bull"
        109 -> "T19 20 D16"
        108 -> "T20 16 D16"
        107 -> "T19 Bull"
        106 -> "T20 14 D16"
        105 -> "T19 16 D16"
        104 -> "T18 Bull"
        103 -> "T20 3 D20"
        102 -> "T20 10 D16"
        101 -> "T17 Bull"
        100 -> "T20 D20"
        99 -> "T19 10 D16"
        98 -> "T20 D19"
        97 -> "T19 D20"
        96 -> "T20 D18"
        95 -> "T19 D19"
        94 -> "T18 D20"
        93 -> "T19 D18"
        92 -> "T20 D16"
        91 -> "T17 D20"
        90 -> "T20 D15"
        89 -> "T19 D16"
        88 -> "T16 D20"
        87 -> "T17 D18"
        86 -> "T18 D16"
        85 -> "T15 D20"
        84 -> "T20 D12"
        83 -> "T17 D16"
        82 -> "T14 D20"
        81 -> "T19 D12"
        80 -> "T20 D10"
        79 -> "T13 D20"
        78 -> "T18 D12"
        77 -> "T19 D10"
        76 -> "T20 D8"
        75 -> "T17 D12"
        74 -> "T14 D16"
        73 -> "T19 D8"
        72 -> "T16 D12"
        71 -> "T13 D16"
        70 -> "T10 D20"
        69 -> "T15 D12"
        68 -> "T20 D4"
        67 -> "T17 D8"
        66 -> "T10 D18"
        65 -> "T19 D4"
        64 -> "T16 D8"
        63 -> "T13 D12"
        62 -> "T10 D16"
        61 -> "T15 D8"
        60 -> "20 D20"
        in 2..40 step 2 -> "D${score / 2}"
        50 -> "Bull"
        // Disse scorene kan ikke fullføres på noen lovlig måte (for høye, eller ikke
        // nåbare med en gyldig kombinasjon av kast som ender på en double)
        else -> if (score > 170 || score == 169 || score == 168 || score == 166 ||
            score == 165 || score == 163 || score == 162 || score == 159) {
            "No out shot"
        } else {
            // Tabellen over er eksplisitt ned til 60 og dekker deretter kun partall
            // 2..40. Alt mellom 41 og 59, og alle oddetall under 40, falt tidligere
            // ned hit og ga tom streng - altså ingen checkout-hjelp på svært vanlige
            // utganger som 41, 44 eller 57. Regnes nå ut generisk i stedet.
            genericCheckout(score)
        }
    }
}

// Rekkefølgen doublene prøves i når vi leter etter alternative checkout-ruter - de mest
// brukte/foretrukne doblene først (D20, D16, D12, D8 osv.), pluss Bull (=grønn bull,
// teller som double-out) som egen utgangsdouble.
private val preferredCheckoutDoubles = listOf(40, 32, 16, 8, 20, 24, 36, 4, 12, 28, 2, 6, 10, 14, 18, 22, 26, 30, 34, 38, 50)
private fun checkoutDoubleLabel(target: Int) = if (target == 50) "Bull" else "D${target / 2}"

// "Store" scoringspiler - tripler og bull - sortert med de mest trente/høyeste verdiene
// først, siden det er slik spillere faktisk velger rute (T20 og Bull er standardkastene).
private val bigScoringDarts: List<Pair<Int, String>> =
    listOf(60 to "T20", 50 to "Bull", 57 to "T19", 54 to "T18", 51 to "T17", 48 to "T16",
        45 to "T15", 42 to "T14", 39 to "T13", 36 to "T12", 33 to "T11", 30 to "T10",
        27 to "T9", 24 to "T8", 21 to "T7", 18 to "T6", 15 to "T5", 12 to "T4",
        9 to "T3", 6 to "T2", 3 to "T1")

// "Små" scoringspiler - enkeltfelt 1-20, pluss ytre bull (25).
private val smallScoringDarts: List<Pair<Int, String>> =
    listOf(25 to "25") + (20 downTo 1).map { it to "$it" }

private val allScoringDarts = bigScoringDarts + smallScoringDarts

// Håndplukkede ekstra-ruter for enkeltscorer, lagt inn rett etter hovedforslaget.
//
// Generatoren under prøver bare tripler og indre bull (50) som "store" førstepiler, og
// finner derfor aldri ruter som åpner med ytre bull (25). 121 er det tydeligste
// tilfellet: 25 først etterlater 96, som er T20 D18 - altså én trippel i stedet for de
// to som hovedforslaget T17 T10 D20 krever.
private val curatedCheckoutAlternatives: Map<Int, List<String>> = mapOf(
    121 to listOf("25 T20 D18")
)

// Genererer alternative gyldige checkout-ruter for en score, i tillegg til forslaget fra
// calculateCheckout(). Brukes av "veksle mellom utganger"-knappen i spillskjermen.
//
// Går gjennom doublene i preferredCheckoutDoubles - altså de spillere faktisk liker å
// avslutte på - og prøver for hver av dem å finne én eller to scoringspiler (trippel,
// bull, enkeltfelt) som til sammen tar resten av scoren. Dette speiler hvordan ekte
// checkout-tabeller er bygget opp (velg en trygg double, finn så veien dit), og gjør at
// kjente ruter som "T20 T16 D12" for 132 faktisk dukker opp, i stedet for hvilken som
// helst matematisk gyldig men lite brukt kombinasjon.
//
// Returnerer bare [primary] (ett element) hvis scoren ikke er mulig å avslutte på, eller
// hvis det ikke finnes noen reell alternativ rute.
fun calculateCheckoutAlternatives(score: Int, maxAlternatives: Int = 4): List<String> {
    val primary = calculateCheckout(score)
    if (primary == "No out shot" || primary.isBlank()) return listOf(primary)

    val alternatives = linkedSetOf(primary)
    // Legges inn før de genererte, slik at den håndplukkede ruten er det første man
    // får se når man blar med veksle-knappen.
    curatedCheckoutAlternatives[score]?.let { alternatives.addAll(it) }

    fun isFull() = alternatives.size > maxAlternatives

    for (target in preferredCheckoutDoubles) {
        if (isFull()) break
        val remainder = score - target
        if (remainder <= 0) continue

        // Ett kast igjen etter doublen: finnes det en scoringspil verdt nøyaktig remainder?
        allScoringDarts.firstOrNull { it.first == remainder }?.let { (_, label) ->
            alternatives.add("$label ${checkoutDoubleLabel(target)}")
        }
        if (isFull()) break

        // To kast igjen etter doublen: finn en stor pil + en (stor eller liten) pil som
        // til sammen gir remainder.
        outer@ for ((v1, l1) in bigScoringDarts) {
            if (v1 >= remainder) continue
            for ((v2, l2) in allScoringDarts) {
                if (v1 + v2 == remainder) {
                    alternatives.add("$l1 $l2 ${checkoutDoubleLabel(target)}")
                    break@outer
                }
            }
        }
    }

    return alternatives.take(maxAlternatives + 1).toList()
}

// Regner ut en to-kast-utgang for scorer som ikke står i den håndskrevne
// checkout-tabellen: finn et enkeltkast (1-20) som etterlater en gyldig double.
// Doublene prøves i den rekkefølgen spillere faktisk sikter på dem - D16 og D20
// først, siden de gir best "restsjanse" hvis man bommer.
fun genericCheckout(score: Int): String {
    if (score in 2..40 && score % 2 == 0) return "D${score / 2}"

    val preferredDoubles = listOf(32, 40, 16, 8, 20, 24, 36, 4, 12, 28, 2, 6, 10, 14, 18, 22, 26, 30, 34, 38)
    for (target in preferredDoubles) {
        val firstDart = score - target
        if (firstDart in 1..20) return "$firstDart D${target / 2}"
    }
    return ""
}
