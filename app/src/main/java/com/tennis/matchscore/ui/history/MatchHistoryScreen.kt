package com.tennis.matchscore.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.data.local.relation.MatchWithDetails
import com.tennis.matchscore.ui.match.setup.ScoringMode
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
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Partidas", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (matches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma partida registrada até o momento.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(matches, key = { it.match.id }) { matchDetails ->
                        MatchHistoryCard(
                            matchDetails = matchDetails,
                            onResumeClick = { onMatchClick(it.match.id) },
                            onDeleteClick = { matchToDelete = it.match.id },
                            onViewStatsClick = { onViewStatsClick(it.match.id) }
                        )
                    }
                }
            }
        }
    }

    if (matchToDelete != null) {
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            title = { Text("Excluir Partida") },
            text = { Text("Deseja realmente excluir este registro do histórico?") },
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
    matchDetails: MatchWithDetails,
    onResumeClick: (MatchWithDetails) -> Unit,
    onDeleteClick: (MatchWithDetails) -> Unit,
    onViewStatsClick: (MatchWithDetails) -> Unit
) {
    val match = matchDetails.match
    val p1Name = "${matchDetails.player1.firstName} ${matchDetails.player1.lastName}".trim().ifBlank { "Jogador 1" }
    val p2Name = "${matchDetails.player2.firstName} ${matchDetails.player2.lastName}".trim().ifBlank { "Jogador 2" }

    val isP1Winner = match.winnerId == match.player1Id
    val isP2Winner = match.winnerId == match.player2Id

    val completedSets = matchDetails.sets.sortedBy { it.setNumber }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val formattedDate = dateFormatter.format(Date(match.createdAt))

    val advantageText = if (matchDetails.format.hasAdvantage) "Ad" else "No-Ad"
    val isInProgress = match.status != com.tennis.matchscore.domain.model.MatchStatus.FINISHED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isInProgress) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        contentColor = Color(0xFFE65100),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "EM ANDAMENTO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${matchDetails.format.name} ($advantageText)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = match.courtType.displayName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Jogador 1 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = p1Name,
                        fontWeight = if (isP1Winner) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                    if (isP1Winner) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Vencedor",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    completedSets.forEach { set ->
                        val isSuperTieBreak = (((set.player1Games + set.player2Games <= 1) &&
                                set.tieBreakPointsPlayer1 != null && set.tieBreakPointsPlayer2 != null))

                        SetScoreItem(
                            games = set.player1Games,
                            myTbPoints = set.tieBreakPointsPlayer1,
                            opponentTbPoints = set.tieBreakPointsPlayer2,
                            isWinner = set.winnerPlayerId == match.player1Id || set.player1Games > set.player2Games,
                            isSuperTieBreak = isSuperTieBreak
                        )
                    }
                    if (isInProgress) {
                        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "${match.player1GamesCurrentSet}",
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = match.player1PointsCurrentGame,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Jogador 2 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = p2Name,
                        fontWeight = if (isP2Winner) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                    if (isP2Winner) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Vencedor",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    completedSets.forEach { set ->
                        val isSuperTieBreak = (((set.player1Games + set.player2Games <= 1) &&
                                set.tieBreakPointsPlayer1 != null && set.tieBreakPointsPlayer2 != null))

                        SetScoreItem(
                            games = set.player2Games,
                            myTbPoints = set.tieBreakPointsPlayer2,
                            opponentTbPoints = set.tieBreakPointsPlayer1,
                            isWinner = set.winnerPlayerId == match.player2Id || set.player2Games > set.player1Games,
                            isSuperTieBreak = isSuperTieBreak
                        )
                    }
                    if (isInProgress) {
                        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "${match.player2GamesCurrentSet}",
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = match.player2PointsCurrentGame,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (isInProgress) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDeleteClick(matchDetails) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onResumeClick(matchDetails) },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retomar Partida")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (match.trackingLevel == com.tennis.matchscore.domain.model.TrackingLevel.ADVANCED) {
                        TextButton(
                            onClick = { onViewStatsClick(matchDetails) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Estatísticas")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    IconButton(onClick = { onDeleteClick(matchDetails) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetScoreItem(
    games: Int,
    myTbPoints: Int?,
    opponentTbPoints: Int?,
    isWinner: Boolean,
    isSuperTieBreak: Boolean,
) {
    val hasTieBreak = (myTbPoints != null && opponentTbPoints != null)
    val isLoser = !isWinner

    val displayScore = if (isSuperTieBreak && myTbPoints != null) {
        myTbPoints.toString()
    } else {
        games.toString()
    }

    Box(
        modifier = Modifier
            .width(32.dp)
            .height(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayScore,
            fontSize = 16.sp,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!isSuperTieBreak && hasTieBreak && isLoser && myTbPoints != null) {
            Text(
                text = myTbPoints.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 10.dp, y = (-5).dp)
            )
        }
    }
}
