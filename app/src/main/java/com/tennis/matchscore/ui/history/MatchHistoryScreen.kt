package com.tennis.matchscore.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.data.local.relation.MatchWithDetails
import com.tennis.matchscore.domain.model.MatchStatus
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchHistoryScreen(
    viewModel: MatchHistoryViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val matches by viewModel.matches.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Partidas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
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

    val isFinished = match.status == MatchStatus.FINISHED
    val isP1Winner = match.winnerId == match.player1Id
    val isP2Winner = match.winnerId == match.player2Id

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
                Surface(
                    color = if (isFinished) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isFinished) "FINALIZADA" else "EM ANDAMENTO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFinished) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Text(
                    text = "Formato: ${matchDetails.format.name}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Jogador 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isP1Winner) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Vencedor",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = p1Name,
                        fontWeight = if (isP1Winner) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                }
            }

            // Jogador 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isP2Winner) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Vencedor",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = p2Name,
                        fontWeight = if (isP2Winner) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}