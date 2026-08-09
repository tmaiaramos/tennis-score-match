package com.tennis.matchscore.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tennis.matchscore.data.local.entity.PlayerEntity
import com.tennis.matchscore.domain.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchUiState(
    val currentMatchId: Long? = null,
    val player1Id: Long = 0L,
    val player2Id: Long = 0L,
    val player1Name: String = "Jogador 1",
    val player2Name: String = "Jogador 2",
    val player1Score: String = "0",
    val player2Score: String = "0",
    val player1Games: Int = 0,
    val player2Games: Int = 0,
    val player1Sets: Int = 0,
    val player2Sets: Int = 0,
    val isTieBreak: Boolean = false,
    val isMatchFinished: Boolean = false,
    val winnerName: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class MatchViewModel @Inject constructor(
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchUiState())
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    init {
        initMatch()
    }

    private fun initMatch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val p1Id = matchRepository.savePlayer(PlayerEntity(firstName = "Jogador 1", lastName = ""))
                val p2Id = matchRepository.savePlayer(PlayerEntity(firstName = "Jogador 2", lastName = ""))

                val matchId = matchRepository.createMatch(
                    player1Id = p1Id,
                    player2Id = p2Id,
                    matchFormatId = 1L,
                    initialServerId = p1Id
                )

                _uiState.update {
                    it.copy(
                        currentMatchId = matchId,
                        player1Id = p1Id,
                        player2Id = p2Id,
                        isSaving = false
                    )
                }

                // Começa a escutar as atualizações da partida do Room
                matchRepository.observeMatchWithDetails(matchId).collect { details ->
                    details?.let { matchDetails ->
                        val match = matchDetails.match
                        val p1Sets = matchDetails.sets.count { set -> set.winnerPlayerId == match.player1Id }
                        val p2Sets = matchDetails.sets.count { set -> set.winnerPlayerId == match.player2Id }

                        _uiState.update { state ->
                            state.copy(
                                player1Score = match.player1PointsCurrentGame,
                                player2Score = match.player2PointsCurrentGame,
                                player1Games = match.player1GamesCurrentSet,
                                player2Games = match.player2GamesCurrentSet,
                                player1Sets = p1Sets,
                                player2Sets = p2Sets,
                                isTieBreak = match.isTieBreak,
                                isMatchFinished = match.status == com.tennis.matchscore.domain.model.MatchStatus.FINISHED,
                                winnerName = if (match.winnerId == match.player1Id) state.player1Name
                                else if (match.winnerId == match.player2Id) state.player2Name
                                else null
                            )
                        }
                    }
                }
            }.onFailure { error ->
                error.printStackTrace()
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun onPlayer1Scored() {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        if (state.isMatchFinished) return

        viewModelScope.launch {
            runCatching {
                matchRepository.scorePoint(matchId, state.player1Id)
            }.onFailure { it.printStackTrace() }
        }
    }

    fun onPlayer2Scored() {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        if (state.isMatchFinished) return

        viewModelScope.launch {
            runCatching {
                matchRepository.scorePoint(matchId, state.player2Id)
            }.onFailure { it.printStackTrace() }
        }
    }

    fun undoLastPoint() {
        val matchId = _uiState.value.currentMatchId ?: return
        viewModelScope.launch {
            runCatching {
                matchRepository.undoLastPoint(matchId)
            }.onFailure { it.printStackTrace() }
        }
    }

    fun resetMatch() {
        initMatch()
    }
}