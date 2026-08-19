package com.tennis.matchscore.domain.engine

import com.tennis.matchscore.domain.model.ServeState

data class MatchScoreState(
    val currentSet: Int = 1,
    val player1GamesCurrentSet: Int = 0,
    val player2GamesCurrentSet: Int = 0,
    val player1PointsCurrentGame: String = "0", // "0", "15", "30", "40", "AD" ou número de pontos do Tie-Break
    val player2PointsCurrentGame: String = "0",
    val player1SetsWon: Int = 0,
    val player2SetsWon: Int = 0,
    val currentServerId: Long,
    val player1Id: Long,
    val player2Id: Long,
    val serveState: ServeState = ServeState.FIRST_SERVE, // Controle do 1º ou 2º saque
    val isTieBreak: Boolean = false,
    val isSuperTieBreak: Boolean = false,
    val isFinished: Boolean = false,
    val winnerId: Long? = null,
    val completedSetScores: List<Pair<Int, Int>> = emptyList() // Lista com histórico dos sets já encerrados ex: [(6,4), (4,6)]
)