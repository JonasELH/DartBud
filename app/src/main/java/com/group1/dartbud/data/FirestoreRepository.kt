package com.group1.dartbud.data


import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Firestore-representasjonen av en spillerprofil. Lagres under users/{userId}/profiles/{profileId}.
// Speiler PlayerEntity, men er en egen modell fordi Firestore trenger et objekt med
// tomme default-verdier (for doc.toObject) og fordi feltene som synkroniseres til skyen
// ikke nødvendigvis er identiske med Room-kolonnene.
data class FirestorePlayerProfile(
    val profileId: String = "",
    val username: String = "",
    val email: String = "",
    // Firestore utleder feltnavnet i dokumentet fra getteren, og for en Boolean
    // som heter isPrimaryProfile blir getteren isPrimaryProfile() -> feltnavnet
    // "primaryProfile". Ved lesing (toObject) leter mapperen etter et felt som
    // heter "primaryProfile", finner ingen (backing-feltet heter isPrimaryProfile),
    // logger "No setter/field for primaryProfile found" og lar verdien stå som
    // default false - primærprofil-flagget gikk tapt ved synk fra skyen.
    // @field:PropertyName knytter backing-feltet til samme navn som getteren
    // allerede skriver, slik at dokumenter som allerede ligger i Firestore med
    // nøkkelen "primaryProfile" leses riktig, og skrivingen er uendret.
    @field:PropertyName("primaryProfile")
    val isPrimaryProfile: Boolean = false,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// Firestore-representasjonen av et ferdigspilt spill, lagret under
// users/{userId}/games/{gameId}. Denormalisert (inkluderer navn, ikke bare ID-er,
// og begge spilleres statistikk flatt på selve dokumentet) slik at spillhistorikk
// kan vises uten ekstra oppslag mot spillerdokumenter.
data class FirestoreGame(
    val gameId: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    val player1Name: String = "",
    val player2Name: String = "",
    val winnerId: String = "",
    val winnerName: String = "",
    val doubleIn: Boolean = false,
    val doubleOut: Boolean = true,
    // Kampformat (best av 1/3/5/7/9 legs) og legs-stillingen ved kampslutt.
    // totalLegsInMatch = 1 for kamper spilt for denne funksjonen fantes.
    val player1LegsWon: Int = 0,
    val player2LegsWon: Int = 0,
    val totalLegsInMatch: Int = 1,
    val player1Average: Double = 0.0,
    val player2Average: Double = 0.0,
    val player1HighestScore: Int = 0,
    val player2HighestScore: Int = 0,
    val player1DartsThrown: Int = 0,
    val player2DartsThrown: Int = 0,
    val player1RoundsPlayed: Int = 0,
    val player2RoundsPlayed: Int = 0,
    val player1FinalScore: Int = 0,
    val player2FinalScore: Int = 0,
    val playedAt: Long = System.currentTimeMillis()
)

/**
 * Håndterer all lesing/skriving mot Firestore (skylagring), organisert per bruker
 * under users/{userId}/... Dette er sky-motstykket til Room-lagene (Player/GameRepository)
 * og brukes kun for innloggede Google-brukere.
 *
 * Alle suspend-funksjoner returnerer [Result] i stedet for å kaste unntak, slik at
 * kalleren (ViewModel) kan bruke onSuccess/onFailure uten try/catch rundt hvert kall.
 */
class FirestoreRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // SPILLERE

    // Lagrer (eller overskriver) én profil under brukerens profils-subcollection.
    // .set() erstatter hele dokumentet, så dette dekker både "opprett ny" og "oppdater alt".
    suspend fun saveUserProfile(
        userId: String,
        profile: FirestorePlayerProfile
    ): Result<Unit> {
        return try {
            firestore
                .collection("users")
                .document(userId)
                .collection("profiles")
                .document(profile.profileId)
                .set(profile)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Henter alle profiler for en bruker i ett engangs-kall (ikke live). Brukes til
    // engangs-synkronisering fra Firestore til Room (se PlayerViewModel.syncFromFirestore).
    suspend fun getUserProfiles(userId: String): Result<List<FirestorePlayerProfile>> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("profiles")
                .get()
                .await()

            // mapNotNull hopper stille over dokumenter som ikke lar seg deserialisere
            // til FirestorePlayerProfile, i stedet for å kaste feil for hele listen.
            val profiles = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirestorePlayerProfile::class.java)
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Oppdaterer kun brukernavn-feltet (.update med ett felt), i motsetning til
    // saveUserProfile som skriver over hele dokumentet.
    suspend fun updateUserProfile(
        userId: String,
        profileId: String,
        username: String
    ): Result<Unit> {
        return try {
            firestore
                .collection("users")
                .document(userId)
                .collection("profiles")
                .document(profileId)
                .update("username", username)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserProfile(
        userId: String,
        profileId: String
    ): Result<Unit> {
        return try {
            firestore
                .collection("users")
                .document(userId)
                .collection("profiles")
                .document(profileId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // SPILL

    // Lagrer et ferdigspilt spill i skyen for den innloggede brukeren, kalt fra
    // GameViewModel.saveGame etter at spillet er lagret lokalt i Room.
    suspend fun saveGame(
        userId: String,
        game: FirestoreGame
    ): Result<Unit> {
        return try {
            firestore
                .collection("users")
                .document(userId)
                .collection("games")
                .document(game.gameId)
                .set(game)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Henter brukerens spillhistorikk fra skyen, nyeste først.
    suspend fun getUserGames(userId: String): Result<List<FirestoreGame>> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("games")
                .orderBy("playedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val games = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirestoreGame::class.java)
            }
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sletter ALT en bruker har i Firestore (profiler, spill, og brukerdokumentet selv).
    // Brukes typisk ved kontosletting. Firestore har ikke rekursiv sletting av
    // subcollections innebygd, så hvert dokument i profiles/games må hentes og
    // slettes enkeltvis før foreldredokumentet kan slettes.
    suspend fun deleteAllUserData(userId: String): Result<Unit> {
        return try {
            val profilesSnapshot = firestore
                .collection("users")
                .document(userId)
                .collection("profiles")
                .get()
                .await()
            profilesSnapshot.documents.forEach { it.reference.delete().await() }

            val gamesSnapshot = firestore
                .collection("users")
                .document(userId)
                .collection("games")
                .get()
                .await()
            gamesSnapshot.documents.forEach { it.reference.delete().await() }

            firestore.collection("users").document(userId).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // REAL-TIME LISTENERS (valgfritt)

    // callbackFlow bygger en Flow rundt Firestores callback-baserte snapshot-listener,
    // slik at man kan collecte live-oppdateringer med vanlig Flow-syntaks. Listeneren
    // sender ny liste hver gang dataene endres i skyen (i motsetning til getUserProfiles
    // som bare henter én gang). awaitClose fjerner listeneren når Flow-collectoren
    // avsluttes, så vi unngår at den fortsetter å lytte etter at ingen bruker den lenger.
    fun getUserProfilesFlow(userId: String): Flow<List<FirestorePlayerProfile>> = callbackFlow {
        val registration = firestore
            .collection("users")
            .document(userId)
            .collection("profiles")
            .addSnapshotListener { snapshot, error ->
                // Feil ved lytting ignoreres stille (ingen verdi sendes videre denne gangen).
                if (error != null) return@addSnapshotListener

                val profiles = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirestorePlayerProfile::class.java)
                } ?: emptyList()

                trySend(profiles)
            }

        awaitClose { registration.remove() }
    }
}