package com.tennis.matchscore.data.repository

import com.tennis.matchscore.data.local.dao.MatchDao
import com.tennis.matchscore.data.local.dao.MatchFormatDao
import com.tennis.matchscore.data.local.dao.PlayerDao
import com.tennis.matchscore.data.local.entity.MatchEntity
import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import com.tennis.matchscore.data.local.entity.PlayerEntity
import com.tennis.matchscore.data.local.entity.PointHistoryEntity
import com.tennis.matchscore.data.local.entity.SetScoreEntity
import com.tennis.matchscore.data.local.relation.MatchWithDetails
import com.tennis.matchscore.domain.engine.MatchScoreState
import com.tennis.matchscore.domain.engine.TennisScoreEngine
import com.tennis.matchscore.domain.model.CourtPosition
import com.tennis.matchscore.domain.model.CourtType
import com.tennis.matchscore.domain.model.HitHand
import com.tennis.matchscore.domain.model.MatchEventType
import com.tennis.matchscore.domain.model.MatchStatus
import com.tennis.matchscore.domain.model.ServeState
import com.tennis.matchscore.domain.model.ShotType
import com.tennis.matchscore.domain.model.TrackingLevel
import com.tennis.matchscore.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepositoryImpl @Inject constructor(
    private val matchDao: MatchDao,
    private val matchFormatDao: MatchFormatDao,
    private val playerDao: PlayerDao,
) : MatchRepository {

    override suspend fun createMatch(
        player1Id: Long,
        player2Id: Long,
        matchFormatId: Long,
        initialServerId: Long,
        courtType: CourtType,
        trackingLevel: TrackingLevel,
        createdAt: Long,
    ): Long {
        val match = MatchEntity(
            player1Id = player1Id,
            player2Id = player2Id,
            matchFormatId = matchFormatId,
            currentServerId = initialServerId,
            courtType = courtType,
            trackingLevel = trackingLevel,
            status = MatchStatus.IN_PROGRESS,
            createdAt = createdAt,
        )
        return matchDao.insertMatch(match)
    }

    override fun observeMatchWithDetails(matchId: Long): Flow<MatchWithDetails?> {
        return matchDao.observeMatchWithDetailsById(matchId)
    }

    override suspend fun getMatchWithDetails(matchId: Long): MatchWithDetails? {
        return matchDao.getMatchWithDetailsById(matchId)
    }

    override fun observeAllMatches(): Flow<List<MatchWithDetails>> {
        return matchDao.getAllMatchesWithDetails()
    }

    override suspend fun deleteMatch(matchId: Long) {
        matchDao.deleteMatchById(matchId)
    }

    override suspend fun recordFault(matchId: Long) {
        val matchDetails = matchDao.getMatchWithDetailsById(matchId) ?: return
        val match = matchDetails.match
        val format = matchDetails.format

        if (match.status == MatchStatus.FINISHED) return
        if (match.serveState == ServeState.SECOND_SERVE) return

        val engine = TennisScoreEngine(format)
        val currentState = mapToEngineState(match)
        val newState = engine.processFault(currentState)

        // Salva histórico da falta
        val historyEntry = PointHistoryEntity(
            matchId = matchId,
            setNumber = match.currentSet,
            gameNumber = match.player1GamesCurrentSet + match.player2GamesCurrentSet + 1,
            pointWinnerId = 0L,
            serverId = match.currentServerId,
            eventType = MatchEventType.UNFORCED_ERROR,
            serveStateBefore = match.serveState,
            scoreP1Before = match.player1PointsCurrentGame,
            scoreP2Before = match.player2PointsCurrentGame,
            gamesP1Before = match.player1GamesCurrentSet,
            gamesP2Before = match.player2GamesCurrentSet,
        )
        matchDao.insertPointHistory(historyEntry)

        matchDao.updateMatch(match.copy(serveState = newState.serveState))
    }

    override suspend fun scorePoint(matchId: Long, pointWinnerId: Long, eventType: MatchEventType): Long {
        val matchDetails = matchDao.getMatchWithDetailsById(matchId) ?: return 0L
        val match = matchDetails.match
        val format = matchDetails.format

        if (match.status == MatchStatus.FINISHED) return 0L

        // 1. Grava histórico
        val historyEntry = PointHistoryEntity(
            matchId = matchId,
            setNumber = match.currentSet,
            gameNumber = match.player1GamesCurrentSet + match.player2GamesCurrentSet + 1,
            pointWinnerId = pointWinnerId,
            serverId = match.currentServerId,
            eventType = eventType,
            serveStateBefore = match.serveState,
            scoreP1Before = match.player1PointsCurrentGame,
            scoreP2Before = match.player2PointsCurrentGame,
            gamesP1Before = match.player1GamesCurrentSet,
            gamesP2Before = match.player2GamesCurrentSet,
        )
        val historyId = matchDao.insertPointHistory(historyEntry)

        // 2. Processa com a Engine
        val engine = TennisScoreEngine(format)
        val currentState = mapToEngineState(match)
        val newState = engine.processPoint(currentState, pointWinnerId)

        // 3. Registra Set se concluído
        if ((newState.currentSet > currentState.currentSet) || newState.isFinished) {
            val lastCompletedSetIndex = currentState.currentSet - 1
            if (newState.completedSetScores.size > lastCompletedSetIndex) {
                val (p1Games, p2Games) = newState.completedSetScores[lastCompletedSetIndex]
                val setWinnerId = if (p1Games > p2Games) match.player1Id else match.player2Id

                val wasTieBreakSet = currentState.isTieBreak ||
                        format.isTieBreakSet(p1Games, p2Games, setNumber = currentState.currentSet)

                val tbP1 = if (wasTieBreakSet) currentState.player1PointsCurrentGame.toIntOrNull() else null
                val tbP2 = if (wasTieBreakSet) currentState.player2PointsCurrentGame.toIntOrNull() else null

                val setScore = SetScoreEntity(
                    matchId = matchId,
                    setNumber = currentState.currentSet,
                    player1Games = p1Games,
                    player2Games = p2Games,
                    tieBreakPointsPlayer1 = tbP1,
                    tieBreakPointsPlayer2 = tbP2,
                    winnerPlayerId = setWinnerId
                )
                matchDao.insertSetScore(setScore)
            }
        }

        // 4. Atualiza Partida
        val updatedMatch = match.copy(
            currentSet = newState.currentSet,
            player1GamesCurrentSet = newState.player1GamesCurrentSet,
            player2GamesCurrentSet = newState.player2GamesCurrentSet,
            player1PointsCurrentGame = newState.player1PointsCurrentGame,
            player2PointsCurrentGame = newState.player2PointsCurrentGame,
            player1SetsWon = newState.player1SetsWon,
            player2SetsWon = newState.player2SetsWon,
            currentServerId = newState.currentServerId,
            serveState = newState.serveState,
            isTieBreak = newState.isTieBreak,
            isSuperTieBreak = newState.isSuperTieBreak,
            status = if (newState.isFinished) MatchStatus.FINISHED else match.status,
            winnerId = newState.winnerId,
            finishedAt = if (newState.isFinished) System.currentTimeMillis() else null
        )

        matchDao.updateMatch(updatedMatch)
        return historyId
    }

    override suspend fun updatePointStats(
        pointId: Long,
        winnerPosition: CourtPosition?,
        winnerHitHand: HitHand?,
        winnerShotType: ShotType?,
        loserPosition: CourtPosition?
    ) {
        val point = matchDao.getPointHistoryById(pointId) ?: return
        val updatedPoint = point.copy(
            winnerPosition = winnerPosition,
            winnerHitHand = winnerHitHand,
            winnerShotType = winnerShotType,
            loserPosition = loserPosition
        )
        matchDao.updatePointHistory(updatedPoint)
    }

    private fun mapToEngineState(match: MatchEntity) = MatchScoreState(
        currentSet = match.currentSet,
        player1GamesCurrentSet = match.player1GamesCurrentSet,
        player2GamesCurrentSet = match.player2GamesCurrentSet,
        player1PointsCurrentGame = match.player1PointsCurrentGame,
        player2PointsCurrentGame = match.player2PointsCurrentGame,
        player1SetsWon = match.player1SetsWon,
        player2SetsWon = match.player2SetsWon,
        currentServerId = match.currentServerId,
        player1Id = match.player1Id,
        player2Id = match.player2Id,
        serveState = match.serveState,
        isTieBreak = match.isTieBreak,
        isSuperTieBreak = match.isSuperTieBreak,
        isFinished = match.status == MatchStatus.FINISHED,
        winnerId = match.winnerId,
        completedSetScores = emptyList()
    )

    override suspend fun undoLastPoint(matchId: Long): Boolean {
        val lastPoint = matchDao.getLastPoint(matchId) ?: return false
        val matchDetails = matchDao.getMatchWithDetailsById(matchId) ?: return false
        val match = matchDetails.match

        // Se o ponto a desfazer for de um set concluído, apaga o registro desse set
        if (lastPoint.setNumber < match.currentSet || match.status == MatchStatus.FINISHED) {
            matchDao.deleteSetScoreBySetNumber(matchId, lastPoint.setNumber)
        }

        // Restaura o estado exato gravado na PointHistoryEntity
        val restoredMatch = match.copy(
            currentSet = lastPoint.setNumber,
            player1GamesCurrentSet = lastPoint.gamesP1Before,
            player2GamesCurrentSet = lastPoint.gamesP2Before,
            player1PointsCurrentGame = lastPoint.scoreP1Before,
            player2PointsCurrentGame = lastPoint.scoreP2Before,
            currentServerId = lastPoint.serverId,
            serveState = lastPoint.serveStateBefore,
            status = MatchStatus.IN_PROGRESS,
            winnerId = null,
            finishedAt = null
        )

        // Recalcula sets vencidos baseado na tabela set_scores
        val updatedMatchWithSets = restoredMatch.copy(
            player1SetsWon = matchDao.getSetScoresForMatchSync(matchId).count { it.winnerPlayerId == match.player1Id },
            player2SetsWon = matchDao.getSetScoresForMatchSync(matchId).count { it.winnerPlayerId == match.player2Id }
        )

        matchDao.updateMatch(updatedMatchWithSets)
        matchDao.deletePointHistory(lastPoint.id)
        return true
    }

    override suspend fun finishMatch(matchId: Long) {
        val match = matchDao.getMatchWithDetailsById(matchId)?.match ?: return
        val updated = match.copy(
            status = MatchStatus.FINISHED,
            finishedAt = System.currentTimeMillis()
        )
        matchDao.updateMatch(updated)
    }

    override fun observeAllMatchFormats(): Flow<List<MatchFormatEntity>> {
        return matchFormatDao.getAllFormats()
    }

    override suspend fun getMatchFormatById(id: Long): MatchFormatEntity? {
        return matchFormatDao.getFormatById(id)
    }

    override suspend fun saveMatchFormat(format: MatchFormatEntity): Long {
        return matchFormatDao.insert(format)
    }

    override fun observeAllPlayers(): Flow<List<PlayerEntity>> {
        return playerDao.getAllPlayers()
    }

    override suspend fun savePlayer(player: PlayerEntity): Long {
        return playerDao.insert(player)
    }
}
