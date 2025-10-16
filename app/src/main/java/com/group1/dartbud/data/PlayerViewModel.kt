package com.group1.dartbud.viewmodel

import com.group1.dartbud.data.FirestoreRepository
import com.group1.dartbud.data.FirestorePlayerProfile
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
    private val firestoreRepository = FirestoreRepository()

    private val _players = MutableStateFlow<List<PlayerEntity>>(emptyList())
    val players: StateFlow<List<PlayerEntity>> = _players.asStateFlow()

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

    // ===== GOOGLE SIGN-IN MED FIRESTORE SYNKRONISERING =====

    fun setGoogleUserId(googleUserId: String?) {
        _currentGoogleUserId.value = googleUserId

        if (googleUserId != null) {
            // Synkroniser fra Firestore til Room
            syncFromFirestore(googleUserId)

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

    private fun syncFromFirestore(userId: String) {
        viewModelScope.launch {
            val result = firestoreRepository.getUserProfiles(userId)
            result.onSuccess { firestoreProfiles ->
                // Oppdater Room med data fra Firestore
                firestoreProfiles.forEach { fsProfile ->
                    val existingPlayer = repository.getPlayerByUsername(fsProfile.username)

                    if (existingPlayer == null) {
                        // Opprett ny spiller i Room
                        val newPlayer = PlayerEntity(
                            playerId = 0,
                            username = fsProfile.username,
                            userEmail = fsProfile.email,
                            googleUserId = userId,
                            isUserProfile = true,
                            isPrimaryProfile = fsProfile.isPrimaryProfile,
                            photoUrl = fsProfile.photoUrl
                        )
                        repository.insertPlayer(newPlayer)
                    }
                }
            }
        }
    }

    fun createPrimaryProfileForGoogleUser(
        googleUserId: String,
        displayName: String,
        email: String,
        photoUrl: String? = null
    ) {
        viewModelScope.launch {
            if (!repository.hasPrimaryProfile(googleUserId)) {
                // Lagre i Room
                repository.createPrimaryProfileForGoogleUser(
                    googleUserId = googleUserId,
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl
                )

                // Lagre i Firestore
                val firestoreProfile = FirestorePlayerProfile(
                    profileId = googleUserId, // Bruker googleUserId som profileId for primærprofil
                    username = displayName,
                    email = email,
                    isPrimaryProfile = true,
                    photoUrl = photoUrl
                )
                firestoreRepository.saveUserProfile(googleUserId, firestoreProfile)
            }
        }
    }

    suspend fun hasPrimaryProfile(googleUserId: String): Boolean {
        return repository.hasPrimaryProfile(googleUserId)
    }

    fun addUserProfile(googleUserId: String, username: String, email: String) {
        viewModelScope.launch {
            // Lagre i Room
            val newPlayer = PlayerEntity(
                username = username,
                userEmail = email,
                googleUserId = googleUserId,
                isUserProfile = true,
                isPrimaryProfile = false
            )
            val insertedId = repository.insertPlayer(newPlayer)

            // Lagre i Firestore
            val firestoreProfile = FirestorePlayerProfile(
                profileId = insertedId.toString(),
                username = username,
                email = email,
                isPrimaryProfile = false,
                photoUrl = null
            )
            firestoreRepository.saveUserProfile(googleUserId, firestoreProfile)
        }
    }

    fun addLocalPlayer(username: String) {
        viewModelScope.launch {
            repository.insertPlayer(
                PlayerEntity(
                    username = username,
                    isUserProfile = false
                )
            )
            // Lokale spillere lagres IKKE i Firestore
        }
    }

    // ===== GENERELLE METODER =====

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

            // Hvis det er en Google-profil, oppdater også i Firestore
            if (player.googleUserId != null) {
                firestoreRepository.updateUserProfile(
                    userId = player.googleUserId,
                    profileId = player.playerId.toString(),
                    username = player.username
                )
            }
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.deletePlayer(player)

            // Hvis det er en Google-profil, slett også fra Firestore
            if (player.googleUserId != null) {
                firestoreRepository.deleteUserProfile(
                    userId = player.googleUserId,
                    profileId = player.playerId.toString()
                )
            }
        }
    }

    fun deletePlayerByUsername(username: String) {
        viewModelScope.launch {
            val player = repository.getPlayerByUsername(username)
            player?.let {
                deletePlayer(it)
            }
        }
    }
}