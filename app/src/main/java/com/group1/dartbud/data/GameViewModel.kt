package com.group1.dartbud.viewmodel

import com.group1.dartbud.data.FirestoreRepository
import com.group1.dartbud.data.FirestoreGame
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.group1.dartbud.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for spillhistorikk og lagring av ferdigspilte 501-spill.
 * Lagrer alltid til Room lokalt, og speiler i tillegg til Firestore hvis brukeren
 * er innlogget med en Google-konto (currentGoogleUserId er satt).
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()
    private var currentGoogleUserId: String? = null

    private val repository: GameRepository
    private val playerRepository: PlayerRepository

    // Alle spill fra Room, observert av UI (f.eks. historikkskjerm).
    private val _games = MutableStateFlow<List<GameEntity>>(emptyList())
    val games: StateFlow<List<GameEntity>> = _games.asStateFlow()

    // Settes hvis saveGame feiler, slik at UI kan vise en feilmelding til brukeren.
    private val _saveGameError = MutableStateFlow<String?>(null)
    val saveGameError: StateFlow<String?> = _saveGameError.asStateFlow()

    init {
        val database = DartBudDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao(), database.gameStatsDao())
        playerRepository = PlayerRepository(database.playerDao())

        // Følger Room-tabellen kontinuerlig og speiler den til _games.
        viewModelScope.launch {
            repository.allGames.collect { gameList ->
                _games.value = gameList
            }
        }
    }

    // Kalles ved login/logout for å styre om spillhistorikk skal synkroniseres fra Firestore.
    fun setGoogleUserId(userId: String?) {
        currentGoogleUserId = userId
        if (userId != null) {
            // Synkroniser spill fra Firestore
            syncGamesFromFirestore(userId)
        }
    }

    // NB: henter spillene fra Firestore, men gjør foreløpig ingenting med resultatet
    // (verken vises i UI eller caches i Room) - onSuccess-blokken er tom.
    private fun syncGamesFromFirestore(userId: String) {
        viewModelScope.launch {
            val result = firestoreRepository.getUserGames(userId)
            result.onSuccess { firestoreGames ->
                // Her kan vi vise disse spillene i UI uten å lagre i Room
                // Eller vi kan velge å også lagre dem i Room som cache
            }
        }
    }

    // Lagrer et ferdigspilt spill: oppretter GameEntity + to GameStatsEntity-rader i Room,
    // og hvis brukeren er innlogget, speiler i tillegg spillet til Firestore med
    // spillernavn og alle statistikkfelt flatt ut (se FirestoreGame).
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
            try {

                // Opprett GameEntity - UTEN playedAt hvis det ikke finnes
                val game = GameEntity(
                    gameId = 0,
                    player1Id = player1Id,
                    player2Id = player2Id,
                    winnerId = winnerId,
                    doubleIn = doubleIn,
                    doubleOut = doubleOut
                )

                // Lagre game i Room
                val insertedGameId = repository.insertGame(game)

                // Lagre stats med riktig gameId
                val player1StatsWithGameId = player1Stats.copy(gameId = insertedGameId.toInt())
                val player2StatsWithGameId = player2Stats.copy(gameId = insertedGameId.toInt())

                repository.insertStats(player1StatsWithGameId)
                repository.insertStats(player2StatsWithGameId)

                // Hvis bruker er innlogget, lagre også i Firestore
                currentGoogleUserId?.let { userId ->
                    // Hent spillere
                    val player1 = playerRepository.getPlayerById(player1Id)
                    val player2 = playerRepository.getPlayerById(player2Id)
                    val winner = playerRepository.getPlayerById(winnerId)

                    if (player1 != null && player2 != null && winner != null) {
                        val firestoreGame = FirestoreGame(
                            gameId = insertedGameId.toString(),
                            player1Id = player1Id.toString(),
                            player2Id = player2Id.toString(),
                            player1Name = player1.username,
                            player2Name = player2.username,
                            winnerId = winnerId.toString(),
                            winnerName = winner.username,
                            doubleIn = doubleIn,
                            doubleOut = doubleOut,
                            player1Average = player1Stats.average,
                            player2Average = player2Stats.average,
                            player1HighestScore = player1Stats.highestScore,
                            player2HighestScore = player2Stats.highestScore,
                            player1DartsThrown = player1Stats.dartsThrown,
                            player2DartsThrown = player2Stats.dartsThrown,
                            player1RoundsPlayed = player1Stats.roundsPlayed,
                            player2RoundsPlayed = player2Stats.roundsPlayed,
                            player1FinalScore = player1Stats.finalScore,
                            player2FinalScore = player2Stats.finalScore,
                            playedAt = System.currentTimeMillis()
                        )

                        firestoreRepository.saveGame(userId, firestoreGame)
                    }
                }
            } catch (e: Exception) {
                // Fanger feil fra hele lagre-forløpet (Room- eller Firestore-skriving)
                // og eksponerer den via _saveGameError slik at UI kan varsle brukeren.
                Log.e("GameViewModel", "saveGame failed", e)
                _saveGameError.value = e.message ?: "Failed to save game"
            }
        }
    }

    suspend fun getStatsByGame(gameId: Int): List<GameStatsEntity> {
        return repository.getStatsByGame(gameId)
    }

    // NB: getStatsByPlayer i repository returnerer en Flow (kan sende flere verdier
    // over tid), men her collectes kun første emisjon før funksjonen returnerer -
    // den henter altså gjeldende stats som en engangsliste, ikke en løpende strøm.
    suspend fun getStatsByPlayer(playerId: Int): List<GameStatsEntity> {
        var stats: List<GameStatsEntity> = emptyList()
        repository.getStatsByPlayer(playerId).collect {
            stats = it
        }
        return stats
    }
}