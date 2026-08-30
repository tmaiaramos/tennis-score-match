package com.tennis.matchscore.ui.match

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.TextStyle
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
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.tennis.matchscore.R.drawable.ic_app_logo_png),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when {
                                uiState.isMatchFinished && !uiState.isDetalingActive -> "Placar"
                                uiState.isSuperTieBreak -> "Placar (SUPER TIE-BREAK)"
                                uiState.isTieBreak -> "Placar (TIE-BREAK)"
                                else -> "Placar"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isMatchFinished && !uiState.isDetalingActive) {
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

                // Botão "Desfazer Ponto" - Sempre habilitado se não terminou de fato
                if (!uiState.isMatchFinished || uiState.isDetalingActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.undoLastPoint() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Desfazer Ponto / Jogada", fontWeight = FontWeight.Bold)
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
                    uiState.isDetalingActive && uiState.detailingPointId != null -> {
                        // TELA DE DETALHAMENTO (MODO AVANÇADO)
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

    if (uiState.isMatchFinished && uiState.winnerName != null && !uiState.isDetalingActive) {
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
    
    // Azul clareado baseado no Winner (Primary), mas menos vivo
    val customBlue = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val discreteRed = Color(0xFFCF6679) // Vermelho mais discreto

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
                if (isP1Server) {
                    val isSecondServe = uiState.serveState == ServeState.SECOND_SERVE
                    val label = if (isSecondServe) "2º Saque" else "1º Saque"
                    Surface(
                        color = if (isSecondServe) Color(0xFFCF6679) else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(text = "Recebendo", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
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
                if (!isP1Server) {
                    val isSecondServe = uiState.serveState == ServeState.SECOND_SERVE
                    val label = if (isSecondServe) "2º Saque" else "1º Saque"
                    Surface(
                        color = if (isSecondServe) Color(0xFFCF6679) else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(text = "Recebendo", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
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
                        val isFirst = uiState.serveState == ServeState.FIRST_SERVE
                        Button(
                            onClick = onFaultClick, 
                            shape = RoundedCornerShape(12.dp), 
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = customBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = if (isFirst) "Falta" else "Dupla\nFalta", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                        }
                    } else if (isAdvanced) {
                        Button(
                            onClick = onReturnErrorClick, 
                            shape = RoundedCornerShape(12.dp), 
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.buttonColors(containerColor = customBlue, contentColor = Color.White)
                        ) {
                            Text("Erro\nDevolução", textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                        }
                    }
                }

                // Coluna Jogador 2
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (!isP1Server) {
                        val isFirst = uiState.serveState == ServeState.FIRST_SERVE
                        Button(
                            onClick = onFaultClick, 
                            shape = RoundedCornerShape(12.dp), 
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = customBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = if (isFirst) "Falta" else "Dupla\nFalta", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                        }
                    } else if (isAdvanced) {
                        Button(
                            onClick = onReturnErrorClick, 
                            shape = RoundedCornerShape(12.dp), 
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.buttonColors(containerColor = customBlue, contentColor = Color.White)
                        ) {
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
    val customBlue = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
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
                Button(
                    onClick = { onForcedErrorClick(uiState.player1Id) }, 
                    shape = RoundedCornerShape(12.dp), 
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = customBlue, contentColor = Color.White)
                ) {
                    Text(text = "Forced\nError", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                }
                Button(
                    onClick = { onForcedErrorClick(uiState.player2Id) }, 
                    shape = RoundedCornerShape(12.dp), 
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = customBlue, contentColor = Color.White)
                ) {
                    Text(text = "Forced\nError", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onUnforcedErrorClick(uiState.player1Id) }, 
                    shape = RoundedCornerShape(12.dp), 
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = customBlue, contentColor = Color.White)
                ) {
                    Text(text = "Unforced\nError", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                }
                Button(
                    onClick = { onUnforcedErrorClick(uiState.player2Id) }, 
                    shape = RoundedCornerShape(12.dp), 
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = customBlue, contentColor = Color.White)
                ) {
                    Text(text = "Unforced\nError", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
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
        val eventLabel = when (uiState.detailingEventType) {
            MatchEventType.WINNER -> "WINNER"
            MatchEventType.FORCED_ERROR -> "FORCED ERROR"
            MatchEventType.UNFORCED_ERROR -> "UNFORCED ERROR"
            else -> "PONTO"
        }
        
        Surface(
            color = Color(0xFF90CAF9), // Azul Médio (Blue 200)
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
        ) {
            Text(
                text = eventLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = Color(0xFF0D47A1) // Azul Escuro
            )
        }

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
    val customBlue = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    Column(modifier = modifier) {
        Text(
            text = playerName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Posicionamento
        Text("Posição:", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = if (positionEnabled) Color.Unspecified else Color.Gray)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CourtPosition.entries.forEach { pos ->
                val selected = selectedPosition == pos
                Button(
                    onClick = { onPositionSelected(pos) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    enabled = positionEnabled,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = if (selected) ButtonDefaults.buttonColors(containerColor = customBlue, contentColor = Color.White) 
                             else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    elevation = null
                ) {
                    Text(
                        text = pos.name.take(1) + pos.name.drop(1).lowercase(), 
                        fontSize = 11.sp,
                        style = TextStyle(letterSpacing = 0.9.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lado do Golpe
        Text("Lado:", fontSize = 12.sp, fontWeight = FontWeight.Normal)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HitHand.entries.forEach { hand ->
                val selected = selectedHitHand == hand
                Button(
                    onClick = { onHitHandSelected(hand) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = if (selected) ButtonDefaults.buttonColors(containerColor = customBlue, contentColor = Color.White) 
                             else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    elevation = null
                ) {
                    Text(
                        text = hand.name.lowercase().capitalize(), 
                        fontSize = 11.sp,
                        style = TextStyle(letterSpacing = 0.9.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tipo de Golpe
        Text("Golpe:", fontSize = 12.sp, fontWeight = FontWeight.Normal)
        
        val row1 = listOf(ShotType.GROUND, ShotType.SLICE, ShotType.VOLLEY)
        val row2 = listOf(ShotType.DROP, ShotType.LOB, ShotType.SMASH, ShotType.SWING)

        // Primeira linha: 3 botões
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row1.forEach { type ->
                ShotTypeButton(type, selectedShotType == type, customBlue, onShotTypeSelected)
            }
        }

        // Segunda linha: 4 botões
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row2.forEach { type ->
                ShotTypeButton(type, selectedShotType == type, customBlue, onShotTypeSelected)
            }
        }
    }
}

@Composable
private fun RowScope.ShotTypeButton(
    type: ShotType,
    selected: Boolean,
    customBlue: Color,
    onShotTypeSelected: (ShotType) -> Unit
) {
    Button(
        onClick = { onShotTypeSelected(type) },
        modifier = Modifier.weight(1f).height(40.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(0.dp),
        colors = if (selected) ButtonDefaults.buttonColors(containerColor = customBlue, contentColor = Color.White) 
                 else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        elevation = null
    ) {
        Text(
            text = type.name.lowercase().capitalize(),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(letterSpacing = 0.7.sp)
        )
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
    val customBlue = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    Column(modifier = modifier) {
        Text(
            text = playerName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text("Posição Oponente:", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = if (positionEnabled) Color.Unspecified else Color.Gray)
        CourtPosition.entries.forEach { pos ->
            val selected = selectedPosition == pos
            Button(
                onClick = { onPositionSelected(pos) },
                enabled = positionEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = if (selected) ButtonDefaults.buttonColors(containerColor = customBlue, contentColor = Color.White) 
                         else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                elevation = null
            ) {
                Text(
                    text = pos.name.lowercase().capitalize(), 
                    fontSize = 11.sp,
                    style = TextStyle(letterSpacing = 0.8.sp)
                )
            }
        }
    }
}

@Composable
private fun ScoreBoardCard(uiState: MatchUiState) {
    val showGames = !uiState.isMatchFinished && 
        !(uiState.isDetalingActive && uiState.completedSets.isNotEmpty() && 
          uiState.completedSets.last().let { it.player1Games == uiState.player1Games && it.player2Games == uiState.player2Games })

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
                
                // Só mostra coluna de Games se necessário
                if (showGames) {
                    Text(text = "G", modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
                
                // Pts continua visível
                if (!uiState.isMatchFinished || uiState.isDetalingActive) {
                    Text(text = "Pts", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            PlayerScoreRow(formatPlayerName(uiState.player1Name), uiState.player1Id, uiState.completedSets, uiState.player1Games, uiState.player1Score, uiState.currentServerId == uiState.player1Id, true, uiState.isMatchFinished, uiState.isDetalingActive, showGames)
            Spacer(modifier = Modifier.height(8.dp))
            PlayerScoreRow(formatPlayerName(uiState.player2Name), uiState.player2Id, uiState.completedSets, uiState.player2Games, uiState.player2Score, uiState.currentServerId == uiState.player2Id, false, uiState.isMatchFinished, uiState.isDetalingActive, showGames)
        }
    }
}

@Composable
private fun PlayerScoreRow(playerName: String, playerId: Long, completedSets: List<CompletedSetUiState>, currentGames: Int, points: String, isServing: Boolean, isPlayer1: Boolean, isMatchFinished: Boolean, isDetaling: Boolean, showGames: Boolean) {
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
        
        // Coluna Games - Escondida se finalizada ou redundante
        if (showGames) {
            Text(text = currentGames.toString(), modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, fontSize = 16.sp)
        }

        // Coluna Pontos
        if (!isMatchFinished || isDetaling) {
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
