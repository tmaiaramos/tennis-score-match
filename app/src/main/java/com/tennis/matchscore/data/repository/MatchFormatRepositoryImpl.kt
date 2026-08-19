package com.tennis.matchscore.data.repository

import com.tennis.matchscore.data.local.dao.MatchFormatDao
import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import com.tennis.matchscore.domain.repository.MatchFormatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchFormatRepositoryImpl @Inject constructor(
    private val matchFormatDao: MatchFormatDao
) : MatchFormatRepository {

    override fun getAllMatchFormats(): Flow<List<MatchFormatEntity>> {
        return matchFormatDao.getAllFormats()
    }

    override suspend fun insertMatchFormat(format: MatchFormatEntity): Long {
        return matchFormatDao.insert(format)
    }

    override suspend fun updateMatchFormat(format: MatchFormatEntity) {
        matchFormatDao.update(format)
    }

    override suspend fun deleteMatchFormat(format: MatchFormatEntity) {
        matchFormatDao.delete(format)
    }

    override suspend fun isFormatInUse(formatId: Long): Boolean {
        return matchFormatDao.getMatchCountForFormat(formatId) > 0
    }
}
