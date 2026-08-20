package com.group1.dartbud.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository-lag mellom [PlayerViewModel] og Room for spillerdata. Kapsler inn
 * [PlayerDao] slik at ViewModel jobber mot enklere metodenavn i stedet for DAO-en
 * direkte. Firestore-synkronisering håndteres separat i ViewModel via [FirestoreRepository].
 */
class PlayerRepository(private val playerDao: PlayerDao) {

    // Alle spillere i Room, som Flow slik at UI oppdateres automatisk ved endringer.
    val allPlayers: Flow<List<PlayerEntity>> = playerDao.getAllPlayers()

    suspend fun getPlayerById(id: Int): PlayerEntity? {
        return playerDao.getPlayerById(id)
    }

    suspend fun getPlayerByUsername(username: String): PlayerEntity? {
        return playerDao.getPlayerByUsername(username)
    }

    suspend fun getPlayerByUsernameForGoogleUser(username: String, googleUserId: String): PlayerEntity? {
        return playerDao.getPlayerByUsernameForGoogleUser(username, googleUserId)
    }

    suspend fun getLocalPlayerByUsername(username: String): PlayerEntity? {
        return playerDao.getLocalPlayerByUsername(username)
    }

    suspend fun insertPlayer(player: PlayerEntity): Long {
        return playerDao.insertPlayer(player)
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        playerDao.updatePlayer(player)
    }

    suspend fun deletePlayer(player: PlayerEntity) {
        playerDao.deletePlayer(player)
    }

    suspend fun deletePlayerById(id: Int) {
        playerDao.deletePlayerById(id)
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