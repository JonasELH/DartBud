package com.group1.dartbud.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStatsDao {
    @Query("SELECT * FROM game_stats WHERE gameId = :gameId")
    suspend fun getStatsByGame(gameId: Int): List<GameStatsEntity>

    @Query("SELECT * FROM game_stats WHERE playerId = :playerId")
    fun getStatsByPlayer(playerId: Int): Flow<List<GameStatsEntity>>

    @Insert
    suspend fun insertStats(stats: GameStatsEntity)

    @Delete
    suspend fun deleteStats(stats: GameStatsEntity)
}