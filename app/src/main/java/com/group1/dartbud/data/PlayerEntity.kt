package com.group1.dartbud.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room-entitet for en spillerprofil. Brukes både for lokale gjestespillere
 * (googleUserId = null, isUserProfile = false) og for profiler tilknyttet en
 * innlogget Google-bruker (isUserProfile = true, evt. isPrimaryProfile = true
 * for hovedprofilen som opprettes automatisk ved innlogging).
 */
@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val playerId: Int = 0,
    val username: String,
    val userEmail: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Satt kun for profiler knyttet til en Google-konto, brukes til å koble
    // mot riktig bruker i Firestore.
    val googleUserId: String? = null,
    // true = profilen tilhører en innlogget Google-bruker, false = ren lokal profil.
    val isUserProfile: Boolean = false,
    // true kun for hovedprofilen til en Google-bruker (én per googleUserId).
    val isPrimaryProfile: Boolean = false,
    val photoUrl: String? = null
)