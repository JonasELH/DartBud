package com.group1.dartbud.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room-databasen for DartBud. Definerer hvilke tabeller (entities) som finnes lokalt
 * på enheten, og eksponerer DAO-ene som brukes til å lese/skrive dem.
 *
 * Dette er den lokale (offline) datakilden. Firestore-synkronisering skjer separat
 * via [FirestoreRepository] og er ikke en del av Room-skjemaet.
 */
@Database(
    entities = [
        PlayerEntity::class,
        GameEntity::class,
        GameStatsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DartBudDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun gameStatsDao(): GameStatsDao

    companion object {
        // Sikrer at hele appen deler én databaseinstans (singleton).
        // @Volatile gjør at endringer på INSTANCE er synlig umiddelbart for alle tråder.
        @Volatile
        private var INSTANCE: DartBudDatabase? = null

        fun getDatabase(context: Context): DartBudDatabase {
            // Dobbel sjekk med synchronized: unngår at flere tråder oppretter
            // hver sin database samtidig ved første kall.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DartBudDatabase::class.java,
                    "dartbud_database"
                )
                    // NB: sletter og gjenoppretter hele databasen ved skjemaendring
                    // i steden for å migrere data. Greit under utvikling, men fører
                    // til datatap for eksisterende brukere ved versjonsøkning i produksjon.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}