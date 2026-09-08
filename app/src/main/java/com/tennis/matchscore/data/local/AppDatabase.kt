package com.tennis.matchscore.data.local

import androidx.room.Database
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

@Database(
    entities = [
        PlayerEntity::class,
        MatchFormatEntity::class,
        MatchEntity::class,
        SetScoreEntity::class,
        PointHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun matchFormatDao(): MatchFormatDao
    abstract fun matchDao(): MatchDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE match_formats ADD COLUMN hasAdvantage INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // match_formats table changes
                db.execSQL("ALTER TABLE match_formats ADD COLUMN hasSuperTieBreakInFinalSet INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE match_formats ADD COLUMN superTieBreakPoints INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE match_formats ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")

                // matches table changes
                db.execSQL("ALTER TABLE matches ADD COLUMN isTieBreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE matches ADD COLUMN isSuperTieBreak INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
