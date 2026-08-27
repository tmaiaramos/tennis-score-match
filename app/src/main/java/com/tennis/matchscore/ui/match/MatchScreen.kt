package com.tennis.matchscore.ui.match

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.domain.model.CourtPosition
import com.tennis.matchscore.domain.model.HitHand
import com.tennis.matchscore.domain.model.MatchEventType
import com.tennis.matchscore.domain.model.ServeState
import com.tennis.matchscore.domain.model.ShotType
import com.tennis.matchscore.ui.match.setup.ScoringMode

private fun formatPlayerName(fullName: String): String {
    val parts = fullName.trim().split("\\s+".toRegex())
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts.first()
        else -> "${parts.first().firstOrNull()?.uppercase() ?: ""}. ${parts.last()}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    viewModel: MatchViewModel = hiltViewModel(),
    onCloseClick: () -> Unit,
    onViewStatsClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitConfirmationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            uiState.isMatchFinished -> "Placar"
                            uiState.isSuperTieBreak -> "Placar (SUPER TIE-BREAK)"
                            uiState.isTieBreak -> "Placar (TIE-BREAK)"
                            else -> "Placar"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isMatchFinished) {
                                onCloseClick()
                            } else {
                                showExitConfirmationDialog = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Seção Superior: Cartão do Placar + Botão de Desfazer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cartão de Placar Principal
                ScoreBoardCard(uiState = uiState)

                // Botão "Desfazer Ponto" posicionado logo abaixo do placar
                if (!uiState.isMatchFinished && uiState.detailingPointId == null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.undoLastPoint() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("↩ Desfazer Ponto / Jogada")
                    }
                }
            }

            // Seção Inferior: Controles de Pontuação
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .padding(top = 12.dp)
            ) {
                when {
                    uiState.detailingPointId != null -> {
                        // TELA DE DETALHAMENTO (MODO AVANÇADO) - Prioridade Máxima
                        AdvancedWinnerDetailingControls(
                            uiState = uiState,
                            onWinnerPositionSelected = viewModel::onWinnerPositionSelected,
                            onWinnerHitHandSelected = viewModel::onWinnerHitHandSelected,
                            onWinnerShotTypeSelected = viewModel::onWinnerShotTypeSelected,
                            onLoserPositionSelected = viewModel::onLoserPositionSelected,
                            onConfirmClick = viewModel::onConfirmStats
                        )
                    }
                    !uiState.isMatchFinished -> {
                        if (uiState.isInRally) {
                            RallyScoringControls(
                                uiState = uiState,
                                onWinnerClick = viewModel::onWinnerClicked,
                                onForcedErrorClick = viewModel::onForcedErrorClicked,
                                onUnforcedErrorClick = viewModel::onUnforcedErrorClicked,
                                onCancelClick = viewModel::onCancelRally
                            )
                        } else if (uiState.scoringMode == ScoringMode.INTERMEDIATE || uiState.scoringMode == ScoringMode.ADVANCED) {
                            IntermediateScoringControls(
                                uiState = uiState,
                                onAceClick = viewModel::onAceClicked,
                                onFaultClick = viewModel::onFaultClicked,
                                onReturnWinnerClick = viewModel::onReturnWinnerClicked,
                                onReturnErrorClick = viewModel::onReturnErrorClicked,
                                onBallInPlayClick = viewModel::onBallInPlayClicked
                            )
                        } else {
                            BasicScoringControls(
                                uiState = uiState,
                                onPlayer1Scored = viewModel::onPlayer1Scored,
                                onPlayer2Scored = viewModel::onPlayer2Scored
                            )
                        }
                    }
                    else -> {
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (uiState.scoringMode == ScoringMode.ADVANCED) {
                            Button(
                                onClick = { uiState.currentMatchId?.let { onViewStatsClick(it) } },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(bottom = 8.dp)
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ver Estatísticas", fontSize = 16.sp)
                            }
                        }

                        Button(
                            onClick = onCloseClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("Voltar ao Menu Principal", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    // Diálogos ...
    if (showExitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            title = { Text("Sair da partida?") },
            text = { Text("A partida ainda está em andamento. Deseja realmente sair e retornar ao menu?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmationDialog = false
                        onCloseClick()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmationDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (uiState.isMatchFinished && uiState.winnerName != null && uiState.detailingPointId == null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Fim de Jogo! 🏆") },
            text = {
                Text("Vencedor: ${uiState.winnerName}\n\nA partida foi salva automaticamente no histórico!")
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.scoringMode == ScoringMode.ADVANCED) {
                        Button(
                            onClick = { uiState.currentMatchId?.let { onViewStatsClick(it) } },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver Estatísticas")
                        }
                    }
                    Button(
                        onClick = onCloseClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar")
                    }
                }
            }
        )
    }
}

@Composable
private fun BasicScoringControls(
    uiState: MatchUiState,
    onPlayer1Scored: () -> Unit,
    onPlayer2Scored: () -> Unit
) {
    val currentServerName = if (uiState.currentServerId == uiState.player1Id) {
        uiState.player1Name
    } else {
        uiState.player2Name
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎾 Saque: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = currentServerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Text(
            text = "Registrar Ponto",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onPlayer1Scored,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "+ Ponto ${formatPlayerName(uiState.player1Name)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onPlayer2Scored,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = "+ Ponto ${formatPlayerName(uiState.player2Name)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun IntermediateScoringControls(
    uiState: MatchUiState,
    onAceClick: () -> Unit,
    onFaultClick: () -> Unit,
    onReturnWinnerClick: () -> Unit,
    onReturnErrorClick: () -> Unit,
    onBallInPlayClick: () -> Unit
) {
    val isP1Server = uiState.currentServerId == uiState.player1Id
    val isAdvanced = uiState.scoringMode == ScoringMode.ADVANCED

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabeçalhos dos Jogadores
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatPlayerName(uiState.player1Name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isP1Server) {
                        if (uiState.serveState == ServeState.FIRST_SERVE) "1º Saque" else "2º Saque"
                    } else "Recebendo",
                    color = if (isP1Server) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatPlayerName(uiState.player2Name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (!isP1Server) {
                        if (uiState.serveState == ServeState.FIRST_SERVE) "1º Saque" else "2º Saque"
                    } else "Recebendo",
                    color = if (!isP1Server) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Linha 1: ACE ou Winner Devolução
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Coluna Jogador 1
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (isP1Server) {
                        Button(onClick = onAceClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxSize()) {
                            Text("ACE", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isAdvanced) {
                        Button(onClick = onReturnWinnerClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxSize()) {
                            Text("Winner\nDevolução", textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                        }
                    }
                }

                // Coluna Jogador 2
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (!isP1Server) {
                        Button(onClick = onAceClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxSize()) {
                            Text("ACE", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isAdvanced) {
                        Button(onClick = onReturnWinnerClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxSize()) {
                            Text("Winner\nDevolução", textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                        }
                    }
                }
            }

            // Linha 2: Falta ou Erro Devolução
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Coluna Jogador 1
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (isP1Server) {
                        OutlinedButton(onClick = onFaultClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxSize()) {
                            Text(text = if (uiState.serveState == ServeState.FIRST_SERVE) "Falta" else "Dupla\nFalta", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                        }
                    } else if (isAdvanced) {
                        OutlinedButton(onClick = onReturnErrorClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxSize()) {
                            Text("Erro\nDevolução", textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                        }
                    }
                }

                // Coluna Jogador 2
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (!isP1Server) {
                        OutlinedButton(onClick = onFaultClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxSize()) {
                            Text(text = if (uiState.serveState == ServeState.FIRST_SERVE) "Falta" else "Dupla\nFalta", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                        }
                    } else if (isAdvanced) {
                        OutlinedButton(onClick = onReturnErrorClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxSize()) {
                            Text("Erro\nDevolução", textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                        }
                    }
                }
            }

            // Linha 3: Bola em Jogo
            Button(
                onClick = onBallInPlayClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TennisBallIcon(size = 16.dp, modifier = Modifier.padding(end = 8.dp))
                    Text("Bola em Jogo", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RallyScoringControls(
    uiState: MatchUiState,
    onWinnerClick: (Long) -> Unit,
    onForcedErrorClick: (Long) -> Unit,
    onUnforcedErrorClick: (Long) -> Unit,
    onCancelClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = formatPlayerName(uiState.player1Name), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = formatPlayerName(uiState.player2Name), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onWinnerClick(uiState.player1Id) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text("Winner", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { onWinnerClick(uiState.player2Id) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text("Winner", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onForcedErrorClick(uiState.player1Id) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(text = "Erro\nForçado", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                }
                OutlinedButton(onClick = { onForcedErrorClick(uiState.player2Id) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(text = "Erro\nForçado", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onUnforcedErrorClick(uiState.player1Id) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(text = "Erro\nNÃO\nForçado", textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                }
                OutlinedButton(onClick = { onUnforcedErrorClick(uiState.player2Id) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(text = "Erro\nNÃO\nForçado", textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                }
            }

            TextButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar / Voltar", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AdvancedWinnerDetailingControls(
    uiState: MatchUiState,
    onWinnerPositionSelected: (CourtPosition) -> Unit,
    onWinnerHitHandSelected: (HitHand) -> Unit,
    onWinnerShotTypeSelected: (ShotType) -> Unit,
    onLoserPositionSelected: (CourtPosition) -> Unit,
    onConfirmClick: () -> Unit
) {
    val winnerId = uiState.winnerDetailingPlayerId
    val isP1Winner = winnerId == uiState.player1Id
    val positionEnabled = !uiState.isReturnDetailing

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (uiState.detailingEventType == MatchEventType.WINNER) "Detalhamento do Winner" else "Detalhamento do Ponto",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .align(Alignment.CenterHorizontally)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isP1Winner) {
                // Jogador 1 (Vencedor) na Esquerda
                WinnerDetailingColumn(
                    modifier = Modifier.weight(1.2f),
                    playerName = formatPlayerName(uiState.player1Name),
                    selectedPosition = uiState.selectedWinnerPosition,
                    selectedHitHand = uiState.selectedWinnerHitHand,
                    selectedShotType = uiState.selectedWinnerShotType,
                    onPositionSelected = onWinnerPositionSelected,
                    onHitHandSelected = onWinnerHitHandSelected,
                    onShotTypeSelected = onWinnerShotTypeSelected,
                    positionEnabled = positionEnabled
                )

                // Jogador 2 (Perdedor) na Direita
                LoserDetailingColumn(
                    modifier = Modifier.weight(0.8f),
                    playerName = formatPlayerName(uiState.player2Name),
                    selectedPosition = uiState.selectedLoserPosition,
                    onPositionSelected = onLoserPositionSelected,
                    positionEnabled = positionEnabled
                )
            } else {
                // Jogador 1 (Perdedor) na Esquerda
                LoserDetailingColumn(
                    modifier = Modifier.weight(0.8f),
                    playerName = formatPlayerName(uiState.player1Name),
                    selectedPosition = uiState.selectedLoserPosition,
                    onPositionSelected = onLoserPositionSelected,
                    positionEnabled = positionEnabled
                )

                // Jogador 2 (Vencedor) na Direita
                WinnerDetailingColumn(
                    modifier = Modifier.weight(1.2f),
                    playerName = formatPlayerName(uiState.player2Name),
                    selectedPosition = uiState.selectedWinnerPosition,
                    selectedHitHand = uiState.selectedWinnerHitHand,
                    selectedShotType = uiState.selectedWinnerShotType,
                    onPositionSelected = onWinnerPositionSelected,
                    onHitHandSelected = onWinnerHitHandSelected,
                    onShotTypeSelected = onWinnerShotTypeSelected,
                    positionEnabled = positionEnabled
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onConfirmClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (uiState.isMatchFinished) "Confirmar e Finalizar Partida" else "Confirmar e Continuar",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun WinnerDetailingColumn(
    modifier: Modifier,
    playerName: String,
    selectedPosition: CourtPosition?,
    selectedHitHand: HitHand?,
    selectedShotType: ShotType?,
    onPositionSelected: (CourtPosition) -> Unit,
    onHitHandSelected: (HitHand) -> Unit,
    onShotTypeSelected: (ShotType) -> Unit,
    positionEnabled: Boolean
) {
    Column(modifier = modifier) {
        Text(
            text = playerName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Posicionamento
        Text("Posição:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (positionEnabled) Color.Unspecified else Color.Gray)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CourtPosition.entries.forEach { pos ->
                val selected = selectedPosition == pos
                OutlinedButton(
                    onClick = { onPositionSelected(pos) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    enabled = positionEnabled,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = if (selected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(pos.name.take(1) + pos.name.drop(1).lowercase(), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lado do Golpe
        Text("Lado:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HitHand.entries.forEach { hand ->
                val selected = selectedHitHand == hand
                OutlinedButton(
                    onClick = { onHitHandSelected(hand) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = if (selected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(hand.name.lowercase().capitalize(), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tipo de Golpe
        Text("Golpe:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        val shots = ShotType.entries
        val chunkedShots = shots.chunked(3)
        
        chunkedShots.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { type ->
                    val selected = selectedShotType == type
                    OutlinedButton(
                        onClick = { onShotTypeSelected(type) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = if (selected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(
                            text = type.name.lowercase().capitalize(),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Preenchimento para manter alinhamento
                if (row.size < 3) {
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun LoserDetailingColumn(
    modifier: Modifier,
    playerName: String,
    selectedPosition: CourtPosition?,
    onPositionSelected: (CourtPosition) -> Unit,
    positionEnabled: Boolean
) {
    Column(modifier = modifier) {
        Text(
            text = playerName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text("Posição Oponente:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (positionEnabled) Color.Unspecified else Color.Gray)
        CourtPosition.entries.forEach { pos ->
            val selected = selectedPosition == pos
            OutlinedButton(
                onClick = { onPositionSelected(pos) },
                enabled = positionEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = if (selected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(pos.name.lowercase().capitalize(), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ScoreBoardCard(uiState: MatchUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Jogador", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold)
                uiState.completedSets.forEach { completedSet ->
                    Text(text = "S${completedSet.setNumber}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
                if (!uiState.isMatchFinished) {
                    Text(text = "G", modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Text(text = "Pts", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            PlayerScoreRow(formatPlayerName(uiState.player1Name), uiState.player1Id, uiState.completedSets, uiState.player1Games, uiState.player1Score, uiState.currentServerId == uiState.player1Id, true, uiState.isMatchFinished)
            Spacer(modifier = Modifier.height(8.dp))
            PlayerScoreRow(formatPlayerName(uiState.player2Name), uiState.player2Id, uiState.completedSets, uiState.player2Games, uiState.player2Score, uiState.currentServerId == uiState.player2Id, false, uiState.isMatchFinished)
        }
    }
}

@Composable
private fun PlayerScoreRow(playerName: String, playerId: Long, completedSets: List<CompletedSetUiState>, currentGames: Int, points: String, isServing: Boolean, isPlayer1: Boolean, isMatchFinished: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(2.5f), verticalAlignment = Alignment.CenterVertically) {
            Text(text = playerName, fontSize = 15.sp, fontWeight = if (isServing && !isMatchFinished) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isServing && !isMatchFinished) { Spacer(modifier = Modifier.width(4.dp)); Text(text = "🎾", fontSize = 13.sp) }
        }
        completedSets.forEach { set ->
            val games = if (isPlayer1) set.player1Games else set.player2Games
            val opponentGames = if (isPlayer1) set.player2Games else set.player1Games
            val myTbPoints = if (isPlayer1) set.tieBreakPointsPlayer1 else set.tieBreakPointsPlayer2
            val opponentTbPoints = if (isPlayer1) set.tieBreakPointsPlayer2 else set.tieBreakPointsPlayer1
            val isWinner = set.winnerPlayerId == playerId || (set.winnerPlayerId == null && games > opponentGames)
            val hasTieBreak = myTbPoints != null && opponentTbPoints != null
            val displayScore = if (set.isSuperTieBreak && myTbPoints != null) myTbPoints.toString() else games.toString()
            Box(modifier = Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.Center) {
                Text(text = displayScore, fontSize = 16.sp, fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                if (!set.isSuperTieBreak && hasTieBreak && !isWinner && myTbPoints != null) {
                    Text(text = myTbPoints.toString(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center).offset(x = 10.dp, y = (-5).dp))
                }
            }
        }
        if (!isMatchFinished) {
            Text(text = currentGames.toString(), modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, fontSize = 16.sp)
            Box(modifier = Modifier.weight(1.2f).background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                Text(text = points, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun TennisBallIcon(size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = this.center
        drawCircle(color = Color(0xFFCCFF00), radius = radius)
        val strokeWidth = radius * 0.18f
        val linePaint = Color.White
        val leftPath = Path().apply { moveTo(center.x - radius * 0.3f, center.y - radius * 0.95f); quadraticTo(center.x - radius * 0.95f, center.y, center.x - radius * 0.3f, center.y + radius * 0.95f) }
        drawPath(path = leftPath, color = linePaint, style = Stroke(width = strokeWidth))
        val rightPath = Path().apply { moveTo(center.x + radius * 0.3f, center.y - radius * 0.95f); quadraticTo(center.x + radius * 0.95f, center.y, center.x + radius * 0.3f, center.y + radius * 0.95f) }
        drawPath(path = rightPath, color = linePaint, style = Stroke(width = strokeWidth))
    }
}

fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
