package com.tennis.matchscore.ui.match.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tennis.matchscore.domain.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MatchStatisticsUiState {
    object Loading : MatchStatisticsUiState()
    data class Success(val stats: MatchStats) : MatchStatisticsUiState()
    data class Error(val message: String) : MatchStatisticsUiState()
}

@HiltViewModel
class MatchStatisticsViewModel @Inject constructor(
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchStatisticsUiState>(MatchStatisticsUiState.Loading)
    val uiState: StateFlow<MatchStatisticsUiState> = _uiState.asStateFlow()

    fun loadStatistics(matchId: Long) {
        viewModelScope.launch {
            _uiState.value = MatchStatisticsUiState.Loading
            runCatching {
                val matchDetails = matchRepository.getMatchWithDetails(matchId)
                    ?: throw IllegalArgumentException("Match not found")
                val points = matchRepository.getPointsForMatch(matchId)
                
                val p1FullName = "${matchDetails.player1.firstName} ${matchDetails.player1.lastName}".trim()
                val p2FullName = "${matchDetails.player2.firstName} ${matchDetails.player2.lastName}".trim()
                
                val calculator = MatchStatisticsCalculator(
                    p1Id = matchDetails.match.player1Id,
                    p2Id = matchDetails.match.player2Id,
                    p1Name = p1FullName.ifBlank { "Jogador 1" },
                    p2Name = p2FullName.ifBlank { "Jogador 2" },
                    points = points
                )
                
                _uiState.value = MatchStatisticsUiState.Success(calculator.calculate())
            }.onFailure { error ->
                _uiState.value = MatchStatisticsUiState.Error(error.message ?: "Erro desconhecido")
            }
        }
    }
}
