package com.group1.dartbud.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for tabellen "players". Dekker både lokale spillerprofiler (opprettet
 * på enheten uten innlogging) og Google-tilknyttede profiler som synkroniseres
 * mot Firestore (se [PlayerRepository] og [FirestoreRepository]).
 */
@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY username ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE playerId = :id")
    suspend fun getPlayerById(id: Int): PlayerEntity?

    @Query("SELECT * FROM players WHERE username = :username LIMIT 1")
    suspend fun getPlayerByUsername(username: String): PlayerEntity?

    // Som over, men begrenset til én bestemt Google-brukers profiler. Brukes ved
    // synkronisering fra Firestore, der et navnetreff på en urelatert lokal
    // gjesteprofil ikke skal telle som "profilen finnes allerede".
    @Query("SELECT * FROM players WHERE username = :username AND googleUserId = :googleUserId LIMIT 1")
    suspend fun getPlayerByUsernameForGoogleUser(username: String, googleUserId: String): PlayerEntity?

    // Lokal gjesteprofil med et gitt navn (googleUserId IS NULL). Brukes til å hindre
    // duplikate gjestenavn, som ellers gjør at kampen lagres på feil spiller siden
    // spillere slås opp på navn når en kamp skal lagres.
    @Query("SELECT * FROM players WHERE username = :username AND googleUserId IS NULL LIMIT 1")
    suspend fun getLocalPlayerByUsername(username: String): PlayerEntity?

    // IGNORE ved konflikt (f.eks. dobbel innsetting av samme profil under sync)
    // gjør at eksisterende rad beholdes i stedet for å kaste feil eller overskrive.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    @Query("DELETE FROM players WHERE playerId = :id")
    suspend fun deletePlayerById(id: Int)

    // Alle profiler tilknyttet en Google-konto. Brukes ved kontosletting, slik at
    // dataene faktisk forsvinner også lokalt - ikke bare i Firestore.
    @Query("DELETE FROM players WHERE googleUserId = :googleUserId")
    suspend fun deletePlayersByGoogleUserId(googleUserId: String)

    // NYE QUERIES for Google Sign-In

    // Alle lokale profiler knyttet til en gitt Google-konto (kan være flere,
    // f.eks. primærprofil + underprofiler for familiemedlemmer).
    @Query("SELECT * FROM players WHERE googleUserId = :googleUserId")
    fun getPlayersByGoogleUserId(googleUserId: String): Flow<List<PlayerEntity>>

    // Henter "hovedprofilen" til en innlogget Google-bruker, dvs. profilen som
    // opprettes automatisk ved første innlogging (se createPrimaryProfileForGoogleUser).
    @Query("SELECT * FROM players WHERE googleUserId = :googleUserId AND isPrimaryProfile = 1 LIMIT 1")
    suspend fun getPrimaryProfileByGoogleUserId(googleUserId: String): PlayerEntity?

    // Alle profiler (primær + eventuelle underprofiler) tilknyttet en innlogget bruker,
    // sortert med primærprofilen først.
    @Query("SELECT * FROM players WHERE isUserProfile = 1 AND googleUserId = :googleUserId ORDER BY isPrimaryProfile DESC, username ASC")
    fun getUserProfiles(googleUserId: String): Flow<List<PlayerEntity>>

    // Profiler som IKKE er knyttet til en Google-konto, dvs. rene lokale gjestespillere.
    @Query("SELECT * FROM players WHERE isUserProfile = 0 ORDER BY username ASC")
    fun getLocalProfiles(): Flow<List<PlayerEntity>>
}