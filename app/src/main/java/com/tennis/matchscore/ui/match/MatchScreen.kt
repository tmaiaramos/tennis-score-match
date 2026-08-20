package com.tennis.matchscore.ui.match

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.domain.model.ServeState
import com.tennis.matchscore.ui.match.setup.ScoringMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

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
                if (!uiState.isMatchFinished) {
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

            // Seção Inferior: Controles de Pontuação de acordo com o Tipo de Marcação
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .padding(top = 12.dp)
            ) {
                if (!uiState.isMatchFinished) {
                    if (uiState.scoringMode == ScoringMode.INTERMEDIATE) {
                        // LAYOUT DE MARCAÇÃO INTERMEDIÁRIA
                        if (uiState.isInRally) {
                            RallyScoringControls(
                                uiState = uiState,
                                onWinnerClick = viewModel::onWinnerClicked,
                                onForcedErrorClick = viewModel::onForcedErrorClicked,
                                onUnforcedErrorClick = viewModel::onUnforcedErrorClicked,
                                onCancelClick = viewModel::onCancelRally
                            )
                        } else {
                            IntermediateScoringControls(
                                uiState = uiState,
                                onAceClick = viewModel::onAceClicked,
                                onFaultClick = viewModel::onFaultClicked,
                                onBallInPlayClick = viewModel::onBallInPlayClicked
                            )
                        }
                    } else {
                        // LAYOUT DE MARCAÇÃO BÁSICA/SIMPLIFICADA
                        BasicScoringControls(
                            uiState = uiState,
                            onPlayer1Scored = viewModel::onPlayer1Scored,
                            onPlayer2Scored = viewModel::onPlayer2Scored
                        )
                    }
                } else {
                    // Botão para sair quando encerrado
                    Spacer(modifier = Modifier.weight(1f))
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

    // Diálogo de confirmação para sair/abandonar partida em andamento
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

    // Diálogo exibido ao finalizar a partida
    if (uiState.isMatchFinished && uiState.winnerName != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Fim de Jogo! 🏆") },
            text = {
                Text("Vencedor: ${uiState.winnerName}\n\nA partida foi salva automaticamente no histórico!")
            },
            confirmButton = {
                Button(
                    onClick = onCloseClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fechar")
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
    onBallInPlayClick: () -> Unit
) {
    val isP1Server = uiState.currentServerId == uiState.player1Id

    Column(modifier = Modifier.fillMaxSize()) {
        // Nomes Fixos dos Jogadores e Indicador de Saque no Topo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabeçalho Jogador 1
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
                    } else " ",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Cabeçalho Jogador 2
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
                    } else " ",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Layout de Botões com Proporção Equilibrada de Altura (weights idênticos para os 3 botões)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Linha 1: Botão ACE (Equivalente a 1/3 da área útil)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (isP1Server) {
                        Button(
                            onClick = onAceClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("ACE", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (!isP1Server) {
                        Button(
                            onClick = onAceClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("ACE", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Linha 2: Botão Falta / Dupla Falta (Equivalente a 1/3 da área útil)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (isP1Server) {
                        OutlinedButton(
                            onClick = onFaultClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = if (uiState.serveState == ServeState.FIRST_SERVE) "Falta" else "Dupla Falta",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (!isP1Server) {
                        OutlinedButton(
                            onClick = onFaultClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = if (uiState.serveState == ServeState.FIRST_SERVE) "Falta" else "Dupla Falta",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Linha 3: Botão Bola em Jogo (Equivalente a 1/3 da área útil)
            Button(
                onClick = onBallInPlayClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp)
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
private fun ScoreBoardCard(uiState: MatchUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabeçalho da Tabela Dinâmico
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jogador",
                    modifier = Modifier.weight(2.5f),
                    fontWeight = FontWeight.Bold
                )

                // Colunas para Sets Finalizados (S1, S2, ...)
                uiState.completedSets.forEach { completedSet ->
                    Text(
                        text = "S${completedSet.setNumber}",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Colunas de Games e Pontos (somente se a partida NÃO estiver encerrada)
                if (!uiState.isMatchFinished) {
                    Text(
                        text = "G",
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Pts",
                        modifier = Modifier.weight(1.2f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Jogador 1
            PlayerScoreRow(
                playerName = formatPlayerName(uiState.player1Name),
                playerId = uiState.player1Id,
                completedSets = uiState.completedSets,
                currentGames = uiState.player1Games,
                points = uiState.player1Score,
                isServing = uiState.currentServerId == uiState.player1Id,
                isPlayer1 = true,
                isMatchFinished = uiState.isMatchFinished
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Jogador 2
            PlayerScoreRow(
                playerName = formatPlayerName(uiState.player2Name),
                playerId = uiState.player2Id,
                completedSets = uiState.completedSets,
                currentGames = uiState.player2Games,
                points = uiState.player2Score,
                isServing = uiState.currentServerId == uiState.player2Id,
                isPlayer1 = false,
                isMatchFinished = uiState.isMatchFinished
            )
        }
    }
}

@Composable
private fun PlayerScoreRow(
    playerName: String,
    playerId: Long,
    completedSets: List<CompletedSetUiState>,
    currentGames: Int,
    points: String,
    isServing: Boolean,
    isPlayer1: Boolean,
    isMatchFinished: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nome e Indicador de Saque
        Row(
            modifier = Modifier.weight(2.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = playerName,
                fontSize = 15.sp,
                fontWeight = if (isServing && !isMatchFinished) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isServing && !isMatchFinished) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "🎾", fontSize = 13.sp)
            }
        }

        // Placar dos Sets Finalizados (Congelados com Sobrescrito de Tiebreak)
        completedSets.forEach { set ->
            val games = if (isPlayer1) set.player1Games else set.player2Games
            val opponentGames = if (isPlayer1) set.player2Games else set.player1Games
            val myTbPoints = if (isPlayer1) set.tieBreakPointsPlayer1 else set.tieBreakPointsPlayer2
            val opponentTbPoints = if (isPlayer1) set.tieBreakPointsPlayer2 else set.tieBreakPointsPlayer1

            val isWinner = set.winnerPlayerId == playerId || (set.winnerPlayerId == null && games > opponentGames)
            val hasTieBreak = myTbPoints != null && opponentTbPoints != null

            val displayScore = if (set.isSuperTieBreak && myTbPoints != null) {
                myTbPoints.toString()
            } else {
                games.toString()
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayScore,
                    fontSize = 16.sp,
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )

                if (!set.isSuperTieBreak && hasTieBreak && !isWinner && myTbPoints != null) {
                    Text(
                        text = myTbPoints.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = 10.dp, y = (-5).dp)
                    )
                }
            }
        }

        // Games e Pontos atuais (Ocultos se o jogo acabou)
        if (!isMatchFinished) {
            Text(
                text = currentGames.toString(),
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )

            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = points,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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
        // Nomes dos Jogadores no Topo
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
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Winner
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onWinnerClick(uiState.player1Id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text("Winner", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onWinnerClick(uiState.player2Id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text("Winner", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Erro Forçado
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onForcedErrorClick(uiState.player1Id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text(
                        text = "Erro\nForçado",
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                }
                OutlinedButton(
                    onClick = { onForcedErrorClick(uiState.player2Id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text(
                        text = "Erro\nForçado",
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                }
            }

            // Erro Não Forçado
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onUnforcedErrorClick(uiState.player1Id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text(
                        text = "Erro\nNÃO\nForçado",
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                }
                OutlinedButton(
                    onClick = { onUnforcedErrorClick(uiState.player2Id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text(
                        text = "Erro\nNÃO\nForçado",
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                }
            }

            // Botão Cancelar
            TextButton(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar / Voltar", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TennisBallIcon(
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = this.center

        // Cor amarela/esverdeada clássica de bola de tênis (Optic Yellow)
        drawCircle(
            color = Color(0xFFCCFF00),
            radius = radius
        )

        // Linhas/Costuras brancas arredondadas
        val strokeWidth = radius * 0.18f
        val linePaint = Color.White

        // Curva Esquerda
        val leftPath = Path().apply {
            moveTo(center.x - radius * 0.3f, center.y - radius * 0.95f)
            quadraticTo(
                center.x - radius * 0.95f, center.y,
                center.x - radius * 0.3f, center.y + radius * 0.95f
            )
        }
        drawPath(
            path = leftPath,
            color = linePaint,
            style = Stroke(width = strokeWidth)
        )

        // Curva Direita
        val rightPath = Path().apply {
            moveTo(center.x + radius * 0.3f, center.y - radius * 0.95f)
            quadraticTo(
                center.x + radius * 0.95f, center.y,
                center.x + radius * 0.3f, center.y + radius * 0.95f
            )
        }
        drawPath(
            path = rightPath,
            color = linePaint,
            style = Stroke(width = strokeWidth)
        )
    }
}