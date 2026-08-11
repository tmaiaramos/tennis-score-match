package com.tennis.matchscore.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tennis.matchscore.data.local.relation.MatchWithDetails
import com.tennis.matchscore.domain.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MatchHistoryViewModel @Inject constructor(
    matchRepository: MatchRepository
) : ViewModel() {

    // Assumindo que o repositório possui uma chamada para listar todas as partidas gravadas
    val matches: StateFlow<List<MatchWithDetails>> = matchRepository.observeAllMatches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}