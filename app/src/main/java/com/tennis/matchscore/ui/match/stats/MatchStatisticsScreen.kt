package com.tennis.matchscore.ui.match.stats

import androidx.compose.foundation.background
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

private fun formatVal(v: Int, hasData: Boolean = true): String = if (v == 0 && !hasData) "-" else v.toString()
private fun formatPct(v: Int, hasData: Boolean = true): String = if (v == 0 && !hasData) "-" else "$v%"

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
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.tennis.matchscore.R.drawable.ic_app_logo_png),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Estatísticas da Partida")
                    }
                },
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp) // Mais compacto
    ) {
        when (tabIndex) {
            0 -> essentialTab(stats)
            1 -> detailedTab(stats)
            2 -> byShotTab(stats)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.essentialTab(stats: MatchStats) {
    val p1Active = stats.p1.totalPointsServed + stats.p1.totalPointsReceived > 0
    val p2Active = stats.p2.totalPointsServed + stats.p2.totalPointsReceived > 0

    item { GroupHeader("1 - Service", stats) }
    item { StatRow("% 1st service", formatPct(stats.p1.firstServePercentage, stats.p1.totalServes > 0), formatPct(stats.p2.firstServePercentage, stats.p2.totalServes > 0)) }
    item { StatRow("Aces", formatVal(stats.p1.aces, p1Active), formatVal(stats.p2.aces, p2Active)) }
    item { StatRow("Double Faults", formatVal(stats.p1.doubleFaults, p1Active), formatVal(stats.p2.doubleFaults, p2Active)) }

    item { GroupHeader("2 - Points", stats) }
    item { StatRow("Total points won", formatVal(stats.p1.totalPointsWon, p1Active), formatVal(stats.p2.totalPointsWon, p2Active)) }
    item { 
        ComplexStatRow(
            label = "Winners",
            p1Total = stats.p1.winnersBH + stats.p1.winnersFH + stats.p1.aces, // Soma ACE (Item 5)
            p1BH = stats.p1.winnersBH, p1FH = stats.p1.winnersFH,
            p2Total = stats.p2.winnersBH + stats.p2.winnersFH + stats.p2.aces, // Soma ACE (Item 5)
            p2BH = stats.p2.winnersBH, p2FH = stats.p2.winnersFH,
            hasData = p1Active || p2Active
        )
    }
    item { 
        ComplexStatRow(
            label = "Unforced Errors",
            p1Total = stats.p1.unforcedErrorsBH + stats.p1.unforcedErrorsFH + stats.p1.doubleFaults, // Soma DF (Item 5)
            p1BH = stats.p1.unforcedErrorsBH, p1FH = stats.p1.unforcedErrorsFH,
            p2Total = stats.p2.unforcedErrorsBH + stats.p2.unforcedErrorsFH + stats.p2.doubleFaults, // Soma DF (Item 5)
            p2BH = stats.p2.unforcedErrorsBH, p2FH = stats.p2.unforcedErrorsFH,
            hasData = p1Active || p2Active
        )
    }
    item { StatRow("Agressive Margin", formatVal(stats.p1.aggressiveMargin, p1Active), formatVal(stats.p2.aggressiveMargin, p2Active)) }

    item { GroupHeader("3 - Conversion", stats) }
    item { StatRow("Receiving pts won", formatPct(stats.p1.receivingPointsWonPercentage, stats.p1.totalPointsReceived > 0), formatPct(stats.p2.receivingPointsWonPercentage, stats.p2.totalPointsReceived > 0)) }
    item { StatRow("Break points", "${formatVal(stats.p1.breakPointsWon, p1Active)}/${formatVal(stats.p1.breakPointsTotal, p1Active)}", "${formatVal(stats.p2.breakPointsWon, p2Active)}/${formatVal(stats.p2.breakPointsTotal, p2Active)}") }
    item { StatRow("1st service pts won", formatPct(stats.p1.firstServePointsWonPercentage, stats.p1.firstServesIn > 0), formatPct(stats.p2.firstServePointsWonPercentage, stats.p2.firstServesIn > 0)) }
    item { StatRow("Net points", "${formatVal(stats.p1.netPointsWon, stats.p1.netPointsTotal > 0)}/${formatVal(stats.p1.netPointsTotal, stats.p1.netPointsTotal > 0)}", "${formatVal(stats.p2.netPointsWon, stats.p2.netPointsTotal > 0)}/${formatVal(stats.p2.netPointsTotal, stats.p2.netPointsTotal > 0)}") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.detailedTab(stats: MatchStats) {
    item { GroupHeader("1 - Service", stats) }
    item { StatRow("Total Services", formatVal(stats.p1.totalServes), formatVal(stats.p2.totalServes)) }
    item { StatRow("% 1st service", formatPct(stats.p1.firstServePercentage), formatPct(stats.p2.firstServePercentage)) }
    item { StatRow("Aces", formatVal(stats.p1.aces), formatVal(stats.p2.aces)) }
    item { StatRow("Double Faults", formatVal(stats.p1.doubleFaults), formatVal(stats.p2.doubleFaults)) }
    item { StatRow("1st services in", formatVal(stats.p1.firstServesIn), formatVal(stats.p2.firstServesIn)) }
    item { StatRow("2nd services", formatVal(stats.p1.secondServesIn), formatVal(stats.p2.secondServesIn)) }

    item { GroupHeader("2 - Return", stats) }
    item { 
        ComplexStatRow(
            label = "Return errors",
            p1Total = stats.p1.returnErrorsBH + stats.p1.returnErrorsFH, p1BH = stats.p1.returnErrorsBH, p1FH = stats.p1.returnErrorsFH,
            p2Total = stats.p2.returnErrorsBH + stats.p2.returnErrorsFH, p2BH = stats.p2.returnErrorsBH, p2FH = stats.p2.returnErrorsFH
        )
    }
    item { 
        ComplexStatRow(
            label = "Return winners",
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
            label = "Winners",
            p1Total = stats.p1.winnersBH + stats.p1.winnersFH, p1BH = stats.p1.winnersBH, p1FH = stats.p1.winnersFH,
            p2Total = stats.p2.winnersBH + stats.p2.winnersFH, p2BH = stats.p2.winnersBH, p2FH = stats.p2.winnersFH
        )
    }
    item { 
        ComplexStatRow(
            label = "Unforced Errors",
            p1Total = stats.p1.unforcedErrorsBH + stats.p1.unforcedErrorsFH, p1BH = stats.p1.unforcedErrorsBH, p1FH = stats.p1.unforcedErrorsFH,
            p2Total = stats.p2.unforcedErrorsBH + stats.p2.unforcedErrorsFH, p2BH = stats.p2.unforcedErrorsBH, p2FH = stats.p2.unforcedErrorsFH
        )
    }
    item { 
        ComplexStatRow(
            label = "Forced Errors",
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
                label = "Winners",
                p1Total = s1.winnersBH + s1.winnersFH, p1BH = s1.winnersBH, p1FH = s1.winnersFH,
                p2Total = s2.winnersBH + s2.winnersFH, p2BH = s2.winnersBH, p2FH = s2.winnersFH
            )
        }
        item { 
            ComplexStatRow(
                label = "Forced Errors",
                p1Total = s1.forcedErrorsBH + s1.forcedErrorsFH, p1BH = s1.forcedErrorsBH, p1FH = s1.forcedErrorsFH,
                p2Total = s2.forcedErrorsBH + s2.forcedErrorsFH, p2BH = s2.forcedErrorsBH, p2FH = s2.forcedErrorsFH
            )
        }
        item { 
            ComplexStatRow(
                label = "Unforced Errors",
                p1Total = s1.unforcedErrorsBH + s1.unforcedErrorsFH, p1BH = s1.unforcedErrorsBH, p1FH = s1.unforcedErrorsFH,
                p2Total = s2.unforcedErrorsBH + s2.unforcedErrorsFH, p2BH = s2.unforcedErrorsBH, p2FH = s2.unforcedErrorsFH
            )
        }
    }
}

@Composable
private fun ScoreSummary(state: MatchStatisticsUiState.Success) {
    val isInProgress = !state.isMatchFinished
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Placar", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    state.completedSets.forEach { set ->
                        Text(text = "S${set.setNumber}", modifier = Modifier.width(34.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    if (isInProgress) {
                        val currentSetNum = state.completedSets.size + 1
                        Text(text = "S$currentSetNum", modifier = Modifier.width(34.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Pts", modifier = Modifier.width(34.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            PlayerScoreSummaryRow(state.stats.p1.playerName, state.completedSets, true, isInProgress, state)
            Spacer(modifier = Modifier.height(2.dp))
            PlayerScoreSummaryRow(state.stats.p2.playerName, state.completedSets, false, isInProgress, state)
        }
    }
}

@Composable
private fun PlayerScoreSummaryRow(
    playerName: String, 
    sets: List<CompletedSetUiState>, 
    isPlayer1: Boolean,
    isInProgress: Boolean,
    state: MatchStatisticsUiState.Success
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = formatShortName(playerName), modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            sets.forEach { set ->
                val games = if (isPlayer1) set.player1Games else set.player2Games
                val opponentGames = if (isPlayer1) set.player2Games else set.player1Games
                val tieBreakPoints = if (isPlayer1) set.tieBreakPointsPlayer1 else set.tieBreakPointsPlayer2
                
                val isWinner = games > opponentGames

                Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = games.toString(),
                        fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = 14.sp,
                        color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (tieBreakPoints != null) {
                        Text(
                            text = tieBreakPoints.toString(),
                            fontSize = 8.sp,
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                        )
                    }
                }
            }
            
            if (isInProgress) {
                // Games do set atual
                val currentGames = if (isPlayer1) state.player1GamesCurrentSet else state.player2GamesCurrentSet
                val opponentGames = if (isPlayer1) state.player2GamesCurrentSet else state.player1GamesCurrentSet
                val isWinningSet = currentGames > opponentGames

                Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = currentGames.toString(),
                        fontWeight = if (isWinningSet) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        color = if (isWinningSet) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                // Pontos do game atual
                val points = if (isPlayer1) state.player1Points else state.player2Points
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = points,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String, stats: MatchStats) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
        Text(text = label, modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(text = v1, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp)
        Text(text = v2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp)
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
}

@Composable
private fun ComplexStatRow(label: String, p1Total: Int, p1BH: Int, p1FH: Int, p2Total: Int, p2BH: Int, p2FH: Int, hasData: Boolean = true) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 11.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "BH   FH",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                lineHeight = 10.sp
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ComplexStatCell(total = p1Total, bh = p1BH, fh = p1FH, hasData = hasData)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ComplexStatCell(total = p2Total, bh = p2BH, fh = p2FH, hasData = hasData)
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
}

@Composable
private fun ComplexStatCell(total: Int, bh: Int, fh: Int, hasData: Boolean = true) {
    if (total == 0 && !hasData) {
        Text("-", fontSize = 13.sp)
        return
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
        // Lado Esquerdo (Backhand)
        Text(
            text = formatVal(bh, hasData),
            fontSize = 12.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 0.dp)
        )
        // Total (Elevado)
        Text(
            text = total.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 5.dp, start = 4.dp, end = 4.dp)
        )
        // Lado Direito (Forehand)
        Text(
            text = formatVal(fh, hasData),
            fontSize = 12.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 0.dp)
        )
    }
}
