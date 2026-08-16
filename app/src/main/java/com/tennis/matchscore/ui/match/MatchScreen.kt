package com.tennis.matchscore.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

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
    var showExitConfirmationDialog by remember { mutableStateOf(value = false) }

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
            // Cartão de Placar Principal
            ScoreBoardCard(uiState = uiState)

            // Controles de Pontuação (exibidos somente durante o jogo)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!uiState.isMatchFinished) {
                    // Banner indicando o Sacador Atual
                    val currentServerName = if (uiState.currentServerId == uiState.player1Id) {
                        uiState.player1Name
                    } else {
                        uiState.player2Name
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
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
                            onClick = { viewModel.onPlayer1Scored() },
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
                            onClick = { viewModel.onPlayer2Scored() },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { viewModel.undoLastPoint() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("↩ Desfazer Ponto")
                    }
                } else {
                    // Botão para sair quando encerrado
                    Button(
                        onClick = onCloseClick,
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
                    }
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fechar")
                }
            }
        )
    }
}

@Composable
private fun ScoreBoardCard(uiState: MatchUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

        // Placar dos Sets Finalizados (Congelados)
        completedSets.forEach { set ->
            val isWinnerOfSet = set.winnerPlayerId == playerId
            val setScoreText = if (set.isSuperTieBreak) {
                if (isPlayer1) set.player1Points else set.player2Points
            } else {
                (if (isPlayer1) set.player1Games else set.player2Games).toString()
            }

            Text(
                text = setScoreText,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = if (isWinnerOfSet) FontWeight.Bold else FontWeight.Normal
            )
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