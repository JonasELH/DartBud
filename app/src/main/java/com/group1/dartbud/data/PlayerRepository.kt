package com.group1.dartbud.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class PlayerRepository(private val playerDao: PlayerDao) {

    private val firestore = Firebase.firestore
    private val playersCollection = firestore.collection("players")

    val allPlayers: Flow<List<PlayerEntity>> = playerDao.getAllPlayers()

    suspend fun getPlayerById(id: Int): PlayerEntity? {
        return playerDao.getPlayerById(id)
    }

    suspend fun getPlayerByUsername(username: String): PlayerEntity? {
        return playerDao.getPlayerByUsername(username)
    }

    suspend fun insertPlayer(player: PlayerEntity): Long {
        // Save to Firestore first
        playersCollection.document(player.username).set(player).await()
        // Then save to the local Room database
        return playerDao.insertPlayer(player)
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        // Update in Firestore first
        playersCollection.document(player.username).set(player).await()
        // Then update in the local Room database
        playerDao.updatePlayer(player)
    }

    suspend fun deletePlayer(player: PlayerEntity) {
        // Delete from Firestore first
        playersCollection.document(player.username).delete().await()
        // Then delete from the local Room database
        playerDao.deletePlayer(player)
    }

    suspend fun deletePlayerById(id: Int) {
        val player = getPlayerById(id)
        player?.let {
            deletePlayer(it)
        }
    }

    fun listenForPlayerUpdates(googleUserId: String) {
        playersCollection.whereEqualTo("googleUserId", googleUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != nil) {
                    // Handle error, maybe log it
                    return@addSnapshotListener
                }

                val players = snapshots?.toObjects(PlayerEntity::class.java)
                players?.forEach { player ->
                    // This will insert or update the player in the local database
                    // Note: This needs to run in a coroutine
                    // GlobalScope.launch { playerDao.insertPlayer(player) } is one option,
                    // but ideally you'd use a scope from your ViewModel or Application.
                }
            }
    }


    // NYE METODER for Google Sign-In

    fun getPlayersByGoogleUserId(googleUserId: String): Flow<List<PlayerEntity>> {
        return playerDao.getPlayersByGoogleUserId(googleUserId)
    }

    suspend fun getPrimaryProfileByGoogleUserId(googleUserId: String): PlayerEntity? {
        return playerDao.getPrimaryProfileByGoogleUserId(googleUserId)
    }

    fun getUserProfiles(googleUserId: String): Flow<List<PlayerEntity>> {
        return playerDao.getUserProfiles(googleUserId)
    }

    fun getLocalProfiles(): Flow<List<PlayerEntity>> {
        return playerDao.getLocalProfiles()
    }

    /**
     * Opprett primærprofil for en ny Google-bruker
     */
    suspend fun createPrimaryProfileForGoogleUser(
        googleUserId: String,
        displayName: String,
        email: String,
        photoUrl: String? = null
    ): Long {
        val profile = PlayerEntity(
            username = displayName,
            userEmail = email,
            googleUserId = googleUserId,
            isUserProfile = true,
            isPrimaryProfile = true,
            photoUrl = photoUrl
        )
        return insertPlayer(profile)
    }

    /**
     * Sjekk om bruker allerede har en primærprofil
     */
    suspend fun hasPrimaryProfile(googleUserId: String): Boolean {
        return getPrimaryProfileByGoogleUserId(googleUserId) != null
    }
}
