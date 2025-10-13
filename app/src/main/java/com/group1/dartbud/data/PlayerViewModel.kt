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

    // Nye StateFlows for å gruppere spillere
    private val _userProfiles = MutableStateFlow<List<PlayerEntity>>(emptyList())
    val userProfiles: StateFlow<List<PlayerEntity>> = _userProfiles.asStateFlow()

    private val _localProfiles = MutableStateFlow<List<PlayerEntity>>(emptyList())
    val localProfiles: StateFlow<List<PlayerEntity>> = _localProfiles.asStateFlow()

    private val _currentGoogleUserId = MutableStateFlow<String?>(null)

    init {
        val playerDao = DartBudDatabase.getDatabase(application).playerDao()
        repository = PlayerRepository(playerDao)

        viewModelScope.launch {
            repository.allPlayers.collect { playerList ->
                _players.value = playerList
            }
        }
    }

    // ===== EKSISTERENDE METODER =====

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

    // ===== NYE METODER for Google Sign-In =====

    /**
     * Sett inn den innloggede brukerens Google User ID
     * Dette triggerer lasting av brukerens profiler
     */
    fun setGoogleUserId(googleUserId: String?) {
        _currentGoogleUserId.value = googleUserId

        if (googleUserId != null) {
            // Last inn brukerens profiler
            viewModelScope.launch {
                repository.getUserProfiles(googleUserId).collect { profiles ->
                    _userProfiles.value = profiles
                }
            }

            // Last inn lokale profiler
            viewModelScope.launch {
                repository.getLocalProfiles().collect { profiles ->
                    _localProfiles.value = profiles
                }
            }
        } else {
            // Hvis utlogget, vis bare lokale profiler
            _userProfiles.value = emptyList()
            viewModelScope.launch {
                repository.getLocalProfiles().collect { profiles ->
                    _localProfiles.value = profiles
                }
            }
        }
    }

    /**
     * Opprett primærprofil for ny Google-bruker
     */
    fun createPrimaryProfileForGoogleUser(
        googleUserId: String,
        displayName: String,
        email: String,
        photoUrl: String? = null
    ) {
        viewModelScope.launch {
            // Sjekk om profil allerede finnes
            if (!repository.hasPrimaryProfile(googleUserId)) {
                repository.createPrimaryProfileForGoogleUser(
                    googleUserId = googleUserId,
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl
                )
            }
        }
    }

    /**
     * Sjekk om bruker har primærprofil
     */
    suspend fun hasPrimaryProfile(googleUserId: String): Boolean {
        return repository.hasPrimaryProfile(googleUserId)
    }

    /**
     * Legg til en ny profil for innlogget bruker
     */
    fun addUserProfile(googleUserId: String, username: String, email: String) {
        viewModelScope.launch {
            repository.insertPlayer(
                PlayerEntity(
                    username = username,
                    userEmail = email,
                    googleUserId = googleUserId,
                    isUserProfile = true,
                    isPrimaryProfile = false
                )
            )
        }
    }

    /**
     * Legg til lokal spiller (ikke innlogget)
     */
    fun addLocalPlayer(username: String) {
        viewModelScope.launch {
            repository.insertPlayer(
                PlayerEntity(
                    username = username,
                    isUserProfile = false
                )
            )
        }
    }
}