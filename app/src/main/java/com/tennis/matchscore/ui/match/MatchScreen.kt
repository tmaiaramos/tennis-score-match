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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

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
                        text = if (uiState.isTieBreak) "Placar de Tênis (TIE-BREAK)" else "Placar de Tênis",
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

            // Controles de Pontuação
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                        enabled = !uiState.isMatchFinished,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(end = 8.dp)
                    ) {
                        Text("+ Ponto ${uiState.player1Name}")
                    }

                    Button(
                        onClick = { viewModel.onPlayer2Scored() },
                        enabled = !uiState.isMatchFinished,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(start = 8.dp)
                    ) {
                        Text("+ Ponto ${uiState.player2Name}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.undoLastPoint() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("↩ Desfazer Ponto")
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetMatch() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reiniciar Partida")
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
    if ((uiState.isMatchFinished && uiState.winnerName != null)) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Jogador", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                Text("Sets", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text("Games", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text("Pontos", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            PlayerScoreRow(
                playerName = uiState.player1Name,
                sets = uiState.player1Sets,
                games = uiState.player1Games,
                points = uiState.player1Score
            )

            Spacer(modifier = Modifier.height(8.dp))

            PlayerScoreRow(
                playerName = uiState.player2Name,
                sets = uiState.player2Sets,
                games = uiState.player2Games,
                points = uiState.player2Score
            )
        }
    }
}

@Composable
private fun PlayerScoreRow(
    playerName: String,
    sets: Int,
    games: Int,
    points: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(playerName, modifier = Modifier.weight(2f), fontSize = 16.sp)
        Text(sets.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 18.sp)
        Text(games.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 18.sp)

        Box(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = points,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}