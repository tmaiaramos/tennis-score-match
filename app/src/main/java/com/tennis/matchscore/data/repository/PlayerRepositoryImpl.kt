package com.tennis.matchscore.data.repository

import com.tennis.matchscore.data.local.dao.PlayerDao
import com.tennis.matchscore.data.local.entity.PlayerEntity
import com.tennis.matchscore.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val playerDao: PlayerDao
) : PlayerRepository {

    override fun getAllPlayers(): Flow<List<PlayerEntity>> = playerDao.getAllPlayers()

    override suspend fun insertPlayer(player: PlayerEntity): Long = playerDao.insert(player)

    override suspend fun deletePlayer(player: PlayerEntity) = playerDao.delete(player)
}