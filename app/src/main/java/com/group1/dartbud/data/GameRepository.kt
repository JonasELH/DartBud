package com.group1.dartbud.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GameRepository(
    private val gameDao: GameDao,
    private val gameStatsDao: GameStatsDao,
    private val coroutineScope: CoroutineScope
) {

    private val firestore = Firebase.firestore
    private val gamesCollection = firestore.collection("games")
    private val gameStatsCollection = firestore.collection("game_stats")

    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    init {
        listenForGameUpdates()
        listenForGameStatsUpdates()
    }

    private fun listenForGameUpdates() {
        gamesCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                return@addSnapshotListener
            }

            snapshots?.toObjects(GameEntity::class.java)?.forEach { game ->
                coroutineScope.launch {
                    gameDao.insertGame(game) // This will insert or update
                }
            }
        }
    }

    private fun listenForGameStatsUpdates() {
        gameStatsCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                return@addSnapshotListener
            }

            snapshots?.toObjects(GameStatsEntity::class.java)?.forEach { stats ->
                coroutineScope.launch {
                    gameStatsDao.insertStats(stats) // This will insert or update
                }
            }
        }
    }


    suspend fun getGameById(id: Int): GameEntity? {
        return gameDao.getGameById(id)
    }

    fun getGamesByPlayer(playerId: Int): Flow<List<GameEntity>> {
        return gameDao.getGamesByPlayer(playerId)
    }

    suspend fun insertGame(game: GameEntity): Long {
        val gameId = gameDao.insertGame(game)
        val gameWithId = game.copy(gameId = gameId.toInt())
        gamesCollection.document(gameId.toString()).set(gameWithId).await()
        return gameId
    }

    suspend fun deleteGame(game: GameEntity) {
        // Delete game from Firestore
        gamesCollection.document(game.gameId.toString()).delete().await()

        // Delete associated stats from Firestore
        val statsDoc1 = "${game.gameId}_${game.player1Id}"
        val statsDoc2 = "${game.gameId}_${game.player2Id}"
        gameStatsCollection.document(statsDoc1).delete().await()
        gameStatsCollection.document(statsDoc2).delete().await()

        // Delete from local Room database
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
        val docId = "${stats.gameId}_${stats.playerId}"
        gameStatsCollection.document(docId).set(stats).await()
        gameStatsDao.insertStats(stats)
    }
}
