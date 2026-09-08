package com.tennis.matchscore.ui.match.stats

import com.tennis.matchscore.data.local.entity.PointHistoryEntity
import com.tennis.matchscore.domain.model.CourtPosition
import com.tennis.matchscore.domain.model.HitHand
import com.tennis.matchscore.domain.model.MatchEventType
import com.tennis.matchscore.domain.model.ServeState
import com.tennis.matchscore.domain.model.ShotType
import kotlin.math.ceil

data class MatchStats(
    val p1: PlayerStats,
    val p2: PlayerStats
)

data class PlayerStats(
    val playerId: Long,
    val playerName: String,
    
    // Essential & Detailed Service
    val totalServes: Int,
    val firstServesIn: Int,
    val aces: Int,
    val doubleFaults: Int,
    val firstServesWon: Int,
    val secondServesIn: Int, // Item 4
    val secondServesWon: Int,
    val totalPointsServed: Int,
    
    // Return
    val returnWinnersFH: Int,
    val returnWinnersBH: Int,
    val returnErrorsFH: Int,
    val returnErrorsBH: Int,
    val unreturnedFirstServes: Int,
    val unreturnedSecondServes: Int,
    
    // Points
    val totalPointsWon: Int,
    val winnersFH: Int,
    val winnersBH: Int,
    val unforcedErrorsFH: Int,
    val unforcedErrorsBH: Int,
    val forcedErrorsFH: Int, 
    val forcedErrorsBH: Int, 
    val inducedForcedErrors: Int, 
    
    // Conversion
    val receivingPointsWon: Int,
    val totalPointsReceived: Int,
    val breakPointsWon: Int,
    val breakPointsTotal: Int,
    val netPointsWon: Int,
    val netPointsTotal: Int,
    val approachPointsWon: Int,
    val approachPointsTotal: Int,
    
    // By Shot
    val shotStats: Map<ShotType, ShotTypeStats>
) {
    val firstServePercentage: Int get() = if (totalServes > 0) ceil((firstServesIn.toDouble() * 100) / totalServes).toInt() else 0
    val firstServePointsWonPercentage: Int get() = if (firstServesIn > 0) ceil((firstServesWon.toDouble() * 100) / firstServesIn).toInt() else 0
    val secondServePointsWonPercentage: Int get() {
        val totalSecondServes = totalServes - firstServesIn
        return if (totalSecondServes > 0) ceil((secondServesWon.toDouble() * 100) / totalSecondServes).toInt() else 0
    }
    val receivingPointsWonPercentage: Int get() = if (totalPointsReceived > 0) ceil((receivingPointsWon.toDouble() * 100) / totalPointsReceived).toInt() else 0
    val netPointsWonPercentage: Int get() = if (netPointsTotal > 0) ceil((netPointsWon.toDouble() * 100) / netPointsTotal).toInt() else 0
    val approachPointsWonPercentage: Int get() = if (approachPointsTotal > 0) ceil((approachPointsWon.toDouble() * 100) / approachPointsTotal).toInt() else 0
    
    // Item 5: Ace = Winner; Double Fault = Unforced Error
    val aggressiveMargin: Int get() = (winnersFH + winnersBH + aces + inducedForcedErrors) - (unforcedErrorsFH + unforcedErrorsBH + doubleFaults)
}

data class ShotTypeStats(
    val winnersFH: Int = 0,
    val winnersBH: Int = 0,
    val forcedErrorsFH: Int = 0,
    val forcedErrorsBH: Int = 0,
    val unforcedErrorsFH: Int = 0,
    val unforcedErrorsBH: Int = 0
)

class MatchStatisticsCalculator(
    private val p1Id: Long,
    private val p2Id: Long,
    private val p1Name: String,
    private val p2Name: String,
    private val points: List<PointHistoryEntity>
) {
    fun calculate(): MatchStats {
        val p1Raw = calculateForPlayer(p1Id, p1Name, p2Id)
        val p2Raw = calculateForPlayer(p2Id, p2Name, p1Id)
        
        // Add cross-player stats
        val p1 = p1Raw.copy(
            inducedForcedErrors = p2Raw.forcedErrorsFH + p2Raw.forcedErrorsBH,
            totalPointsReceived = p2Raw.totalPointsServed
        )
        val p2 = p2Raw.copy(
            inducedForcedErrors = p1Raw.forcedErrorsFH + p1Raw.forcedErrorsBH,
            totalPointsReceived = p1Raw.totalPointsServed
        )
        
        return MatchStats(p1, p2)
    }

    private fun calculateForPlayer(playerId: Long, playerName: String, opponentId: Long): PlayerStats {
        var totalServes = 0
        var firstServesIn = 0
        var aces = 0
        var doubleFaults = 0
        var firstServesWon = 0
        var secondServesIn = 0 // Item 4
        var secondServesWon = 0
        var totalPointsServed = 0

        var returnWinnersFH = 0
        var returnWinnersBH = 0
        var returnErrorsFH = 0
        var returnErrorsBH = 0
        var unreturnedFirstServes = 0
        var unreturnedSecondServes = 0

        var totalPointsWon = 0
        var winnersFH = 0
        var winnersBH = 0
        var unforcedErrorsFH = 0
        var unforcedErrorsBH = 0
        var forcedErrorsFH = 0
        var forcedErrorsBH = 0

        var receivingPointsWon = 0
        var breakPointsWon = 0
        var breakPointsTotal = 0
        var netPointsWon = 0
        var netPointsTotal = 0
        var approachPointsWon = 0
        var approachPointsTotal = 0

        val shotStats = mutableMapOf<ShotType, ShotTypeStats>()

        points.forEach { point ->
            val isServer = point.serverId == playerId
            val wonPoint = point.pointWinnerId == playerId
            val lostPoint = point.pointWinnerId == opponentId
            
            val detailingPlayerIsMe = (point.eventType == MatchEventType.WINNER && wonPoint) || 
                                     ((point.eventType == MatchEventType.UNFORCED_ERROR || point.eventType == MatchEventType.FORCED_ERROR) && lostPoint)

            if (isServer) {
                // Item 4 & 13: Lógica de contabilização de saques (1st In, 2nd In, DF)
                if (point.serveStateBefore == ServeState.FIRST_SERVE) {
                    if (point.pointWinnerId != 0L) {
                        // Ponto encerrado no 1o saque (Ace, Winner, Erro de devolução)
                        totalServes++
                        totalPointsServed++
                        firstServesIn++
                        if (wonPoint) firstServesWon++
                    } else {
                        // Foi uma "Falta" (1o saque fora). Ponto continua.
                        totalServes++
                        // Não incrementamos totalPointsServed nem firstServesIn aqui.
                    }
                } else if (point.serveStateBefore == ServeState.SECOND_SERVE) {
                    if (point.pointWinnerId != 0L) {
                        totalPointsServed++ // Ponto encerrado no 2o saque
                        if (point.eventType != MatchEventType.DOUBLE_FAULT) {
                            // 2o saque entrou e o ponto foi disputado (Item 4)
                            secondServesIn++
                            if (wonPoint) secondServesWon++
                        } else {
                            // Dupla falta: Não incrementa secondServesIn (Item 4)
                        }
                    }
                }
                
                if (point.eventType == MatchEventType.ACE) aces++
                if (point.eventType == MatchEventType.DOUBLE_FAULT) doubleFaults++
                
                if (point.eventType == MatchEventType.ACE || ((point.isReturnEvent == true) && point.pointWinnerId == playerId && (point.eventType == MatchEventType.UNFORCED_ERROR || point.eventType == MatchEventType.FORCED_ERROR))) {
                    if (point.serveStateBefore == ServeState.FIRST_SERVE) unreturnedFirstServes++
                    else unreturnedSecondServes++
                }
            }
else {
                if (point.pointWinnerId != 0L) {
                    if (point.isReturnEvent == true) {
                        if (wonPoint && point.eventType == MatchEventType.WINNER) {
                            if (point.winnerHitHand == HitHand.FOREHAND) returnWinnersFH++ else returnWinnersBH++
                        }
                        if (lostPoint && (point.eventType == MatchEventType.UNFORCED_ERROR || point.eventType == MatchEventType.FORCED_ERROR)) {
                            if (point.winnerHitHand == HitHand.FOREHAND) returnErrorsFH++ else returnErrorsBH++
                        }
                    }
                    if (wonPoint) receivingPointsWon++
                }
            }

            if (wonPoint) {
                totalPointsWon++
                if (point.eventType == MatchEventType.WINNER) {
                    if (point.winnerHitHand == HitHand.FOREHAND) winnersFH++ else winnersBH++
                }
            }
            if (lostPoint) {
                if (point.eventType == MatchEventType.UNFORCED_ERROR) {
                    if (point.winnerHitHand == HitHand.FOREHAND) unforcedErrorsFH++ else unforcedErrorsBH++
                }
                if (point.eventType == MatchEventType.FORCED_ERROR) {
                    if (point.winnerHitHand == HitHand.FOREHAND) forcedErrorsFH++ else forcedErrorsBH++
                }
            }

            // Item 14: Break Points (em favor do jogador atual quando ele é RECEBEDOR)
            if (!isServer && point.pointWinnerId != 0L && isBreakPointOpportunity(point, playerId, opponentId)) {
                breakPointsTotal++
                if (wonPoint) breakPointsWon++
            }
            
            // Item 6 & 7: Lógica de Rede (Somente posição NET)
            val myPosition = if (detailingPlayerIsMe) point.winnerPosition else point.loserPosition
            
            if (point.pointWinnerId != 0L) {
                if (myPosition == CourtPosition.NET) {
                    netPointsTotal++
                    if (wonPoint) netPointsWon++
                }
                
                if (myPosition == CourtPosition.APPROACH) {
                    approachPointsTotal++
                    if (wonPoint) approachPointsWon++
                }
            }

            if (point.winnerShotType != null) {
                val shotType = point.winnerShotType
                val current = shotStats.getOrDefault(shotType, ShotTypeStats())
                val updated = if (detailingPlayerIsMe) {
                    when (point.eventType) {
                        MatchEventType.WINNER -> if (point.winnerHitHand == HitHand.FOREHAND) current.copy(winnersFH = current.winnersFH + 1) else current.copy(winnersBH = current.winnersBH + 1)
                        MatchEventType.FORCED_ERROR -> if (point.winnerHitHand == HitHand.FOREHAND) current.copy(forcedErrorsFH = current.forcedErrorsFH + 1) else current.copy(forcedErrorsBH = current.forcedErrorsBH + 1)
                        MatchEventType.UNFORCED_ERROR -> if (point.winnerHitHand == HitHand.FOREHAND) current.copy(unforcedErrorsFH = current.unforcedErrorsFH + 1) else current.copy(unforcedErrorsBH = current.unforcedErrorsBH + 1)
                        else -> current
                    }
                } else current
                shotStats[shotType] = updated
            }
        }

        return PlayerStats(
            playerId = playerId, playerName = playerName,
            totalServes = totalServes, firstServesIn = firstServesIn, aces = aces, doubleFaults = doubleFaults,
            firstServesWon = firstServesWon, secondServesIn = secondServesIn, secondServesWon = secondServesWon, totalPointsServed = totalPointsServed,
            returnWinnersFH = returnWinnersFH, returnWinnersBH = returnWinnersBH, returnErrorsFH = returnErrorsFH, returnErrorsBH = returnErrorsBH,
            unreturnedFirstServes = unreturnedFirstServes, unreturnedSecondServes = unreturnedSecondServes,
            totalPointsWon = totalPointsWon, winnersFH = winnersFH, winnersBH = winnersBH,
            unforcedErrorsFH = unforcedErrorsFH, unforcedErrorsBH = unforcedErrorsBH, forcedErrorsFH = forcedErrorsFH, forcedErrorsBH = forcedErrorsBH, inducedForcedErrors = 0,
            receivingPointsWon = receivingPointsWon, totalPointsReceived = 0, breakPointsWon = breakPointsWon, breakPointsTotal = breakPointsTotal,
            netPointsWon = netPointsWon, netPointsTotal = netPointsTotal, approachPointsWon = approachPointsWon, approachPointsTotal = approachPointsTotal,
            shotStats = shotStats
        )
    }

    private fun isBreakPointOpportunity(point: PointHistoryEntity, receiverId: Long, serverId: Long): Boolean {
        // Placar antes do ponto ser jogado
        val sR = if (receiverId == p1Id) point.scoreP1Before else point.scoreP2Before
        val sS = if (serverId == p1Id) point.scoreP1Before else point.scoreP2Before
        
        // Em Tie-break não existe "Break Point" (é "Mini-break", outra categoria)
        if (sR.toIntOrNull() != null || sS.toIntOrNull() != null) return false

        // Situações de Break Point para o recebedor:
        // 1. Recebedor tem 40 e Sacador tem 0, 15 ou 30.
        // 2. Recebedor tem Vantagem (AD) e Sacador tem 40 (implícito no AD).
        return when {
            sR == "40" && (sS == "0" || sS == "15" || sS == "30") -> true
            sR == "AD" -> true
            else -> false
        }
    }
}
