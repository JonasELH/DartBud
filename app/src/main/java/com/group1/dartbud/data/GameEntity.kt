package com.group1.dartbud.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val gameId: Int = 0,
    val player1Id: Int,
    val player2Id: Int,
    val winnerId: Int,
    val doubleIn: Boolean,
    val doubleOut: Boolean,
    val datePlayed: Long = System.currentTimeMillis()
)