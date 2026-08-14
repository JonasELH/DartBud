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
    val datePlayed: Long = System.currentTimeMillis()
)