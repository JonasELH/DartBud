package com.group1.dartbud.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.group1.dartbud.data.DartBudDatabase
import com.group1.dartbud.data.PlayerEntity
import com.group1.dartbud.data.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlayerRepository
    private val _players = MutableStateFlow<List<PlayerEntity>>(emptyList())
    val players: StateFlow<List<PlayerEntity>> = _players.asStateFlow()

    init {
        val playerDao = DartBudDatabase.getDatabase(application).playerDao()
        repository = PlayerRepository(playerDao)

        viewModelScope.launch {
            repository.allPlayers.collect { playerList ->
                _players.value = playerList
            }
        }
    }

    fun addPlayer(username: String) {
        viewModelScope.launch {
            val existing = repository.getPlayerByUsername(username)
            if (existing == null) {
                repository.insertPlayer(
                    PlayerEntity(username = username)
                )
            }
        }
    }

    fun updatePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.updatePlayer(player)
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.deletePlayer(player)
        }
    }

    fun deletePlayerByUsername(username: String) {
        viewModelScope.launch {
            val player = repository.getPlayerByUsername(username)
            player?.let {
                repository.deletePlayer(it)
            }
        }
    }
}