package com.group1.dartbud.data


import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class FirestorePlayerProfile(
    val profileId: String = "",
    val username: String = "",
    val email: String = "",
    val isPrimaryProfile: Boolean = false,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

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

class FirestoreRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // SPILLERE

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

    suspend fun getUserProfiles(userId: String): Result<List<FirestorePlayerProfile>> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("profiles")
                .get()
                .await()

            val profiles = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirestorePlayerProfile::class.java)
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    fun getUserProfilesFlow(userId: String): Flow<List<FirestorePlayerProfile>> = callbackFlow {
        val registration = firestore
            .collection("users")
            .document(userId)
            .collection("profiles")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val profiles = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirestorePlayerProfile::class.java)
                } ?: emptyList()

                trySend(profiles)
            }

        awaitClose { registration.remove() }
    }
}