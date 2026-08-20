package com.group1.dartbud.viewmodel

import com.group1.dartbud.data.FirestoreRepository
import com.group1.dartbud.data.FirestorePlayerProfile
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.group1.dartbud.data.DartBudDatabase
import com.group1.dartbud.data.GameRepository
import com.group1.dartbud.data.PlayerEntity
import com.group1.dartbud.data.PlayerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel som holder styr på spillerprofiler for UI-laget: alle spillere,
 * profiler tilknyttet en innlogget Google-bruker, og rene lokale profiler.
 *
 * Kombinerer Room (lokal lagring, kilde til sannhet for UI) med Firestore
 * (sky-synkronisering for Google-innloggede brukere). Mønsteret som går igjen:
 * skriv til Room først, og hvis brukeren er innlogget, speil endringen til Firestore.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlayerRepository
    private val gameRepository: GameRepository
    private val firestoreRepository = FirestoreRepository()

    // Alle spillere i Room. Brukes typisk der man trenger hele listen uavhengig
    // av innloggingsstatus.
    private val _players = MutableStateFlow<List<PlayerEntity>>(emptyList())
    val players: StateFlow<List<PlayerEntity>> = _players.asStateFlow()

    // Profiler tilknyttet den innloggede Google-brukeren (primærprofil + evt. underprofiler).
    // Tom liste når ingen er logget inn.
    private val _userProfiles = MutableStateFlow<List<PlayerEntity>>(emptyList())
    val userProfiles: StateFlow<List<PlayerEntity>> = _userProfiles.asStateFlow()

    // Lokale gjesteprofiler (ikke knyttet til noen Google-konto). Vises uavhengig
    // av om noen er innlogget eller ikke.
    private val _localProfiles = MutableStateFlow<List<PlayerEntity>>(emptyList())
    val localProfiles: StateFlow<List<PlayerEntity>> = _localProfiles.asStateFlow()

    private val _currentGoogleUserId = MutableStateFlow<String?>(null)

    // Jobbene som samler inn profil-Flows. Holdes som felt slik at et nytt kall til
    // setGoogleUserId kan kansellere de forrige. Uten dette startet hvert kall enda et
    // sett collectors som aldri ble stoppet - og setGoogleUserId kalles fra en
    // LaunchedEffect i både GameSettingsScreen og ManagePlayersScreen, altså hver gang
    // brukeren åpner en av dem.
    private var userProfilesJob: Job? = null
    private var localProfilesJob: Job? = null
    private var syncJob: Job? = null

    init {
        val database = DartBudDatabase.getDatabase(application)
        repository = PlayerRepository(database.playerDao())
        gameRepository = GameRepository(database.gameDao(), database.gameStatsDao())

        // Følger Room-tabellen kontinuerlig og speiler den til _players,
        // slik at UI (som observerer players) alltid har ferske data.
        viewModelScope.launch {
            repository.allPlayers.collect { playerList ->
                _players.value = playerList
            }
        }
    }

    // ===== GOOGLE SIGN-IN MED FIRESTORE SYNKRONISERING =====

    // Kalles når innloggingsstatus endres (login/logout). Styrer hvilke profil-
    // Flows som samles inn, og trigger synkronisering fra Firestore ved innlogging.
    fun setGoogleUserId(googleUserId: String?) {
        // Ingen endring - ikke start nye collectors eller en ny Firestore-synk unødig.
        if (_currentGoogleUserId.value == googleUserId && localProfilesJob != null) return

        _currentGoogleUserId.value = googleUserId

        // Stopp innsamlingen fra forrige innloggingstilstand før vi starter ny
        userProfilesJob?.cancel()
        localProfilesJob?.cancel()
        syncJob?.cancel()

        // Lokale gjesteprofiler vises uansett om noen er innlogget eller ikke
        localProfilesJob = viewModelScope.launch {
            repository.getLocalProfiles().collect { profiles ->
                _localProfiles.value = profiles
            }
        }

        if (googleUserId != null) {
            syncFromFirestore(googleUserId)

            userProfilesJob = viewModelScope.launch {
                repository.getUserProfiles(googleUserId).collect { profiles ->
                    _userProfiles.value = profiles
                }
            }
        } else {
            _userProfiles.value = emptyList()
        }
    }

    // Henter profiler fra Firestore og "importerer" de som mangler lokalt i Room.
    // Ettpartsvis synk (Firestore -> Room); feil ignoreres stille via Result.onSuccess
    // (onFailure-grenen håndteres ikke, så en feilet sync bare gir tomt resultat).
    private fun syncFromFirestore(userId: String) {
        syncJob = viewModelScope.launch {
            val result = firestoreRepository.getUserProfiles(userId)
            result.onSuccess { firestoreProfiles ->
                // Oppdater Room med data fra Firestore
                firestoreProfiles.forEach { fsProfile ->
                    // Sjekk mot brukerens EGNE profiler, ikke mot alle navn i basen.
                    // Et treff på en lokal gjesteprofil med samme navn gjorde tidligere
                    // at Google-profilen aldri ble opprettet lokalt.
                    val existingPlayer = repository.getPlayerByUsernameForGoogleUser(
                        username = fsProfile.username,
                        googleUserId = userId
                    )

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

    // Oppretter hovedprofilen for en Google-bruker ved første innlogging.
    // Sjekker hasPrimaryProfile først slik at dette er trygt å kalle flere ganger
    // (f.eks. ved hver innlogging) uten å lage duplikate primærprofiler.
    fun createPrimaryProfileForGoogleUser(
        googleUserId: String,
        displayName: String,
        email: String,
        photoUrl: String? = null
    ) {
        viewModelScope.launch {
            if (!repository.hasPrimaryProfile(googleUserId)) {
                // Lagre i Room
                val insertedId = repository.createPrimaryProfileForGoogleUser(
                    googleUserId = googleUserId,
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl
                )

                // Lagre i Firestore. profileId må være Room-IDen, ikke googleUserId:
                // updateUserProfile/deleteUserProfile slår opp dokumentet på
                // player.playerId, så en primærprofil lagret under googleUserId ble
                // aldri funnet igjen - endring og sletting av den traff et dokument
                // som ikke fantes, og feilen ble svelget.
                val firestoreProfile = FirestorePlayerProfile(
                    profileId = insertedId.toString(),
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

    // Legger til en underprofil (f.eks. familiemedlem) under en innlogget Google-bruker.
    // Lagres i Room først, deretter i Firestore med den nye Room-ID-en som profileId.
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

    // Oppretter en ren lokal gjesteprofil (ingen Google-tilknytning), derfor
    // ingen Firestore-skriving her.
    fun addLocalPlayer(username: String) {
        viewModelScope.launch {
            // Duplikate gjestenavn er ikke bare kosmetisk: når en ferdigspilt kamp skal
            // lagres slås spillerne opp på navn, så to profiler med samme navn ville
            // gitt statistikken til feil spiller.
            if (repository.getLocalPlayerByUsername(username) != null) return@launch

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

    // Generisk "legg til spiller"-metode, brukt der man ikke skiller mellom
    // lokal/Google-profil. Sjekker at brukernavnet ikke finnes fra før.
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
            // Kampene må ryddes eksplisitt: game_stats har CASCADE mot players, men
            // games har ingen foreign key. Uten dette forsvant statistikken mens selve
            // kampene ble liggende igjen og pekte på en spiller som ikke fantes lenger.
            gameRepository.deleteGamesByPlayer(player.playerId)
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