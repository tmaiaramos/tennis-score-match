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
import com.tennis.matchscore.domain.model.MatchStatus
import com.tennis.matchscore.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

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
        courtType: com.tennis.matchscore.domain.model.CourtType,
        createdAt: Long,
    ): Long {
        val match = MatchEntity(
            player1Id = player1Id,
            player2Id = player2Id,
            matchFormatId = matchFormatId,
            currentServerId = initialServerId,
            courtType = courtType,
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

    // --- REGISTRO DE PONTO E ATUALIZAÇÃO VIA TENNIS SCORE ENGINE ---
    override suspend fun scorePoint(matchId: Long, pointWinnerId: Long) {
        val matchDetails = matchDao.getMatchWithDetailsById(matchId) ?: return
        val match = matchDetails.match
        val format = matchDetails.format

        if (match.status == MatchStatus.FINISHED) return

        // 1. Grava no histórico o estado ANTES do ponto (para permitir Undo)
        val historyEntry = PointHistoryEntity(
            matchId = matchId,
            setNumber = match.currentSet,
            gameNumber = match.player1GamesCurrentSet + match.player2GamesCurrentSet + 1,
            pointWinnerId = pointWinnerId,
            serverId = match.currentServerId,
            scoreP1Before = match.player1PointsCurrentGame,
            scoreP2Before = match.player2PointsCurrentGame,
            gamesP1Before = match.player1GamesCurrentSet,
            gamesP2Before = match.player2GamesCurrentSet,
        )
        matchDao.insertPointHistory(historyEntry)

        // 2. Mapeia o estado atual do banco de dados para a engine
        val currentState = MatchScoreState(
            currentSet = match.currentSet,
            player1GamesCurrentSet = match.player1GamesCurrentSet,
            player2GamesCurrentSet = match.player2GamesCurrentSet,
            player1PointsCurrentGame = match.player1PointsCurrentGame,
            player2PointsCurrentGame = match.player2PointsCurrentGame,
            player1SetsWon = matchDetails.sets.count { it.winnerPlayerId == match.player1Id },
            player2SetsWon = matchDetails.sets.count { it.winnerPlayerId == match.player2Id },
            currentServerId = match.currentServerId,
            player1Id = match.player1Id,
            player2Id = match.player2Id,
            isTieBreak = match.isTieBreak,
            isFinished = false,
            winnerId = match.winnerId,
            completedSetScores = matchDetails.sets.map { Pair(it.player1Games, it.player2Games) }
        )

        // 3. Processa a lógica de regras de tênis através da TennisScoreEngine
        val engine = TennisScoreEngine(format)
        val newState = engine.processPoint(currentState, pointWinnerId)

        // 4. Se um set foi concluído ou a partida terminou, registra o resultado na tabela SetScore
        if ((newState.currentSet > currentState.currentSet) || newState.isFinished) {
            val lastCompletedSetIndex = currentState.currentSet - 1
            if (newState.completedSetScores.size > lastCompletedSetIndex) {
                val (p1Games, p2Games) = newState.completedSetScores[lastCompletedSetIndex]
                val setWinnerId = if (p1Games > p2Games) match.player1Id else match.player2Id

                val wasTieBreakSet = currentState.isTieBreak ||
                        format.isTieBreakSet(p1Games, p2Games, setNumber = currentState.currentSet)

                val tbP1 = if (wasTieBreakSet) {
                    if (pointWinnerId == match.player1Id) currentState.player1PointsCurrentGame.toIntOrNull()?.plus(1)
                    else currentState.player1PointsCurrentGame.toIntOrNull()
                } else null

                val tbP2 = if (wasTieBreakSet) {
                    if (pointWinnerId == match.player2Id) currentState.player2PointsCurrentGame.toIntOrNull()?.plus(1)
                    else currentState.player2PointsCurrentGame.toIntOrNull()
                } else null

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

        // 5. Persiste as alterações no banco de dados
        val updatedMatch = match.copy(
            currentSet = newState.currentSet,
            player1GamesCurrentSet = newState.player1GamesCurrentSet,
            player2GamesCurrentSet = newState.player2GamesCurrentSet,
            player1PointsCurrentGame = newState.player1PointsCurrentGame,
            player2PointsCurrentGame = newState.player2PointsCurrentGame,
            currentServerId = newState.currentServerId,
            isTieBreak = newState.isTieBreak,
            status = if (newState.isFinished) MatchStatus.FINISHED else match.status,
            winnerId = newState.winnerId,
            finishedAt = if (newState.isFinished) System.currentTimeMillis() else null
        )

        matchDao.updateMatch(updatedMatch)
    }

    // --- LÓGICA DE DESFAZER PONTO (UNDO) ---
    override suspend fun undoLastPoint(matchId: Long): Boolean {
        val lastPoint = matchDao.getLastPoint(matchId) ?: return false
        val matchDetails = matchDao.getMatchWithDetailsById(matchId) ?: return false
        val match = matchDetails.match

        // Restaura o placar exato gravado na PointHistoryEntity
        val restoredMatch = match.copy(
            currentSet = lastPoint.setNumber,
            player1GamesCurrentSet = lastPoint.gamesP1Before,
            player2GamesCurrentSet = lastPoint.gamesP2Before,
            player1PointsCurrentGame = lastPoint.scoreP1Before,
            player2PointsCurrentGame = lastPoint.scoreP2Before,
            currentServerId = lastPoint.serverId,
            status = MatchStatus.IN_PROGRESS,
            winnerId = null,
            finishedAt = null
        )

        matchDao.updateMatch(restoredMatch)
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

    // --- GERENCIAMENTO DE FORMATOS E JOGADORES ---
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