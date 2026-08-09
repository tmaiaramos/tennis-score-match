package com.tennis.matchscore.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tennis.matchscore.data.local.entity.PlayerEntity
import com.tennis.matchscore.domain.model.DominantHand
import com.tennis.matchscore.domain.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : ViewModel() {

    val players: StateFlow<List<PlayerEntity>> = playerRepository.getAllPlayers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun savePlayer(firstName: String, lastName: String, dominantHand: DominantHand) {
        if (firstName.isBlank()) return

        viewModelScope.launch {
            playerRepository.insertPlayer(
                PlayerEntity(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    dominantHand = dominantHand
                )
            )
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            playerRepository.deletePlayer(player)
        }
    }
}