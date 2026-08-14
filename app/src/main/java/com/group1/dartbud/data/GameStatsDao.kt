package com.group1.dartbud.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for tabellen "game_stats". Hver rad representerer én spillers statistikk
 * for ett spesifikt spill (snitt, høyeste score, antall kast osv.).
 */
@Dao
interface GameStatsDao {
    // Henter begge spilleres stats-rader for ett spill (brukes til f.eks. resultatvisning).
    @Query("SELECT * FROM game_stats WHERE gameId = :gameId")
    suspend fun getStatsByGame(gameId: Int): List<GameStatsEntity>

    // All historisk statistikk for én spiller på tvers av spill, som Flow slik at
    // f.eks. en statistikkskjerm oppdateres automatisk etter nye spill.
    @Query("SELECT * FROM game_stats WHERE playerId = :playerId")
    fun getStatsByPlayer(playerId: Int): Flow<List<GameStatsEntity>>

    @Insert
    suspend fun insertStats(stats: GameStatsEntity)

    @Delete
    suspend fun deleteStats(stats: GameStatsEntity)
}