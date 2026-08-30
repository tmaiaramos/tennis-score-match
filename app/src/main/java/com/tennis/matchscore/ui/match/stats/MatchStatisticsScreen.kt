package com.tennis.matchscore.ui.match.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.domain.model.ShotType
import com.tennis.matchscore.ui.match.CompletedSetUiState

private fun formatShortName(fullName: String): String {
    val parts = fullName.trim().split("\\s+".toRegex())
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts.first()
        else -> "${parts.first().firstOrNull()?.uppercase() ?: ""}. ${parts.last()}"
    }
}

private fun formatVal(v: Int): String = if (v == 0) "-" else v.toString()
private fun formatPct(v: Int): String = if (v == 0) "-" else "$v%"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchStatisticsScreen(
    matchId: Long,
    viewModel: MatchStatisticsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Essential", "Detailed", "By Shot")

    LaunchedEffect(matchId) {
        viewModel.loadStatistics(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estatísticas da Partida") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (val state = uiState) {
                is MatchStatisticsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MatchStatisticsUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Placar de Games e Sets no topo
                        ScoreSummary(state)
                        
                        StatisticsContent(state.stats, selectedTabIndex)
                    }
                }
                is MatchStatisticsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(stats: MatchStats, tabIndex: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (tabIndex) {
            0 -> essentialTab(stats)
            1 -> detailedTab(stats)
            2 -> byShotTab(stats)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.essentialTab(stats: MatchStats) {
    item { GroupHeader("1 - Service", stats) }
    item { StatRow("% 1st service", formatPct(stats.p1.firstServePercentage), formatPct(stats.p2.firstServePercentage)) }
    item { StatRow("Aces", formatVal(stats.p1.aces), formatVal(stats.p2.aces)) }
    item { StatRow("Double Faults", formatVal(stats.p1.doubleFaults), formatVal(stats.p2.doubleFaults)) }

    item { GroupHeader("2 - Points", stats) }
    item { StatRow("Total points won", formatVal(stats.p1.totalPointsWon), formatVal(stats.p2.totalPointsWon)) }
    item { 
        ComplexStatRow(
            label = "Winners (BH/FH)",
            p1Total = stats.p1.winnersBH + stats.p1.winnersFH, p1BH = stats.p1.winnersBH, p1FH = stats.p1.winnersFH,
            p2Total = stats.p2.winnersBH + stats.p2.winnersFH, p2BH = stats.p2.winnersBH, p2FH = stats.p2.winnersFH
        )
    }
    item { 
        ComplexStatRow(
            label = "Unforced Errors (BH/FH)",
            p1Total = stats.p1.unforcedErrorsBH + stats.p1.unforcedErrorsFH, p1BH = stats.p1.unforcedErrorsBH, p1FH = stats.p1.unforcedErrorsFH,
            p2Total = stats.p2.unforcedErrorsBH + stats.p2.unforcedErrorsFH, p2BH = stats.p2.unforcedErrorsBH, p2FH = stats.p2.unforcedErrorsFH
        )
    }
    item { StatRow("Agressive Margin", formatVal(stats.p1.aggressiveMargin), formatVal(stats.p2.aggressiveMargin)) }

    item { GroupHeader("3 - Conversion", stats) }
    item { StatRow("Receiving pts won", formatPct(stats.p1.receivingPointsWonPercentage), formatPct(stats.p2.receivingPointsWonPercentage)) }
    item { StatRow("Break points", "${formatVal(stats.p1.breakPointsWon)}/${formatVal(stats.p1.breakPointsTotal)}", "${formatVal(stats.p2.breakPointsWon)}/${formatVal(stats.p2.breakPointsTotal)}") }
    item { StatRow("1st service pts won", formatPct(stats.p1.firstServePointsWonPercentage), formatPct(stats.p2.firstServePointsWonPercentage)) }
    item { StatRow("Net points", "${formatVal(stats.p1.netPointsWon)}/${formatVal(stats.p1.netPointsTotal)}", "${formatVal(stats.p2.netPointsWon)}/${formatVal(stats.p2.netPointsTotal)}") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.detailedTab(stats: MatchStats) {
    item { GroupHeader("1 - Service", stats) }
    item { StatRow("Total Services", formatVal(stats.p1.totalServes), formatVal(stats.p2.totalServes)) }
    item { StatRow("% 1st service", formatPct(stats.p1.firstServePercentage), formatPct(stats.p2.firstServePercentage)) }
    item { StatRow("Aces", formatVal(stats.p1.aces), formatVal(stats.p2.aces)) }
    item { StatRow("Double Faults", formatVal(stats.p1.doubleFaults), formatVal(stats.p2.doubleFaults)) }
    item { StatRow("1st services in", formatVal(stats.p1.firstServesIn), formatVal(stats.p2.firstServesIn)) }
    item { StatRow("2nd services", formatVal(stats.p1.totalServes - stats.p1.firstServesIn), formatVal(stats.p2.totalServes - stats.p2.firstServesIn)) }

    item { GroupHeader("2 - Return", stats) }
    item { 
        ComplexStatRow(
            label = "Return errors (BH/FH)",
            p1Total = stats.p1.returnErrorsBH + stats.p1.returnErrorsFH, p1BH = stats.p1.returnErrorsBH, p1FH = stats.p1.returnErrorsFH,
            p2Total = stats.p2.returnErrorsBH + stats.p2.returnErrorsFH, p2BH = stats.p2.returnErrorsBH, p2FH = stats.p2.returnErrorsFH
        )
    }
    item { 
        ComplexStatRow(
            label = "Return winners (BH/FH)",
            p1Total = stats.p1.returnWinnersBH + stats.p1.returnWinnersFH, p1BH = stats.p1.returnWinnersBH, p1FH = stats.p1.returnWinnersFH,
            p2Total = stats.p2.returnWinnersBH + stats.p2.returnWinnersFH, p2BH = stats.p2.returnWinnersBH, p2FH = stats.p2.returnWinnersFH
        )
    }
    item { StatRow("Unreturned 1st serv.", formatVal(stats.p1.unreturnedFirstServes), formatVal(stats.p2.unreturnedFirstServes)) }
    item { StatRow("Unreturned 2nd serv.", formatVal(stats.p1.unreturnedSecondServes), formatVal(stats.p2.unreturnedSecondServes)) }

    item { GroupHeader("3 - Points", stats) }
    item { StatRow("Total points won", formatVal(stats.p1.totalPointsWon), formatVal(stats.p2.totalPointsWon)) }
    item { 
        ComplexStatRow(
            label = "Winners (BH/FH)",
            p1Total = stats.p1.winnersBH + stats.p1.winnersFH, p1BH = stats.p1.winnersBH, p1FH = stats.p1.winnersFH,
            p2Total = stats.p2.winnersBH + stats.p2.winnersFH, p2BH = stats.p2.winnersBH, p2FH = stats.p2.winnersFH
        )
    }
    item { 
        ComplexStatRow(
            label = "Unforced Errors (BH/FH)",
            p1Total = stats.p1.unforcedErrorsBH + stats.p1.unforcedErrorsFH, p1BH = stats.p1.unforcedErrorsBH, p1FH = stats.p1.unforcedErrorsFH,
            p2Total = stats.p2.unforcedErrorsBH + stats.p2.unforcedErrorsFH, p2BH = stats.p2.unforcedErrorsBH, p2FH = stats.p2.unforcedErrorsFH
        )
    }
    item { 
        ComplexStatRow(
            label = "Forced Errors (BH/FH)",
            p1Total = stats.p1.forcedErrorsBH + stats.p1.forcedErrorsFH, p1BH = stats.p1.forcedErrorsBH, p1FH = stats.p1.forcedErrorsFH,
            p2Total = stats.p2.forcedErrorsBH + stats.p2.forcedErrorsFH, p2BH = stats.p2.forcedErrorsBH, p2FH = stats.p2.forcedErrorsFH
        )
    }
    item { StatRow("Agressive margin", formatVal(stats.p1.aggressiveMargin), formatVal(stats.p2.aggressiveMargin)) }

    item { GroupHeader("4 - Conversion", stats) }
    item { StatRow("2nd service pts won", formatPct(stats.p1.secondServePointsWonPercentage), formatPct(stats.p2.secondServePointsWonPercentage)) }
    item { StatRow("1st service pts won", formatPct(stats.p1.firstServePointsWonPercentage), formatPct(stats.p2.firstServePointsWonPercentage)) }
    item { StatRow("Receiving pts won", formatPct(stats.p1.receivingPointsWonPercentage), formatPct(stats.p2.receivingPointsWonPercentage)) }
    item { StatRow("Break points", "${formatVal(stats.p1.breakPointsWon)}/${formatVal(stats.p1.breakPointsTotal)}", "${formatVal(stats.p2.breakPointsWon)}/${formatVal(stats.p2.breakPointsTotal)}") }
    item { StatRow("Net Points", "${formatVal(stats.p1.netPointsWon)}/${formatVal(stats.p1.netPointsTotal)}", "${formatVal(stats.p2.netPointsWon)}/${formatVal(stats.p2.netPointsTotal)}") }
    item { StatRow("Approach points", "${formatVal(stats.p1.approachPointsWon)}/${formatVal(stats.p1.approachPointsTotal)}", "${formatVal(stats.p2.approachPointsWon)}/${formatVal(stats.p2.approachPointsTotal)}") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.byShotTab(stats: MatchStats) {
    val shots = listOf(
        "Ground Stroke" to ShotType.GROUND,
        "Slice" to ShotType.SLICE,
        "Volley" to ShotType.VOLLEY,
        "Drop-shot" to ShotType.DROP,
        "Smash" to ShotType.SMASH,
        "Lob" to ShotType.LOB,
        "Swing" to ShotType.SWING
    )

    shots.forEachIndexed { index, (label, type) ->
        item { GroupHeader("${index + 1} - $label", stats) }
        val s1 = stats.p1.shotStats[type] ?: ShotTypeStats()
        val s2 = stats.p2.shotStats[type] ?: ShotTypeStats()
        item { 
            ComplexStatRow(
                label = "Winners (BH/FH)",
                p1Total = s1.winnersBH + s1.winnersFH, p1BH = s1.winnersBH, p1FH = s1.winnersFH,
                p2Total = s2.winnersBH + s2.winnersFH, p2BH = s2.winnersBH, p2FH = s2.winnersFH
            )
        }
        item { 
            ComplexStatRow(
                label = "Forced Errors (BH/FH)",
                p1Total = s1.forcedErrorsBH + s1.forcedErrorsFH, p1BH = s1.forcedErrorsBH, p1FH = s1.forcedErrorsFH,
                p2Total = s2.forcedErrorsBH + s2.forcedErrorsFH, p2BH = s2.forcedErrorsBH, p2FH = s2.forcedErrorsFH
            )
        }
        item { 
            ComplexStatRow(
                label = "Unforced Errors (BH/FH)",
                p1Total = s1.unforcedErrorsBH + s1.unforcedErrorsFH, p1BH = s1.unforcedErrorsBH, p1FH = s1.unforcedErrorsFH,
                p2Total = s2.unforcedErrorsBH + s2.unforcedErrorsFH, p2BH = s2.unforcedErrorsBH, p2FH = s2.unforcedErrorsFH
            )
        }
    }
}

@Composable
private fun ScoreSummary(state: MatchStatisticsUiState.Success) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Resumo da Partida", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 14.sp)
                state.completedSets.forEach { set ->
                    Text(text = "S${set.setNumber}", modifier = Modifier.width(36.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            PlayerScoreSummaryRow(state.stats.p1.playerName, state.completedSets, true)
            Spacer(modifier = Modifier.height(4.dp))
            PlayerScoreSummaryRow(state.stats.p2.playerName, state.completedSets, false)
        }
    }
}

@Composable
private fun PlayerScoreSummaryRow(playerName: String, sets: List<CompletedSetUiState>, isPlayer1: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = formatShortName(playerName), modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        sets.forEach { set ->
            val games = if (isPlayer1) set.player1Games else set.player2Games
            val opponentGames = if (isPlayer1) set.player2Games else set.player1Games
            
            val isWinner = games > opponentGames

            Text(
                text = games.toString(),
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center,
                fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Normal,
                fontSize = 15.sp,
                color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun GroupHeader(title: String, stats: MatchStats) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1.2f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = formatShortName(stats.p1.playerName),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = formatShortName(stats.p2.playerName),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatRow(label: String, v1: String, v2: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1.2f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(text = v1, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 14.sp)
        Text(text = v2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 14.sp)
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun ComplexStatRow(label: String, p1Total: Int, p1BH: Int, p1FH: Int, p2Total: Int, p2BH: Int, p2FH: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1.2f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ComplexStatCell(total = p1Total, bh = p1BH, fh = p1FH)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ComplexStatCell(total = p2Total, bh = p2BH, fh = p2FH)
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun ComplexStatCell(total: Int, bh: Int, fh: Int) {
    if (total == 0) {
        Text("-", fontSize = 14.sp)
        return
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
        // Lado Esquerdo (Backhand)
        Text(
            text = formatVal(bh),
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 2.dp)
        )
        // Total (Centralizado e mais alto)
        Text(
            text = total.toString(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        // Lado Direito (Forehand)
        Text(
            text = formatVal(fh),
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}
