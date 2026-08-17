package com.tennis.matchscore.ui.format

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import com.tennis.matchscore.domain.repository.MatchFormatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchFormatViewModel @Inject constructor(
    private val matchFormatRepository: MatchFormatRepository
) : ViewModel() {

    val formats: StateFlow<List<MatchFormatEntity>> = matchFormatRepository.getAllMatchFormats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveFormat(
        name: String,
        numberOfSets: Int,
        gamesPerSet: Int,
        tieBreakAt: Int,
        hasSuperTieBreakInFinalSet: Boolean,
        superTieBreakPoints: Int
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            matchFormatRepository.insertMatchFormat(
                MatchFormatEntity(
                    name = name.trim(),
                    numberOfSets = numberOfSets,
                    gamesPerSet = gamesPerSet,
                    tieBreakAt = tieBreakAt,
                    hasSuperTieBreakInFinalSet = hasSuperTieBreakInFinalSet,
                    superTieBreakPoints = superTieBreakPoints,
                    isDefault = false
                )
            )
        }
    }

    fun updateFormat(format: MatchFormatEntity, onResult: (canUpdate: Boolean) -> Unit) {
        viewModelScope.launch {
            val inUse = matchFormatRepository.isFormatInUse(format.id)
            if (!inUse) {
                matchFormatRepository.updateMatchFormat(format)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun deleteFormat(format: MatchFormatEntity, onResult: (canDelete: Boolean) -> Unit) {
        viewModelScope.launch {
            val inUse = matchFormatRepository.isFormatInUse(format.id)
            if (!inUse) {
                matchFormatRepository.deleteMatchFormat(format)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }
}