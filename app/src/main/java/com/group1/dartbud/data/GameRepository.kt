package com.group1.dartbud.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository-lag mellom [GameViewModel] og Room. Samler tilgang til både spill (games)
 * og spillstatistikk (game_stats) ett sted, slik at ViewModel ikke trenger å kjenne
 * til to separate DAO-er direkte.
 */
class GameRepository(
    private val gameDao: GameDao,
    private val gameStatsDao: GameStatsDao
) {
    // Eksponeres direkte som Flow slik at ViewModel kan collecte og holde UI oppdatert
    // automatisk når spill legges til/fjernes i Room.
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    suspend fun getGameById(id: Int): GameEntity? {
        return gameDao.getGameById(id)
    }

    fun getGamesByPlayer(playerId: Int): Flow<List<GameEntity>> {
        return gameDao.getGamesByPlayer(playerId)
    }

    suspend fun insertGame(game: GameEntity): Long {
        return gameDao.insertGame(game)
    }

    suspend fun deleteGame(game: GameEntity) {
        gameDao.deleteGame(game)
    }

    // Stats methods
    suspend fun getStatsByGame(gameId: Int): List<GameStatsEntity> {
        return gameStatsDao.getStatsByGame(gameId)
    }

    fun getStatsByPlayer(playerId: Int): Flow<List<GameStatsEntity>> {
        return gameStatsDao.getStatsByPlayer(playerId)
    }

    suspend fun insertStats(stats: GameStatsEntity) {
        gameStatsDao.insertStats(stats)
    }
}