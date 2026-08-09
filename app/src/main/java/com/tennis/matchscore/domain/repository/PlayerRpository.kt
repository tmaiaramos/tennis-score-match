package com.tennis.matchscore.domain.repository

import com.tennis.matchscore.data.local.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun getAllPlayers(): Flow<List<PlayerEntity>>
    suspend fun insertPlayer(player: PlayerEntity): Long
    suspend fun deletePlayer(player: PlayerEntity)
}