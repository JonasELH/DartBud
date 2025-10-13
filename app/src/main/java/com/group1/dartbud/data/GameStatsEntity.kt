package com.group1.dartbud.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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