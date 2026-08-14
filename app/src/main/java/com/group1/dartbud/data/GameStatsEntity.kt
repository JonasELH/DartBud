package com.group1.dartbud.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room-entitet for én spillers statistikk i ett spill (f.eks. snitt per kast/runde,
 * høyeste enkeltscore, antall kast/runder, og sluttscore).
 *
 * Foreign keys med CASCADE sørger for at stats-rader automatisk slettes hvis det
 * tilhørende spillet eller spilleren slettes, slik at man ikke sitter igjen med
 * foreldreløse rader i game_stats. Indeksene på gameId/playerId gjør oppslag
 * (se [GameStatsDao]) raskere.
 */
@Entity(
    tableName = "game_stats",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["gameId"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["playerId"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gameId"]),
        Index(value = ["playerId"])
    ]
)
data class GameStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val statsId: Int = 0,
    val gameId: Int,
    val playerId: Int,
    val average: Double,
    val highestScore: Int,
    val dartsThrown: Int,
    val roundsPlayed: Int,
    val finalScore: Int
)