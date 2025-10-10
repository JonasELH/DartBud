package com.group1.dartbud.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY datePlayed DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE gameId = :id")
    suspend fun getGameById(id: Int): GameEntity?

    @Query("SELECT * FROM games WHERE player1Id = :playerId OR player2Id = :playerId ORDER BY datePlayed DESC")
    fun getGamesByPlayer(playerId: Int): Flow<List<GameEntity>>

    @Insert
    suspend fun insertGame(game: GameEntity): Long

    @Delete
    suspend fun deleteGame(game: GameEntity)
}