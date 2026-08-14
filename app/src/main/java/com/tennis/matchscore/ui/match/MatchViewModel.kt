package com.tennis.matchscore.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tennis.matchscore.domain.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    private var observeMatchJob: Job? = null

    fun startNewMatch(
        player1Id: Long,
        player2Id: Long,
        formatId: Long,
        initialServer: Int,
        surface: String,
        createdAt: Long = System.currentTimeMillis(),
        onMatchCreated: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                // Determina o ID do sacador inicial com base na seleção
                val initialServerId = if (initialServer == 1) player1Id else player2Id

                // Cria a partida no repositório com os dados reais
                val matchId = matchRepository.createMatch(
                    player1Id = player1Id,
                    player2Id = player2Id,
                    matchFormatId = formatId,
                    initialServerId = initialServerId,
                    createdAt = createdAt,
                )

                _uiState.update {
                    it.copy(
                        currentMatchId = matchId,
                        player1Id = player1Id,
                        player2Id = player2Id,
                        isSaving = false
                    )
                }

                // Começa a observar os dados dessa nova partida no banco
                observeMatch(matchId)

                // Callback para prosseguir na navegação
                onMatchCreated()
            }.onFailure { error ->
                error.printStackTrace()
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun observeMatch(matchId: Long) {
        observeMatchJob?.cancel()
        observeMatchJob = viewModelScope.launch {
            matchRepository.observeMatchWithDetails(matchId).collect { details ->
                details?.let { matchDetails ->
                    val match = matchDetails.match
                    val p1Sets = matchDetails.sets.count { set -> set.winnerPlayerId == match.player1Id }
                    val p2Sets = matchDetails.sets.count { set -> set.winnerPlayerId == match.player2Id }

                    // Monta o nome completo do jogador se disponível
                    val p1FullName = "${matchDetails.player1.firstName} ${matchDetails.player1.lastName}".trim()
                    val p2FullName = "${matchDetails.player2.firstName} ${matchDetails.player2.lastName}".trim()

                    _uiState.update { state ->
                        state.copy(
                            player1Name = p1FullName.ifBlank { "Jogador 1" },
                            player2Name = p2FullName.ifBlank { "Jogador 2" },
                            player1Score = match.player1PointsCurrentGame,
                            player2Score = match.player2PointsCurrentGame,
                            player1Games = match.player1GamesCurrentSet,
                            player2Games = match.player2GamesCurrentSet,
                            player1Sets = p1Sets,
                            player2Sets = p2Sets,
                            isTieBreak = match.isTieBreak,
                            isMatchFinished = match.status == com.tennis.matchscore.domain.model.MatchStatus.FINISHED,
                            winnerName = if (match.winnerId == match.player1Id) p1FullName
                            else if (match.winnerId == match.player2Id) p2FullName
                            else null
                        )
                    }
                }
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
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return

        // Reinicia a observação/estado da partida atual
        viewModelScope.launch {
            observeMatch(matchId)
        }
    }
}