package com.tennis.matchscore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tennis.matchscore.data.local.entity.MatchEntity
import com.tennis.matchscore.data.local.entity.PointHistoryEntity
import com.tennis.matchscore.data.local.entity.SetScoreEntity
import com.tennis.matchscore.data.local.relation.MatchWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    // --- Inserções e Atualizações da Partida ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)

    // --- Gerenciamento dos Sets ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetScore(setScore: SetScoreEntity): Long

    @Query("SELECT * FROM set_scores WHERE matchId = :matchId ORDER BY setNumber ASC")
    fun getSetScoresForMatch(matchId: Long): Flow<List<SetScoreEntity>>

    @Query("SELECT * FROM set_scores WHERE matchId = :matchId ORDER BY setNumber ASC")
    suspend fun getSetScoresForMatchSync(matchId: Long): List<SetScoreEntity>

    @Query("DELETE FROM set_scores WHERE matchId = :matchId AND setNumber = :setNumber")
    suspend fun deleteSetScoreBySetNumber(matchId: Long, setNumber: Int)

    // --- Histórico de Pontos (para Undo/Desfazer) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPointHistory(point: PointHistoryEntity): Long

    @Query("SELECT * FROM point_history WHERE matchId = :matchId ORDER BY id DESC LIMIT 1")
    suspend fun getLastPoint(matchId: Long): PointHistoryEntity?

    @Query("DELETE FROM point_history WHERE id = :pointId")
    suspend fun deletePointHistory(pointId: Long)

    // --- Consultas Agregadas ---
    @Transaction
    @Query("SELECT * FROM matches WHERE id = :matchId")
    suspend fun getMatchWithDetailsById(matchId: Long): MatchWithDetails?

    @Transaction
    @Query("SELECT * FROM matches WHERE id = :matchId")
    fun observeMatchWithDetailsById(matchId: Long): Flow<MatchWithDetails?>

    @Transaction
    @Query("SELECT * FROM matches ORDER BY createdAt DESC")
    fun getAllMatchesWithDetails(): Flow<List<MatchWithDetails>>
}