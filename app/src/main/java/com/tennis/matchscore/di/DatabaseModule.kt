package com.tennis.matchscore.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tennis.matchscore.data.local.AppDatabase
import com.tennis.matchscore.data.local.dao.MatchDao
import com.tennis.matchscore.data.local.dao.MatchFormatDao
import com.tennis.matchscore.data.local.dao.PlayerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tennis_match_score_v3" // Incrementado para forçar a criação com as novas regras
        )
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)

                    // 1. Melhor de 3 Sets com Super Tie-Break no 3º Set
                    db.execSQL(
                        """
                        INSERT INTO match_formats (
                            id, name, numberOfSets, gamesPerSet, tieBreakAt, 
                            tieBreakPoints, hasSuperTieBreakInFinalSet, superTieBreakPoints, isDefault
                        ) VALUES (
                            4, 'Melhor de 3 c/ Super Tie', 3, 6, 6, 7, 1, 10, 1
                        )
                        """.trimIndent()
                    )

                    // 2. Set Pro (1 set de 8 games)
                    db.execSQL(
                        """
                        INSERT INTO match_formats (
                            id, name, numberOfSets, gamesPerSet, tieBreakAt, 
                            tieBreakPoints, hasSuperTieBreakInFinalSet, superTieBreakPoints, isDefault
                        ) VALUES (
                            2, 'Set Pro (8 Games)', 1, 8, 8, 7, 0, 10, 0
                        )
                        """.trimIndent()
                    )

                    // 3. Set Único de 6 Games
                    db.execSQL(
                        """
                        INSERT INTO match_formats (
                            id, name, numberOfSets, gamesPerSet, tieBreakAt, 
                            tieBreakPoints, hasSuperTieBreakInFinalSet, superTieBreakPoints, isDefault
                        ) VALUES (
                            3, 'Set Único (6 Games)', 1, 6, 6, 7, 0, 10, 0
                        )
                        """.trimIndent()
                    )

                    // 4. Melhor de 3 Sets Tradicional
                    db.execSQL(
                        """
                        INSERT INTO match_formats (
                            id, name, numberOfSets, gamesPerSet, tieBreakAt, 
                            tieBreakPoints, hasSuperTieBreakInFinalSet, superTieBreakPoints, isDefault
                        ) VALUES (
                            1, 'Melhor de 3 Sets', 3, 6, 6, 7, 0, 10, 0
                        )
                        """.trimIndent()
                    )
                }
            })
            .build()
    }

    @Provides
    fun provideMatchDao(db: AppDatabase): MatchDao = db.matchDao()

    @Provides
    fun provideMatchFormatDao(db: AppDatabase): MatchFormatDao = db.matchFormatDao()

    @Provides
    fun providePlayerDao(db: AppDatabase): PlayerDao = db.playerDao()
}