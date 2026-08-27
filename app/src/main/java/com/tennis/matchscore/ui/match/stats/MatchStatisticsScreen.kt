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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.domain.model.ShotType

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
                }
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
                    StatisticsContent(state.stats, selectedTabIndex)
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Spacer(modifier = Modifier.weight(1.5f))
                Text(text = stats.p1.playerName, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = stats.p2.playerName, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        when (tabIndex) {
            0 -> essentialTab(stats)
            1 -> detailedTab(stats)
            2 -> byShotTab(stats)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.essentialTab(stats: MatchStats) {
    item { GroupHeader("1 - Service") }
    item { StatRow("% 1st service", "${stats.p1.firstServePercentage}%", "${stats.p2.firstServePercentage}%") }
    item { StatRow("Aces", stats.p1.aces.toString(), stats.p2.aces.toString()) }
    item { StatRow("Double Faults", stats.p1.doubleFaults.toString(), stats.p2.doubleFaults.toString()) }

    item { GroupHeader("2 - Points") }
    item { StatRow("Total points won", stats.p1.totalPointsWon.toString(), stats.p2.totalPointsWon.toString()) }
    item { StatRow("Winners (BH / FH)", "${stats.p1.winnersBH} / ${stats.p1.winnersFH}", "${stats.p2.winnersBH} / ${stats.p2.winnersFH}") }
    item { StatRow("Unforced Errors (BH / FH)", "${stats.p1.unforcedErrorsBH} / ${stats.p1.unforcedErrorsFH}", "${stats.p2.unforcedErrorsBH} / ${stats.p2.unforcedErrorsFH}") }
    item { StatRow("Aggressive Margin", stats.p1.aggressiveMargin.toString(), stats.p2.aggressiveMargin.toString()) }

    item { GroupHeader("3 - Conversion") }
    item { StatRow("Receiving pts won", stats.p1.receivingPointsWon.toString(), stats.p2.receivingPointsWon.toString()) }
    item { StatRow("Break points", "${stats.p1.breakPointsWon}/${stats.p1.breakPointsTotal}", "${stats.p2.breakPointsWon}/${stats.p2.breakPointsTotal}") }
    item { StatRow("1st service pts won", stats.p1.firstServesWon.toString(), stats.p2.firstServesWon.toString()) }
    item { StatRow("Net points", stats.p1.netPointsWon.toString(), stats.p2.netPointsWon.toString()) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.detailedTab(stats: MatchStats) {
    item { GroupHeader("1 - Service") }
    item { StatRow("Total Services", stats.p1.totalServes.toString(), stats.p2.totalServes.toString()) }
    item { StatRow("% 1st service", "${stats.p1.firstServePercentage}%", "${stats.p2.firstServePercentage}%") }
    item { StatRow("Aces", stats.p1.aces.toString(), stats.p2.aces.toString()) }
    item { StatRow("Double Faults", stats.p1.doubleFaults.toString(), stats.p2.doubleFaults.toString()) }
    item { StatRow("1st services in", stats.p1.firstServesIn.toString(), stats.p2.firstServesIn.toString()) }
    item { StatRow("2nd services", (stats.p1.totalServes - stats.p1.firstServesIn).toString(), (stats.p2.totalServes - stats.p2.firstServesIn).toString()) }

    item { GroupHeader("2 - Return") }
    item { StatRow("Return errors (BH / FH)", "${stats.p1.returnErrorsBH} / ${stats.p1.returnErrorsFH}", "${stats.p2.returnErrorsBH} / ${stats.p2.returnErrorsFH}") }
    item { StatRow("Return winners (BH / FH)", "${stats.p1.returnWinnersBH} / ${stats.p1.returnWinnersFH}", "${stats.p2.returnWinnersBH} / ${stats.p2.returnWinnersFH}") }
    item { StatRow("Unreturned 1st serv.", stats.p1.unreturnedFirstServes.toString(), stats.p2.unreturnedFirstServes.toString()) }
    item { StatRow("Unreturned 2nd serv.", stats.p1.unreturnedSecondServes.toString(), stats.p2.unreturnedSecondServes.toString()) }

    item { GroupHeader("3 - Points") }
    item { StatRow("Total points won", stats.p1.totalPointsWon.toString(), stats.p2.totalPointsWon.toString()) }
    item { StatRow("Winners (BH / FH)", "${stats.p1.winnersBH} / ${stats.p1.winnersFH}", "${stats.p2.winnersBH} / ${stats.p2.winnersFH}") }
    item { StatRow("Unforced Erros (BH / FH)", "${stats.p1.unforcedErrorsBH} / ${stats.p1.unforcedErrorsFH}", "${stats.p2.unforcedErrorsBH} / ${stats.p2.unforcedErrorsFH}") }
    item { StatRow("Forced Erros (BH / FH)", "${stats.p1.forcedErrorsBH} / ${stats.p1.forcedErrorsFH}", "${stats.p2.forcedErrorsBH} / ${stats.p2.forcedErrorsFH}") }
    item { StatRow("Aggressive margin", stats.p1.aggressiveMargin.toString(), stats.p2.aggressiveMargin.toString()) }

    item { GroupHeader("4 - Conversion") }
    item { StatRow("2nd service pts won", stats.p1.secondServesWon.toString(), stats.p2.secondServesWon.toString()) }
    item { StatRow("1st service pts won", stats.p1.firstServesWon.toString(), stats.p2.firstServesWon.toString()) }
    item { StatRow("Receiving pts won", stats.p1.receivingPointsWon.toString(), stats.p2.receivingPointsWon.toString()) }
    item { StatRow("Break points", "${stats.p1.breakPointsWon}/${stats.p1.breakPointsTotal}", "${stats.p2.breakPointsWon}/${stats.p2.breakPointsTotal}") }
    item { StatRow("Net Points", stats.p1.netPointsWon.toString(), stats.p2.netPointsWon.toString()) }
    item { StatRow("Approach points", stats.p1.approachPointsWon.toString(), stats.p2.approachPointsWon.toString()) }
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
        item { GroupHeader("${index + 1} - $label") }
        val s1 = stats.p1.shotStats[type] ?: ShotTypeStats()
        val s2 = stats.p2.shotStats[type] ?: ShotTypeStats()
        item { StatRow("Winners (BH / FH)", "${s1.winnersBH} / ${s1.winnersFH}", "${s2.winnersBH} / ${s2.winnersFH}") }
        item { StatRow("Forced errors (BH / FH)", "${s1.forcedErrorsBH} / ${s1.forcedErrorsFH}", "${s2.forcedErrorsBH} / ${s2.forcedErrorsFH}") }
        item { StatRow("Unforced errors (BH / FH)", "${s1.unforcedErrorsBH} / ${s1.unforcedErrorsFH}", "${s2.unforcedErrorsBH} / ${s2.unforcedErrorsFH}") }
    }
}

@Composable
private fun GroupHeader(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun StatRow(label: String, v1: String, v2: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1.5f), fontSize = 13.sp)
        Text(text = v1, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 14.sp)
        Text(text = v2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 14.sp)
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
