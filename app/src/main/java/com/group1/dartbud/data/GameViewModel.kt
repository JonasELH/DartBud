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

    // Kalles ved login/logout. Styrer om ferdigspilte kamper også speiles til Firestore.
    //
    // Her lå tidligere et kall til syncGamesFromFirestore(), som hentet hele
    // spillhistorikken fra skyen og så kastet resultatet - onSuccess-blokken var tom.
    // Det ga en Firestore-lesning ved hver innlogging uten noen effekt, og er fjernet.
    // Historikk fra skyen vises altså ikke på tvers av enheter enda: lokal Room er
    // kilden til sannhet for visning, mens Firestore kun er en sikkerhetskopi.
    // En reell toveis-synk må håndtere at gameId i dag er Room sin autoinkrementerte
    // ID, som ikke er unik på tvers av enheter.
    fun setGoogleUserId(userId: String?) {
        currentGoogleUserId = userId
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
        player1LegsWon: Int = 0,
        player2LegsWon: Int = 0,
        totalLegsInMatch: Int = 1,
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
                    doubleOut = doubleOut,
                    player1LegsWon = player1LegsWon,
                    player2LegsWon = player2LegsWon,
                    totalLegsInMatch = totalLegsInMatch
                )

                // Lagre game i Room
                val insertedGameId = repository.insertGame(game)

                // Lagre stats med riktig gameId
                val player1StatsWithGameId = player1Stats.copy(gameId = insertedGameId.toInt())
                val player2StatsWithGameId = player2Stats.copy(gameId = insertedGameId.toInt())

                repository.insertStats(player1StatsWithGameId)
                repository.insertStats(player2StatsWithGameId)

                // Hvis bruker er innlogget, lagre også i Firestore
                val userId = currentGoogleUserId
                if (userId == null) {
                    // Gjestemodus: dette er forventet, kampen finnes kun lokalt
                    Log.i("GameViewModel", "Ingen innlogget bruker - kampen lagres kun lokalt")
                } else {
                    // Hent spillere
                    val player1 = playerRepository.getPlayerById(player1Id)
                    val player2 = playerRepository.getPlayerById(player2Id)
                    val winner = playerRepository.getPlayerById(winnerId)

                    if (player1 == null || player2 == null || winner == null) {
                        Log.w(
                            "GameViewModel",
                            "Fant ikke spillerne i Room (p1=$player1Id p2=$player2Id vinner=$winnerId) " +
                                "- hopper over Firestore"
                        )
                    } else {
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
                            player1LegsWon = player1LegsWon,
                            player2LegsWon = player2LegsWon,
                            totalLegsInMatch = totalLegsInMatch,
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

                        // Resultatet ble tidligere kastet. Feilet skrivingen - avviste
                        // sikkerhetsregler, manglende nett - skjedde det helt stille,
                        // og skysynkronisering kunne vært død i månedsvis uten at
                        // noen merket det. Nå logges utfallet begge veier.
                        firestoreRepository.saveGame(userId, firestoreGame)
                            .onSuccess {
                                Log.i(
                                    "GameViewModel",
                                    "Kamp $insertedGameId lagret i Firestore for bruker $userId"
                                )
                            }
                            .onFailure { e ->
                                Log.e("GameViewModel", "Firestore-lagring feilet", e)
                                _saveGameError.value =
                                    "Kampen ble lagret lokalt, men ikke i skyen: ${e.message}"
                            }
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