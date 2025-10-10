package com.group1.dartbud.data

import kotlinx.coroutines.flow.Flow

class PlayerRepository(private val playerDao: PlayerDao) {

    val allPlayers: Flow<List<PlayerEntity>> = playerDao.getAllPlayers()

    suspend fun getPlayerById(id: Int): PlayerEntity? {
        return playerDao.getPlayerById(id)
    }

    suspend fun getPlayerByUsername(username: String): PlayerEntity? {
        return playerDao.getPlayerByUsername(username)
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
}