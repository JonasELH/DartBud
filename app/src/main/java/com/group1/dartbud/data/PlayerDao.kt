package com.group1.dartbud.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY username ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE playerId = :id")
    suspend fun getPlayerById(id: Int): PlayerEntity?

    @Query("SELECT * FROM players WHERE username = :username LIMIT 1")
    suspend fun getPlayerByUsername(username: String): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    @Query("DELETE FROM players WHERE playerId = :id")
    suspend fun deletePlayerById(id: Int)

    // NYE QUERIES for Google Sign-In

    @Query("SELECT * FROM players WHERE googleUserId = :googleUserId")
    fun getPlayersByGoogleUserId(googleUserId: String): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE googleUserId = :googleUserId AND isPrimaryProfile = 1 LIMIT 1")
    suspend fun getPrimaryProfileByGoogleUserId(googleUserId: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE isUserProfile = 1 AND googleUserId = :googleUserId ORDER BY isPrimaryProfile DESC, username ASC")
    fun getUserProfiles(googleUserId: String): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE isUserProfile = 0 ORDER BY username ASC")
    fun getLocalProfiles(): Flow<List<PlayerEntity>>
}