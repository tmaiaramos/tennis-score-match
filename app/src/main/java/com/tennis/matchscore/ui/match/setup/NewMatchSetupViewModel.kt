package com.tennis.matchscore.ui.match.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import com.tennis.matchscore.data.local.entity.PlayerEntity
import com.tennis.matchscore.domain.repository.MatchFormatRepository
import com.tennis.matchscore.domain.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class CourtSurface(val displayName: String) {
    HARD("Rápida"),
    CLAY("Saibro")
}

enum class ScoringMode(val displayName: String) {
    BASIC("Básica"),
    INTERMEDIATE("Intermediária"),
    ADVANCED("Avançada")
}

data class NewMatchSetupUiState(
    val players: List<PlayerEntity> = emptyList(),
    val formats: List<MatchFormatEntity> = emptyList(),
    val player1: PlayerEntity? = null,
    val player2: PlayerEntity? = null,
    val selectedFormat: MatchFormatEntity? = null,
    val initialServer: Int = 1, // 1 para Player 1, 2 para Player 2
    val surface: CourtSurface = CourtSurface.HARD,
    val scoringMode: ScoringMode = ScoringMode.ADVANCED,
    val isLoading: Boolean = true
) {
    val isValid: Boolean
        get() = player1 != null && player2 != null &&
                player1.id != player2.id &&
                selectedFormat != null
}

@HiltViewModel
class NewMatchSetupViewModel @Inject constructor(
    playerRepository: PlayerRepository,
    matchFormatRepository: MatchFormatRepository
) : ViewModel() {

    private val _userSelections = MutableStateFlow(NewMatchSetupUiState())

    val uiState: StateFlow<NewMatchSetupUiState> = combine(
        playerRepository.getAllPlayers(),
        matchFormatRepository.getAllMatchFormats(),
        _userSelections
    ) { players, formats, currentSelection ->
        currentSelection.copy(
            players = players,
            formats = formats,
            selectedFormat = currentSelection.selectedFormat ?: formats.firstOrNull { it.isDefault } ?: formats.firstOrNull(),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewMatchSetupUiState()
    )

    fun onPlayer1Selected(player: PlayerEntity) {
        _userSelections.update { it.copy(player1 = player) }
    }

    fun onPlayer2Selected(player: PlayerEntity) {
        _userSelections.update { it.copy(player2 = player) }
    }

    fun onFormatSelected(format: MatchFormatEntity) {
        _userSelections.update { it.copy(selectedFormat = format) }
    }

    fun onInitialServerChanged(server: Int) {
        _userSelections.update { it.copy(initialServer = server) }
    }

    fun onSurfaceChanged(surface: CourtSurface) {
        _userSelections.update { it.copy(surface = surface) }
    }

    fun onScoringModeChanged(mode: ScoringMode) {
        _userSelections.update { it.copy(scoringMode = mode) }
    }
}