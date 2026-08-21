# DartBud – Play Store beskrivelse

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

