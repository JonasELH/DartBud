package com.group1.dartbud.data

import kotlinx.coroutines.flow.Flow

class GameRepository(
    private val gameDao: GameDao,
    private val gameStatsDao: GameStatsDao
) {
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