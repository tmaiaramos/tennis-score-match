package com.tennis.matchscore.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tennis.matchscore.domain.model.CourtPosition
import com.tennis.matchscore.domain.model.HitHand
import com.tennis.matchscore.domain.model.MatchEventType
import com.tennis.matchscore.domain.model.ServeState
import com.tennis.matchscore.domain.model.ShotType
import com.tennis.matchscore.domain.repository.MatchRepository
import com.tennis.matchscore.ui.match.setup.ScoringMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompletedSetUiState(
    val setNumber: Int,
    val player1Games: Int,
    val player2Games: Int,
    val winnerPlayerId: Long?,
    val isSuperTieBreak: Boolean = false,
    val player1Points: String = "0",
    val player2Points: String = "0",
    val tieBreakPointsPlayer1: Int? = null,
    val tieBreakPointsPlayer2: Int? = null
)

data class MatchUiState(
    val currentMatchId: Long? = null,
    val player1Id: Long = 0L,
    val player2Id: Long = 0L,
    val currentServerId: Long = 0L,
    val serveState: ServeState = ServeState.FIRST_SERVE,
    val scoringMode: ScoringMode = ScoringMode.BASIC,
    val player1Name: String = "Jogador 1",
    val player2Name: String = "Jogador 2",
    val player1Score: String = "0",
    val player2Score: String = "0",
    val player1Games: Int = 0,
    val player2Games: Int = 0,
    val player1Sets: Int = 0,
    val player2Sets: Int = 0,
    val completedSets: List<CompletedSetUiState> = emptyList(),
    val hasAdvantage: Boolean = true,
    val isTieBreak: Boolean = false,
    val isSuperTieBreak: Boolean = false,
    val isMatchFinished: Boolean = false,
    val isInRally: Boolean = false,
    val winnerName: String? = null,
    val isSaving: Boolean = false,
    val lastRegisteredEventMessage: String? = null,

    // Detalhamento estatístico (Modo Avançado)
    val detailingPointId: Long? = null,
    val detailingEventType: MatchEventType? = null,
    val winnerDetailingPlayerId: Long? = null,
    val isReturnDetailing: Boolean = false,
    val selectedWinnerPosition: CourtPosition? = null,
    val selectedWinnerHitHand: HitHand? = null,
    val selectedWinnerShotType: ShotType? = null,
    val selectedLoserPosition: CourtPosition? = null
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
        scoringModeString: String = "BASIC",
        createdAt: Long = System.currentTimeMillis(),
        onMatchCreated: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val initialServerId = if (initialServer == 1) player1Id else player2Id

                val courtTypeEnum = try {
                    com.tennis.matchscore.domain.model.CourtType.valueOf(surface)
                } catch (e: IllegalArgumentException) {
                    com.tennis.matchscore.domain.model.CourtType.CLAY
                }

                val mode = try {
                    ScoringMode.valueOf(scoringModeString)
                } catch (e: IllegalArgumentException) {
                    ScoringMode.BASIC
                }

                val trackingLevelEnum = when (mode) {
                    ScoringMode.BASIC -> com.tennis.matchscore.domain.model.TrackingLevel.BASIC
                    ScoringMode.INTERMEDIATE -> com.tennis.matchscore.domain.model.TrackingLevel.INTERMEDIATE
                    ScoringMode.ADVANCED -> com.tennis.matchscore.domain.model.TrackingLevel.ADVANCED
                }

                val matchId = matchRepository.createMatch(
                    player1Id = player1Id,
                    player2Id = player2Id,
                    matchFormatId = formatId,
                    initialServerId = initialServerId,
                    courtType = courtTypeEnum,
                    trackingLevel = trackingLevelEnum,
                    createdAt = createdAt,
                )

                _uiState.update {
                    it.copy(
                        currentMatchId = matchId,
                        player1Id = player1Id,
                        player2Id = player2Id,
                        currentServerId = initialServerId,
                        scoringMode = mode,
                        isSaving = false
                    )
                }

                observeMatch(matchId)

                onMatchCreated()
            }.onFailure { error ->
                error.printStackTrace()
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun resumeExistingMatch(matchId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val matchWithDetails = matchRepository.getMatchWithDetails(matchId)
                    ?: throw IllegalArgumentException("Match not found")

                val mode = when (matchWithDetails.match.trackingLevel) {
                    com.tennis.matchscore.domain.model.TrackingLevel.BASIC -> ScoringMode.BASIC
                    com.tennis.matchscore.domain.model.TrackingLevel.INTERMEDIATE -> ScoringMode.INTERMEDIATE
                    com.tennis.matchscore.domain.model.TrackingLevel.ADVANCED -> ScoringMode.ADVANCED
                }

                _uiState.update {
                    it.copy(
                        currentMatchId = matchId,
                        scoringMode = mode,
                        isSaving = false
                    )
                }
                observeMatch(matchId)
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
                    val format = matchDetails.format
                    val p1Sets = matchDetails.sets.count { set -> set.winnerPlayerId == match.player1Id }
                    val p2Sets = matchDetails.sets.count { set -> set.winnerPlayerId == match.player2Id }

                    val completedSetsList = matchDetails.sets
                        .sortedBy { it.setNumber }
                        .map { setEntity ->
                            val isFinalSetSuperTieBreak = (format.hasSuperTieBreakInFinalSet &&
                                    setEntity.setNumber == format.numberOfSets) ||
                                    ((setEntity.player1Games + setEntity.player2Games <= 1) &&
                                            setEntity.tieBreakPointsPlayer1 != null && setEntity.tieBreakPointsPlayer2 != null)

                            CompletedSetUiState(
                                setNumber = setEntity.setNumber,
                                player1Games = setEntity.player1Games,
                                player2Games = setEntity.player2Games,
                                winnerPlayerId = setEntity.winnerPlayerId,
                                isSuperTieBreak = isFinalSetSuperTieBreak,
                                player1Points = match.player1PointsCurrentGame,
                                player2Points = match.player2PointsCurrentGame,
                                tieBreakPointsPlayer1 = setEntity.tieBreakPointsPlayer1,
                                tieBreakPointsPlayer2 = setEntity.tieBreakPointsPlayer2
                            )
                        }

                    val p1FullName = "${matchDetails.player1.firstName} ${matchDetails.player1.lastName}".trim()
                    val p2FullName = "${matchDetails.player2.firstName} ${matchDetails.player2.lastName}".trim()

                    val isSuperTieBreak = format.hasSuperTieBreakInFinalSet && (match.currentSet == format.numberOfSets)

                    _uiState.update { state ->
                        state.copy(
                            player1Id = match.player1Id,
                            player2Id = match.player2Id,
                            currentServerId = match.currentServerId,
                            serveState = match.serveState,
                            player1Name = p1FullName.ifBlank { "Jogador 1" },
                            player2Name = p2FullName.ifBlank { "Jogador 2" },
                            player1Score = match.player1PointsCurrentGame,
                            player2Score = match.player2PointsCurrentGame,
                            player1Games = match.player1GamesCurrentSet,
                            player2Games = match.player2GamesCurrentSet,
                            player1Sets = p1Sets,
                            player2Sets = p2Sets,
                            completedSets = completedSetsList,
                            hasAdvantage = format.hasAdvantage,
                            isTieBreak = match.isTieBreak,
                            isSuperTieBreak = isSuperTieBreak,
                            isMatchFinished = match.status == com.tennis.matchscore.domain.model.MatchStatus.FINISHED,
                            winnerName = when (match.winnerId) {
                                match.player1Id -> p1FullName
                                match.player2Id -> p2FullName
                                else -> null
                            }
                        )
                    }
                }
            }
        }
    }

    fun onAceClicked() {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        if (state.isMatchFinished) return

        viewModelScope.launch {
            runCatching {
                matchRepository.scorePoint(matchId, state.currentServerId, MatchEventType.ACE)
            }.onFailure { it.printStackTrace() }
        }
    }

    fun onFaultClicked() {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        if (state.isMatchFinished) return

        viewModelScope.launch {
            runCatching {
                if (state.serveState == ServeState.FIRST_SERVE) {
                    matchRepository.recordFault(matchId)
                } else {
                    val receiverId = if (state.currentServerId == state.player1Id) state.player2Id else state.player1Id
                    matchRepository.scorePoint(matchId, receiverId, MatchEventType.DOUBLE_FAULT)
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    // Ações de Devolução (Modo Avançado)
    fun onReturnWinnerClicked() {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        if (state.isMatchFinished) return

        val receiverId = if (state.currentServerId == state.player1Id) state.player2Id else state.player1Id
        viewModelScope.launch {
            runCatching {
                val pointId = matchRepository.scorePoint(matchId, receiverId, MatchEventType.WINNER)
                if (state.scoringMode == ScoringMode.ADVANCED) {
                    _uiState.update {
                        it.copy(
                            detailingPointId = pointId,
                            detailingEventType = MatchEventType.WINNER,
                            winnerDetailingPlayerId = receiverId,
                            isReturnDetailing = true,
                            selectedWinnerPosition = null,
                            selectedWinnerHitHand = null,
                            selectedWinnerShotType = null,
                            selectedLoserPosition = null
                        )
                    }
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun onReturnErrorClicked() {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        if (state.isMatchFinished) return

        viewModelScope.launch {
            runCatching {
                val pointId = matchRepository.scorePoint(matchId, state.currentServerId, MatchEventType.UNFORCED_ERROR)
                if (state.scoringMode == ScoringMode.ADVANCED) {
                    _uiState.update {
                        it.copy(
                            detailingPointId = pointId,
                            detailingEventType = MatchEventType.UNFORCED_ERROR,
                            winnerDetailingPlayerId = state.currentServerId,
                            isReturnDetailing = true,
                            selectedWinnerPosition = null,
                            selectedWinnerHitHand = null,
                            selectedWinnerShotType = null,
                            selectedLoserPosition = null
                        )
                    }
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun onBallInPlayClicked() {
        _uiState.update { it.copy(isInRally = true) }
    }

    fun onWinnerClicked(playerId: Long) {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        viewModelScope.launch {
            runCatching {
                val pointId = matchRepository.scorePoint(matchId, playerId, MatchEventType.WINNER)
                if (state.scoringMode == ScoringMode.ADVANCED) {
                    _uiState.update {
                        it.copy(
                            isInRally = false,
                            detailingPointId = pointId,
                            detailingEventType = MatchEventType.WINNER,
                            winnerDetailingPlayerId = playerId,
                            isReturnDetailing = false,
                            selectedWinnerPosition = null,
                            selectedWinnerHitHand = null,
                            selectedWinnerShotType = null,
                            selectedLoserPosition = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isInRally = false) }
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun onForcedErrorClicked(playerId: Long) {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        val opponentId = if (playerId == state.player1Id) state.player2Id else state.player1Id
        viewModelScope.launch {
            runCatching {
                val pointId = matchRepository.scorePoint(matchId, opponentId, MatchEventType.FORCED_ERROR)
                if (state.scoringMode == ScoringMode.ADVANCED) {
                    _uiState.update {
                        it.copy(
                            isInRally = false,
                            detailingPointId = pointId,
                            detailingEventType = MatchEventType.FORCED_ERROR,
                            winnerDetailingPlayerId = playerId, // Detalhar quem cometeu o erro
                            isReturnDetailing = false,
                            selectedWinnerPosition = null,
                            selectedWinnerHitHand = null,
                            selectedWinnerShotType = null,
                            selectedLoserPosition = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isInRally = false) }
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun onUnforcedErrorClicked(playerId: Long) {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        val opponentId = if (playerId == state.player1Id) state.player2Id else state.player1Id
        viewModelScope.launch {
            runCatching {
                val pointId = matchRepository.scorePoint(matchId, opponentId, MatchEventType.UNFORCED_ERROR)
                if (state.scoringMode == ScoringMode.ADVANCED) {
                    _uiState.update {
                        it.copy(
                            isInRally = false,
                            detailingPointId = pointId,
                            detailingEventType = MatchEventType.UNFORCED_ERROR,
                            winnerDetailingPlayerId = playerId, // Detalhar quem cometeu o erro
                            isReturnDetailing = false,
                            selectedWinnerPosition = null,
                            selectedWinnerHitHand = null,
                            selectedWinnerShotType = null,
                            selectedLoserPosition = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isInRally = false) }
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    // Ações de Detalhamento (Modo Avançado)
    fun onWinnerPositionSelected(position: CourtPosition) {
        _uiState.update { it.copy(selectedWinnerPosition = position) }
    }

    fun onWinnerHitHandSelected(hand: HitHand) {
        _uiState.update { it.copy(selectedWinnerHitHand = hand) }
    }

    fun onWinnerShotTypeSelected(shotType: ShotType) {
        _uiState.update { it.copy(selectedWinnerShotType = shotType) }
    }

    fun onLoserPositionSelected(position: CourtPosition) {
        _uiState.update { it.copy(selectedLoserPosition = position) }
    }

    fun onConfirmStats() {
        val state = _uiState.value
        val pointId = state.detailingPointId ?: return
        viewModelScope.launch {
            runCatching {
                matchRepository.updatePointStats(
                    pointId = pointId,
                    winnerPosition = state.selectedWinnerPosition,
                    winnerHitHand = state.selectedWinnerHitHand,
                    winnerShotType = state.selectedWinnerShotType,
                    loserPosition = state.selectedLoserPosition
                )
                _uiState.update {
                    it.copy(
                        detailingPointId = null,
                        winnerDetailingPlayerId = null
                    )
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun onCancelRally() {
        _uiState.update { it.copy(isInRally = false) }
    }

    fun clearConfirmationMessage() {
        _uiState.update { it.copy(lastRegisteredEventMessage = null) }
    }

    fun onPlayer1Scored() {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        if (state.isMatchFinished) return

        viewModelScope.launch {
            runCatching {
                matchRepository.scorePoint(matchId, state.player1Id, MatchEventType.REGULAR_POINT)
            }.onFailure { it.printStackTrace() }
        }
    }

    fun onPlayer2Scored() {
        val state = _uiState.value
        val matchId = state.currentMatchId ?: return
        if (state.isMatchFinished) return

        viewModelScope.launch {
            runCatching {
                matchRepository.scorePoint(matchId, state.player2Id, MatchEventType.REGULAR_POINT)
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
}
