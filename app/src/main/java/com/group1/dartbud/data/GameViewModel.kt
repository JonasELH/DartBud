package com.group1.dartbud.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.group1.dartbud.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameRepository
    private val _games = MutableStateFlow<List<GameEntity>>(emptyList())
    val games: StateFlow<List<GameEntity>> = _games.asStateFlow()

    init {
        val database = DartBudDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao(), database.gameStatsDao())

        viewModelScope.launch {
            repository.allGames.collect { gameList ->
                _games.value = gameList
            }
        }
    }

    fun saveGame(
        player1Id: Int,
        player2Id: Int,
        winnerId: Int,
        doubleIn: Boolean,
        doubleOut: Boolean,
        player1Stats: GameStatsEntity,
        player2Stats: GameStatsEntity
    ) {
        viewModelScope.launch {
            val game = GameEntity(
                player1Id = player1Id,
                player2Id = player2Id,
                winnerId = winnerId,
                doubleIn = doubleIn,
                doubleOut = doubleOut
            )
            val gameId = repository.insertGame(game)

            // Save stats for both players
            repository.insertStats(player1Stats.copy(gameId = gameId.toInt()))
            repository.insertStats(player2Stats.copy(gameId = gameId.toInt()))
        }
    }
    suspend fun getStatsByGame(gameId: Int): List<GameStatsEntity> {
        return repository.getStatsByGame(gameId)
    }

    suspend fun getStatsByPlayer(playerId: Int): List<GameStatsEntity> {
        var stats: List<GameStatsEntity> = emptyList()
        repository.getStatsByPlayer(playerId).collect {
            stats = it
        }
        return stats
    }
}