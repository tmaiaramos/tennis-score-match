package com.tennis.matchscore.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchFormatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(format: MatchFormatEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(formats: List<MatchFormatEntity>)

    @Update
    suspend fun update(format: MatchFormatEntity)

    @Delete
    suspend fun delete(format: MatchFormatEntity)

    @Query("SELECT * FROM match_formats ORDER BY isDefault DESC, name ASC")
    fun getAllFormats(): Flow<List<MatchFormatEntity>>

    @Query("SELECT * FROM match_formats WHERE id = :id")
    suspend fun getFormatById(id: Long): MatchFormatEntity?

    @Query("SELECT COUNT(*) FROM matches WHERE matchFormatId = :matchFormatId")
    suspend fun getMatchCountForFormat(matchFormatId: Long): Int
}