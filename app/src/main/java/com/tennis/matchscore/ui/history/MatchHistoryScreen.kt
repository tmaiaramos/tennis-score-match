package com.tennis.matchscore.ui.history

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

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = match.match.courtType.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linha Jogador 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.player1.firstName} ${match.player1.lastName}",
                    modifier = Modifier.weight(1f),
                    fontWeight = if (match.match.winnerId == match.player1.id) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp
                )
                Row {
                    match.sets.sortedBy { it.setNumber }.forEach { set ->
                        SetScoreItem(set.player1Games, set.player2Games, set.tieBreakPointsPlayer1, match.match.winnerId == match.player1.id && set.winnerPlayerId == match.player1.id, false)
                    }
                }
                if (match.match.winnerId == match.player1.id) {
                    Text(text = "🏆", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
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
                    fontSize = 16.sp
                )
                Row {
                    match.sets.sortedBy { it.setNumber }.forEach { set ->
                        SetScoreItem(set.player2Games, set.player1Games, set.tieBreakPointsPlayer2, match.match.winnerId == match.player2.id && set.winnerPlayerId == match.player2.id, false)
                    }
                }
                if (match.match.winnerId == match.player2.id) {
                    Text(text = "🏆", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
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
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
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
    isSuperTieBreak: Boolean
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
            fontSize = 18.sp,
            color = if (isSetWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
