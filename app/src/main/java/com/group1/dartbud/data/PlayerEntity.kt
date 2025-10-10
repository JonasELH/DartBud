package com.group1.dartbud.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val playerId: Int = 0,
    val username: String,
    val userEmail: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)