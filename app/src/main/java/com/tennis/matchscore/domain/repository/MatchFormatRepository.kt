package com.tennis.matchscore.domain.repository

import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import kotlinx.coroutines.flow.Flow

interface MatchFormatRepository {
    fun getAllMatchFormats(): Flow<List<MatchFormatEntity>>
    suspend fun insertMatchFormat(format: MatchFormatEntity): Long
    suspend fun updateMatchFormat(format: MatchFormatEntity)
    suspend fun deleteMatchFormat(format: MatchFormatEntity)
    suspend fun isFormatInUse(formatId: Long): Boolean
}