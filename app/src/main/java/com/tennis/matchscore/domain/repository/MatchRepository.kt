package com.tennis.matchscore.domain.repository

import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import com.tennis.matchscore.data.local.entity.PlayerEntity
import com.tennis.matchscore.data.local.relation.MatchWithDetails
import com.tennis.matchscore.domain.model.CourtPosition
import com.tennis.matchscore.domain.model.HitHand
import com.tennis.matchscore.domain.model.MatchEventType
import com.tennis.matchscore.domain.model.ShotType
import kotlinx.coroutines.flow.Flow

interface MatchRepository {

    // --- Criação e Leitura de Partidas ---
    suspend fun createMatch(
        player1Id: Long,
        player2Id: Long,
        matchFormatId: Long,
        initialServerId: Long,
        courtType: com.tennis.matchscore.domain.model.CourtType = com.tennis.matchscore.domain.model.CourtType.CLAY,
        createdAt: Long = System.currentTimeMillis()
    ): Long

    fun observeMatchWithDetails(matchId: Long): Flow<MatchWithDetails?>
    suspend fun getMatchWithDetails(matchId: Long): MatchWithDetails?
    fun observeAllMatches(): Flow<List<MatchWithDetails>>

    // --- Lógica de Placar e Ações da Partida ---
    suspend fun scorePoint(
        matchId: Long,
        pointWinnerId: Long,
        eventType: MatchEventType = MatchEventType.REGULAR_POINT
    ): Long

    suspend fun recordFault(matchId: Long)
    suspend fun undoLastPoint(matchId: Long): Boolean
    suspend fun finishMatch(matchId: Long)

    // --- Estatísticas Avançadas ---
    suspend fun updatePointStats(
        pointId: Long,
        winnerPosition: CourtPosition? = null,
        winnerHitHand: HitHand? = null,
        winnerShotType: ShotType? = null,
        loserPosition: CourtPosition? = null
    )

    // --- Formatos de Partida ---
    fun observeAllMatchFormats(): Flow<List<MatchFormatEntity>>
    suspend fun getMatchFormatById(id: Long): MatchFormatEntity?
    suspend fun saveMatchFormat(format: MatchFormatEntity): Long

    // --- Jogadores ---
    fun observeAllPlayers(): Flow<List<PlayerEntity>>
    suspend fun savePlayer(player: PlayerEntity): Long
}
