package com.group1.dartbud.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room-entitet for et ferdigspilt 501-spill mellom to spillere.
 * Selve statistikken per spiller (kast, snitt, osv.) ligger i [GameStatsEntity],
 * knyttet til dette spillet via gameId.
 */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val gameId: Int = 0,
    val player1Id: Int,
    val player2Id: Int,
    val winnerId: Int,
    // Regelvarianter for spillet: om man må starte/avslutte på en dobbel.
    val doubleIn: Boolean,
    val doubleOut: Boolean,
    // Kampformat (best av 1/3/5/7/9 legs) og legs-stillingen ved kampslutt. Lagt til
    // i skjemaversjon 4 - se MIGRATION_3_4 i DartBudDatabase. totalLegsInMatch = 1
    // for kamper spilt før dette fantes (enkelt-leg, som var eneste format da).
    val player1LegsWon: Int = 0,
    val player2LegsWon: Int = 0,
    val totalLegsInMatch: Int = 1,
    val datePlayed: Long = System.currentTimeMillis()
)