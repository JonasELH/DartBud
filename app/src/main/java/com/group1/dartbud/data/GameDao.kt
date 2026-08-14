package com.group1.dartbud.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for tabellen "games". Håndterer lesing/skriving av ferdigspilte 501-spill.
 */
@Dao
interface GameDao {
    // Flow gjør at UI/ViewModel automatisk får oppdatert liste når tabellen endres,
    // uten å måtte spørre på nytt manuelt. Nyeste spill vises først.
    @Query("SELECT * FROM games ORDER BY datePlayed DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE gameId = :id")
    suspend fun getGameById(id: Int): GameEntity?

    // Spilleren kan være enten player1 eller player2, derfor OR i WHERE-klausulen.
    @Query("SELECT * FROM games WHERE player1Id = :playerId OR player2Id = :playerId ORDER BY datePlayed DESC")
    fun getGamesByPlayer(playerId: Int): Flow<List<GameEntity>>

    // Returnerer den autogenererte gameId-en til den nye raden, brukes til å knytte
    // GameStatsEntity-rader til riktig spill etterpå.
    @Insert
    suspend fun insertGame(game: GameEntity): Long

    @Delete
    suspend fun deleteGame(game: GameEntity)
}