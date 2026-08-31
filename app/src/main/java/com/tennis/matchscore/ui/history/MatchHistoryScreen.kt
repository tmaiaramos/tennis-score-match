package com.tennis.matchscore.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.R
import com.tennis.matchscore.data.local.relation.MatchWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchHistoryScreen(
    viewModel: MatchHistoryViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onMatchClick: (Long) -> Unit,
    onViewStatsClick: (Long) -> Unit
) {
    val matches by viewModel.matches.collectAsState()
    var matchToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_app_logo_png),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Histórico de Partidas", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
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
        if (matches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Nenhuma partida registrada", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(matches, key = { it.match.id }) { match ->
                    MatchHistoryCard(
                        match = match,
                        onClick = { onMatchClick(match.match.id) },
                        onDeleteClick = { matchToDelete = match.match.id },
                        onStatsClick = { onViewStatsClick(match.match.id) }
                    )
                }
            }
        }
    }

    if (matchToDelete != null) {
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            title = { Text("Excluir partida?") },
            text = { Text("Deseja realmente excluir esta partida? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        matchToDelete?.let { viewModel.deleteMatch(it) }
                        matchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun MatchHistoryCard(
    match: MatchWithDetails,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }
    val dateStr = dateFormatter.format(Date(match.match.createdAt))
    val isInProgress = match.match.status == com.tennis.matchscore.domain.model.MatchStatus.IN_PROGRESS

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isInProgress) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "EM ANDAMENTO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = match.match.courtType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cabeçalho do Placar (Labels S1, S2, G, Pts)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    match.sets.sortedBy { it.setNumber }.forEach { set ->
                        Text(
                            text = "S${set.setNumber}",
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isInProgress) {
                        val currentSetNum = match.sets.size + 1
                        Text(
                            text = "S$currentSetNum",
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pts",
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(32.dp)) // Espaço do Troféu
            }

            Spacer(modifier = Modifier.height(10.dp)) // Espaçamento vertical entre cabeçalho e números

            // Linha Jogador 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.player1.firstName} ${match.player1.lastName}",
                    modifier = Modifier.weight(1f),
                    fontWeight = if (match.match.winnerId == match.player1.id) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sets concluídos
                    match.sets.sortedBy { it.setNumber }.forEach { set ->
                        SetScoreItem(set.player1Games, set.player2Games, set.tieBreakPointsPlayer1, match.match.winnerId == match.player1.id && set.winnerPlayerId == match.player1.id, false)
                    }
                    // Games do set atual (se em andamento)
                    if (isInProgress) {
                        SetScoreItem(
                            games = match.match.player1GamesCurrentSet,
                            opponentGames = match.match.player2GamesCurrentSet,
                            tieBreakPoints = null,
                            isSetWinner = false,
                            isSuperTieBreak = false,
                            isCurrentGames = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = match.match.player1PointsCurrentGame,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                    if (match.match.winnerId == match.player1.id) {
                        Text(text = "🏆", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Linha Jogador 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.player2.firstName} ${match.player2.lastName}",
                    modifier = Modifier.weight(1f),
                    fontWeight = if (match.match.winnerId == match.player2.id) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sets concluídos
                    match.sets.sortedBy { it.setNumber }.forEach { set ->
                        SetScoreItem(set.player2Games, set.player1Games, set.tieBreakPointsPlayer2, match.match.winnerId == match.player2.id && set.winnerPlayerId == match.player2.id, false)
                    }
                    // Games do set atual (se em andamento)
                    if (isInProgress) {
                        SetScoreItem(
                            games = match.match.player2GamesCurrentSet,
                            opponentGames = match.match.player1GamesCurrentSet,
                            tieBreakPoints = null,
                            isSetWinner = false,
                            isSuperTieBreak = false,
                            isCurrentGames = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = match.match.player2PointsCurrentGame,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                    if (match.match.winnerId == match.player2.id) {
                        Text(text = "🏆", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (match.match.trackingLevel == com.tennis.matchscore.domain.model.TrackingLevel.ADVANCED) {
                    TextButton(onClick = onStatsClick) {
                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Estatísticas")
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = Color(0xFF1A237E)
                    )
                }
            }
        }
    }
}

@Composable
private fun SetScoreItem(
    games: Int,
    opponentGames: Int,
    tieBreakPoints: Int?,
    isSetWinner: Boolean,
    isSuperTieBreak: Boolean,
    isCurrentGames: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = games.toString(),
            fontWeight = if (isSetWinner) FontWeight.ExtraBold else FontWeight.Normal,
            fontSize = if (isCurrentGames) 16.sp else 18.sp,
            color = if (isSetWinner) MaterialTheme.colorScheme.primary 
                    else if (isCurrentGames) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurface
        )
        if (tieBreakPoints != null) {
            Text(
                text = tieBreakPoints.toString(),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            )
        }
    }
}
