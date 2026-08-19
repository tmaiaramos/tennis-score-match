package com.tennis.matchscore.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.data.local.relation.MatchWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchHistoryScreen(
    viewModel: MatchHistoryViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
) {
    val matches by viewModel.matches.collectAsState()

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
                        MatchHistoryCard(matchDetails = matchDetails)
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchHistoryCard(matchDetails: MatchWithDetails) {
    val match = matchDetails.match
    val p1Name = "${matchDetails.player1.firstName} ${matchDetails.player1.lastName}".trim().ifBlank { "Jogador 1" }
    val p2Name = "${matchDetails.player2.firstName} ${matchDetails.player2.lastName}".trim().ifBlank { "Jogador 2" }

    val isP1Winner = match.winnerId == match.player1Id
    val isP2Winner = match.winnerId == match.player2Id

    val completedSets = matchDetails.sets.sortedBy { it.setNumber }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val formattedDate = dateFormatter.format(Date(match.createdAt))

    val advantageText = if (matchDetails.format.hasAdvantage) "Ad" else "No-Ad"

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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (completedSets.isNotEmpty()) {
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
                    } else {
                        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "${match.player1GamesCurrentSet}",
                                fontWeight = if (isP1Winner) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp,
                                color = if (isP1Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (completedSets.isNotEmpty()) {
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
                    } else {
                        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "${match.player2GamesCurrentSet}",
                                fontWeight = if (isP2Winner) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp,
                                color = if (isP2Winner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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