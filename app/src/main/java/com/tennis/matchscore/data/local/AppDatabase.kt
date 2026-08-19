package com.tennis.matchscore.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tennis.matchscore.data.local.converter.Converter
import com.tennis.matchscore.data.local.dao.MatchDao
import com.tennis.matchscore.data.local.dao.MatchFormatDao
import com.tennis.matchscore.data.local.dao.PlayerDao
import com.tennis.matchscore.data.local.entity.MatchEntity
import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import com.tennis.matchscore.data.local.entity.PlayerEntity
import com.tennis.matchscore.data.local.entity.PointHistoryEntity
import com.tennis.matchscore.data.local.entity.SetScoreEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PlayerEntity::class,
        MatchFormatEntity::class,
        MatchEntity::class,
        SetScoreEntity::class,
        PointHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun matchFormatDao(): MatchFormatDao
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE match_formats ADD COLUMN hasAdvantage INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tennis_match_score.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    populateDefaultFormats(getDatabase(context).matchFormatDao())
                }
            }

            private suspend fun populateDefaultFormats(dao: MatchFormatDao) {
                val defaultFormats = listOf(
                    MatchFormatEntity(
                        name = "Melhor de 3 Sets (Padrão)",
                        numberOfSets = 3,
                        gamesPerSet = 6,
                        tieBreakAt = 6,
                        hasAdvantage = true,
                        hasSuperTieBreakInFinalSet = true,
                        superTieBreakPoints = 10,
                        isDefault = true
                    ),
                    MatchFormatEntity(
                        name = "Set Único de 6 Games",
                        numberOfSets = 1,
                        gamesPerSet = 6,
                        tieBreakAt = 6,
                        hasAdvantage = true,
                        hasSuperTieBreakInFinalSet = false,
                        isDefault = true
                    ),
                    MatchFormatEntity(
                        name = "Set Único de 8 Games (Pro-Set)",
                        numberOfSets = 1,
                        gamesPerSet = 8,
                        tieBreakAt = 8,
                        hasAdvantage = true,
                        hasSuperTieBreakInFinalSet = false,
                        isDefault = true
                    ),
                    MatchFormatEntity(
                        name = "Fast4 Tennis",
                        numberOfSets = 3,
                        gamesPerSet = 4,
                        tieBreakAt = 3,
                        hasAdvantage = false, // Fast4 por regra é No-Ad
                        hasSuperTieBreakInFinalSet = true,
                        superTieBreakPoints = 10,
                        isDefault = true
                    )
                )
                dao.insertAll(defaultFormats)
            }
        }
    }
}