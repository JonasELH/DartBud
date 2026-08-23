package com.group1.dartbud.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Skjemaversjon 3 -> 4: la til kampformat (best av 1/3/5/7/9 legs) og legs-stillingen
// på GameEntity (se GameScreen sin nye "legs"-innstilling). Eksisterende rader var
// alle enkelt-leg-kamper, så de får totalLegsInMatch=1 og en legs-stilling som
// speiler den faktiske vinneren (1-0 eller 0-1) - ikke bare 0-0, som ville sett ut
// som en uavgjort kamp i historikken.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN player1LegsWon INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE games ADD COLUMN player2LegsWon INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE games ADD COLUMN totalLegsInMatch INTEGER NOT NULL DEFAULT 1")
        db.execSQL("UPDATE games SET player1LegsWon = 1 WHERE winnerId = player1Id")
        db.execSQL("UPDATE games SET player2LegsWon = 1 WHERE winnerId = player2Id")
    }
}

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
    version = 4,
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
                    // Bevisst UTEN fallbackToDestructiveMigration(): den slettet hele
                    // databasen ved enhver versjonsøkning, altså all spillhistorikk og
                    // alle profiler for brukeren. Nå må hver framtidig skjemaendring
                    // følges av en Migration her - glemmes det, feiler appen synlig
                    // under utvikling i stedet for å slette brukerens data i stillhet.
                    .addMigrations(MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}