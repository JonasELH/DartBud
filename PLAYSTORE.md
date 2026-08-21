# DartBud – Play Store beskrivelse

Appens eget UI er 100 % engelsk, så butikkoppføringen skal også være engelsk
(standardspråk i Play Console: engelsk (USA)). Den engelske versjonen under er
den som faktisk limes inn i Play Console. Den norske teksten er beholdt som
kildetekst/referanse.

## English (Play Console default language)

### Title (max 30 characters)
DartBud – 501 Dart Calculator

### Short description (max 80 characters)
Automatic scoring for 501 dart. Focus on throwing, not doing the math.

### Full description (max 4000 characters)
Stop pausing dart night to do the math in your head — DartBud does it for you.

DartBud is the calculator for anyone playing 501 darts for fun, at home or at the pub. Enter each throw with double and triple buttons, and let the app track the score while you focus on the board.

🎯 HOW IT WORKS
Tap the number you hit, choose 2x for double or 3x for triple — DartBud works out the rest automatically. No stopping the game, no arguing over mental math.

📊 FEATURES
• Automatic scoring for 501 darts
• Double and triple multipliers
• Double In / Double Out rules
• Checkout suggestions as you close in on zero
• Bust detection
• Undo — step all the way back to the first throw
• Stats: average, highest score, darts thrown
• Match history
• Dark and light mode
• Rematch with one tap

👥 PLAYERS
Create local player profiles, or sign in with Google to back up your stats to the cloud.

🔒 PRIVACY
Guests store no data in the cloud. Signed-in users can view and delete their data at any time.

---

## Norsk (kildetekst / referanse)

## Tittel (maks 30 tegn)
DartBud – 501 Dart Kalkulator

## Kort beskrivelse (maks 80 tegn)
Hold tellingen automatisk – fokuser på å kaste piler, ikke hoderegning.

## Lang beskrivelse (maks 4000 tegn)
Slutt med å avbryte dartkvelden for å regne i hodet – DartBud gjør det for deg.

DartBud er kalkulatoren for deg som spiller 501 dart rekreasjonelt, enten det er hjemme eller på puben. Registrer hvert kast med dobbel- og trippelknapper, og la appen holde styr på poengene mens du fokuserer på brettet.

🎯 SLIK FUNGERER DET
Trykk på tallene for kastet ditt, velg 2x for dobbel eller 3x for trippel – DartBud regner ut resten automatisk. Ingen avbrytelser, ingen krangel om hoderegning.

📊 FUNKSJONER
• Automatisk poengregning for 501 dart
• Dobbel- og trippelmultiplikator
• Double In / Double Out regler
• Checkout-forslag når du nærmer deg slutten
• Bust-deteksjon
• Undo – angre helt tilbake til første kast
• Statistikk: gjennomsnitt, høyeste score, piler kastet
• Spillhistorikk
• Mørk og lys modus
• Rematch med ett trykk

👥 SPILLERE
Opprett lokale spillerprofiler, eller logg inn med Google for å sikkerhetskopiere statistikken din.

🔒 PERSONVERN
Gjestebrukere lagrer ingen data i skyen. Innloggede brukere kan se og slette sine data når som helst.

<!--
NB til fremtidige endringer: ikke skriv at statistikk eller historikk følger
brukeren "på tvers av enheter". Innlogget spill speiles til Firestore, men
ingenting leser det tilbake - getUserGames i FirestoreRepository kalles ikke fra
noe sted, og historikken i appen vises fra lokal Room-database. Logger man inn på
en ny telefon, er historikken tom. Firestore er altså en sikkerhetskopi, ikke en
synkronisering. En reell toveis-synk krever først at gameId slutter å være Room
sin autoinkrementerte ID, som kolliderer mellom enheter.
-->

