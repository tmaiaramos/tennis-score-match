package com.tennis.matchscore.domain.engine

import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import com.tennis.matchscore.data.local.entity.REGULAR_TIEBREAK_POINTS

class TennisScoreEngine(
    private val format: MatchFormatEntity
) {

    fun processPoint(
        currentState: MatchScoreState,
        pointWinnerId: Long
    ): MatchScoreState {
        if (currentState.isFinished) return currentState

        val isP1 = pointWinnerId == currentState.player1Id
        val isP2 = pointWinnerId == currentState.player2Id

        if (!isP1 && !isP2) return currentState

        val isFinalSet = currentState.currentSet == format.numberOfSets
        val isSuperTieBreakSet = isFinalSet && format.hasSuperTieBreakInFinalSet

        val inTieBreak = currentState.isTieBreak || currentState.isSuperTieBreak || isSuperTieBreakSet

        val stateToProcess = if (isSuperTieBreakSet && !currentState.isSuperTieBreak) {
            currentState.copy(isSuperTieBreak = true, isTieBreak = true)
        } else {
            currentState
        }

        return if (inTieBreak) {
            processTieBreakPoint(stateToProcess, isP1)
        } else {
            processRegularGamePoint(stateToProcess, isP1)
        }
    }

    private fun processRegularGamePoint(
        state: MatchScoreState,
        isP1Winner: Boolean
    ): MatchScoreState {
        var p1Points = state.player1PointsCurrentGame
        var p2Points = state.player2PointsCurrentGame

        if (isP1Winner) {
            when {
                p1Points == "0" -> p1Points = "15"
                p1Points == "15" -> p1Points = "30"
                p1Points == "30" -> p1Points = "40"
                p1Points == "40" && p2Points == "40" -> {
                    if (format.hasAdvantage) {
                        p1Points = "AD"
                        p2Points = ""
                    } else {
                        // Regra Sem Vantagem (No-Ad): Ponto decisivo na igualdade (Deuce) ganha o game
                        return winGame(state, isP1Winner = true)
                    }
                }
                p1Points == "" && p2Points == "AD" -> {
                    p1Points = "40"
                    p2Points = "40"
                }
                p1Points == "40" && p2Points != "AD" -> return winGame(state, isP1Winner = true)
                p1Points == "AD" -> return winGame(state, isP1Winner = true)
            }
        } else {
            when {
                p2Points == "0" -> p2Points = "15"
                p2Points == "15" -> p2Points = "30"
                p2Points == "30" -> p2Points = "40"
                p2Points == "40" && p1Points == "40" -> {
                    if (format.hasAdvantage) {
                        p2Points = "AD"
                        p1Points = ""
                    } else {
                        // Regra Sem Vantagem (No-Ad): Ponto decisivo na igualdade (Deuce) ganha o game
                        return winGame(state, isP1Winner = false)
                    }
                }
                p2Points == "" && p1Points == "AD" -> {
                    p2Points = "40"
                    p1Points = "40"
                }
                p2Points == "40" && p1Points != "AD" -> return winGame(state, isP1Winner = false)
                p2Points == "AD" -> return winGame(state, isP1Winner = false)
            }
        }

        return state.copy(
            player1PointsCurrentGame = p1Points,
            player2PointsCurrentGame = p2Points
        )
    }

    private fun winGame(state: MatchScoreState, isP1Winner: Boolean): MatchScoreState {
        val newP1Games = if (isP1Winner) state.player1GamesCurrentSet + 1 else state.player1GamesCurrentSet
        val newP2Games = if (!isP1Winner) state.player2GamesCurrentSet + 1 else state.player2GamesCurrentSet
        val nextServerId = toggleServer(state.currentServerId, state.player1Id, state.player2Id)

        val targetGames = format.gamesPerSet
        val tieBreakAt = format.tieBreakAt

        val shouldStartTieBreak = newP1Games == tieBreakAt && newP2Games == tieBreakAt

        if (shouldStartTieBreak) {
            return state.copy(
                player1GamesCurrentSet = newP1Games,
                player2GamesCurrentSet = newP2Games,
                player1PointsCurrentGame = "0",
                player2PointsCurrentGame = "0",
                currentServerId = nextServerId,
                isTieBreak = true,
                isSuperTieBreak = false
            )
        }

        val isSetWon = when {
            newP1Games >= targetGames && (newP1Games - newP2Games) >= 2 -> true
            newP2Games >= targetGames && (newP2Games - newP1Games) >= 2 -> true
            else -> false
        }

        if (isSetWon) {
            return winSet(state, newP1Games, newP2Games, isP1Winner = newP1Games > newP2Games)
        }

        return state.copy(
            player1GamesCurrentSet = newP1Games,
            player2GamesCurrentSet = newP2Games,
            player1PointsCurrentGame = "0",
            player2PointsCurrentGame = "0",
            currentServerId = nextServerId,
            isTieBreak = false,
            isSuperTieBreak = false
        )
    }

    private fun processTieBreakPoint(
        state: MatchScoreState,
        isP1Winner: Boolean
    ): MatchScoreState {
        val p1Pts = state.player1PointsCurrentGame.toIntOrNull() ?: 0
        val p2Pts = state.player2PointsCurrentGame.toIntOrNull() ?: 0

        val newP1Pts = if (isP1Winner) p1Pts + 1 else p1Pts
        val newP2Pts = if (!isP1Winner) p2Pts + 1 else p2Pts
        val totalPts = newP1Pts + newP2Pts

        val nextServer = if (totalPts % 2 == 1) {
            toggleServer(state.currentServerId, state.player1Id, state.player2Id)
        } else {
            state.currentServerId
        }

        val targetPoints = if (state.isSuperTieBreak) format.superTieBreakPoints else REGULAR_TIEBREAK_POINTS

        val isTieBreakWon = when {
            newP1Pts >= targetPoints && (newP1Pts - newP2Pts) >= 2 -> true
            newP2Pts >= targetPoints && (newP2Pts - newP1Pts) >= 2 -> true
            else -> false
        }

        if (isTieBreakWon) {
            val isP1SetWinner = newP1Pts > newP2Pts

            val finalP1Games = if (state.isSuperTieBreak) {
                if (isP1SetWinner) 1 else 0
            } else {
                if (isP1SetWinner) state.player1GamesCurrentSet + 1 else state.player1GamesCurrentSet
            }

            val finalP2Games = if (state.isSuperTieBreak) {
                if (!isP1SetWinner) 1 else 0
            } else {
                if (!isP1SetWinner) state.player2GamesCurrentSet + 1 else state.player2GamesCurrentSet
            }

            return winSet(state, finalP1Games, finalP2Games, isP1Winner = isP1SetWinner)
        }

        return state.copy(
            player1PointsCurrentGame = newP1Pts.toString(),
            player2PointsCurrentGame = newP2Pts.toString(),
            currentServerId = nextServer
        )
    }

    private fun winSet(
        state: MatchScoreState,
        finalP1Games: Int,
        finalP2Games: Int,
        isP1Winner: Boolean
    ): MatchScoreState {
        val newP1SetsWon = if (isP1Winner) state.player1SetsWon + 1 else state.player1SetsWon
        val newP2SetsWon = if (!isP1Winner) state.player2SetsWon + 1 else state.player2SetsWon

        val updatedCompletedSets = state.completedSetScores + Pair(finalP1Games, finalP2Games)
        val setsToWinMatch = (format.numberOfSets / 2) + 1

        val isMatchFinished = newP1SetsWon == setsToWinMatch || newP2SetsWon == setsToWinMatch

        if (isMatchFinished) {
            return state.copy(
                player1GamesCurrentSet = finalP1Games,
                player2GamesCurrentSet = finalP2Games,
                player1PointsCurrentGame = "0",
                player2PointsCurrentGame = "0",
                player1SetsWon = newP1SetsWon,
                player2SetsWon = newP2SetsWon,
                completedSetScores = updatedCompletedSets,
                isFinished = true,
                isTieBreak = false,
                isSuperTieBreak = false,
                winnerId = if (newP1SetsWon > newP2SetsWon) state.player1Id else state.player2Id
            )
        }

        val nextSetNumber = state.currentSet + 1

        val isNextSetSuperTieBreak = format.hasSuperTieBreakInFinalSet &&
                nextSetNumber == format.numberOfSets &&
                newP1SetsWon == newP2SetsWon

        val nextServerId = toggleServer(state.currentServerId, state.player1Id, state.player2Id)

        return state.copy(
            currentSet = nextSetNumber,
            player1GamesCurrentSet = 0,
            player2GamesCurrentSet = 0,
            player1PointsCurrentGame = "0",
            player2PointsCurrentGame = "0",
            player1SetsWon = newP1SetsWon,
            player2SetsWon = newP2SetsWon,
            completedSetScores = updatedCompletedSets,
            currentServerId = nextServerId,
            isTieBreak = isNextSetSuperTieBreak,
            isSuperTieBreak = isNextSetSuperTieBreak
        )
    }

    private fun toggleServer(currentServerId: Long, p1Id: Long, p2Id: Long): Long {
        return if (currentServerId == p1Id) p2Id else p1Id
    }
}