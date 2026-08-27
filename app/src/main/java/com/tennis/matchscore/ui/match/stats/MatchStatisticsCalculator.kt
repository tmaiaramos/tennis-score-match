package com.tennis.matchscore.ui.match.stats

import com.tennis.matchscore.data.local.entity.PointHistoryEntity
import com.tennis.matchscore.domain.model.CourtPosition
import com.tennis.matchscore.domain.model.HitHand
import com.tennis.matchscore.domain.model.MatchEventType
import com.tennis.matchscore.domain.model.ServeState
import com.tennis.matchscore.domain.model.ShotType

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
    val forcedErrorsFH: Int, // Committed by this player
    val forcedErrorsBH: Int, // Committed by this player
    val inducedForcedErrors: Int, // Induced on opponent
    
    // Conversion
    val receivingPointsWon: Int,
    val breakPointsWon: Int,
    val breakPointsTotal: Int,
    val netPointsWon: Int,
    val netPointsTotal: Int,
    val approachPointsWon: Int,
    val approachPointsTotal: Int,
    
    // By Shot
    val shotStats: Map<ShotType, ShotTypeStats>
) {
    val firstServePercentage: Int get() = if (totalServes > 0) (firstServesIn * 100) / totalServes else 0
    val firstServePointsWonPercentage: Int get() = if (firstServesIn > 0) (firstServesWon * 100) / firstServesIn else 0
    val secondServePointsWonPercentage: Int get() = if (totalServes - firstServesIn > 0) (secondServesWon * 100) / (totalServes - firstServesIn) else 0
    val aggressiveMargin: Int get() = (winnersFH + winnersBH + inducedForcedErrors) - (unforcedErrorsFH + unforcedErrorsBH)
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
        
        // Add cross-player stats (like induced errors)
        val p1 = p1Raw.copy(
            inducedForcedErrors = p2Raw.forcedErrorsFH + p2Raw.forcedErrorsBH
        )
        val p2 = p2Raw.copy(
            inducedForcedErrors = p1Raw.forcedErrorsFH + p1Raw.forcedErrorsBH
        )
        
        return MatchStats(p1, p2)
    }

    private fun calculateForPlayer(playerId: Long, playerName: String, opponentId: Long): PlayerStats {
        var totalServes = 0
        var firstServesIn = 0
        var aces = 0
        var doubleFaults = 0
        var firstServesWon = 0
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
            
            if (isServer) {
                if (point.pointWinnerId != 0L) {
                    totalServes++
                    totalPointsServed++
                    if (point.serveStateBefore == ServeState.FIRST_SERVE) {
                        firstServesIn++
                        if (wonPoint) firstServesWon++
                    } else if (point.serveStateBefore == ServeState.SECOND_SERVE) {
                        if (wonPoint) secondServesWon++
                    }
                }
                
                if (point.eventType == MatchEventType.ACE) aces++
                if (point.eventType == MatchEventType.DOUBLE_FAULT) doubleFaults++
                
                if (point.eventType == MatchEventType.ACE || (point.isReturnEvent && point.pointWinnerId == playerId && (point.eventType == MatchEventType.UNFORCED_ERROR || point.eventType == MatchEventType.FORCED_ERROR))) {
                    if (point.serveStateBefore == ServeState.FIRST_SERVE) unreturnedFirstServes++
                    else unreturnedSecondServes++
                }
            } else {
                // Return Stats
                if (point.pointWinnerId != 0L) {
                    if (point.isReturnEvent) {
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

            if (!isServer && isBreakPointOpportunity(point, playerId, opponentId)) {
                breakPointsTotal++
                if (wonPoint) breakPointsWon++
            }
            
            val detailingPlayerIsMe = (point.eventType == MatchEventType.WINNER && wonPoint) || 
                                     ((point.eventType == MatchEventType.UNFORCED_ERROR || point.eventType == MatchEventType.FORCED_ERROR) && lostPoint)
            
            if (detailingPlayerIsMe) {
                if (point.winnerPosition == CourtPosition.NET) {
                    netPointsTotal++
                    if (wonPoint) netPointsWon++
                }
                if (point.winnerPosition == CourtPosition.APPROACH) {
                    approachPointsTotal++
                    if (wonPoint) approachPointsWon++
                }
            } else {
                if (point.loserPosition == CourtPosition.NET) {
                    netPointsTotal++
                    if (wonPoint) netPointsWon++
                }
                if (point.loserPosition == CourtPosition.APPROACH) {
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
            firstServesWon = firstServesWon, secondServesWon = secondServesWon, totalPointsServed = totalPointsServed,
            returnWinnersFH = returnWinnersFH, returnWinnersBH = returnWinnersBH, returnErrorsFH = returnErrorsFH, returnErrorsBH = returnErrorsBH,
            unreturnedFirstServes = unreturnedFirstServes, unreturnedSecondServes = unreturnedSecondServes,
            totalPointsWon = totalPointsWon, winnersFH = winnersFH, winnersBH = winnersBH,
            unforcedErrorsFH = unforcedErrorsFH, unforcedErrorsBH = unforcedErrorsBH, forcedErrorsFH = forcedErrorsFH, forcedErrorsBH = forcedErrorsBH, inducedForcedErrors = 0,
            receivingPointsWon = receivingPointsWon, breakPointsWon = breakPointsWon, breakPointsTotal = breakPointsTotal,
            netPointsWon = netPointsWon, netPointsTotal = netPointsTotal, approachPointsWon = approachPointsWon, approachPointsTotal = approachPointsTotal,
            shotStats = shotStats
        )
    }

    private fun isBreakPointOpportunity(point: PointHistoryEntity, receiverId: Long, serverId: Long): Boolean {
        val sR = if (receiverId == p1Id) point.scoreP1Before else point.scoreP2Before
        val sS = if (serverId == p1Id) point.scoreP1Before else point.scoreP2Before
        return when {
            sR == "40" && (sS == "0" || sS == "15" || sS == "30") -> true
            sR == "AD" -> true
            else -> false
        }
    }
}
