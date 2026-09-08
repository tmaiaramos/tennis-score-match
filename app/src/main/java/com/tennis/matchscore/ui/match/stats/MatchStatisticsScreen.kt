package com.tennis.matchscore.ui.match.stats

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.R
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MatchStatisticsScreen(
    matchId: Long,
    viewModel: MatchStatisticsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedSetTab by remember { mutableIntStateOf(0) } // 0 = Total
    val tabs = listOf(
        stringResource(id = R.string.tab_essential),
        stringResource(id = R.string.tab_detailed),
        stringResource(id = R.string.tab_by_shot)
    )

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
                            painter = painterResource(id = R.drawable.ic_app_logo_png),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(id = R.string.screen_stats_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.btn_cancel_back))
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
                        // Seletor de Sets
                        ScrollableTabRow(
                            selectedTabIndex = state.availableSets.indexOf(selectedSetTab).coerceAtLeast(0),
                            edgePadding = 16.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            divider = {}
                        ) {
                            state.availableSets.forEach { setNum ->
                                Tab(
                                    selected = selectedSetTab == setNum,
                                    onClick = { selectedSetTab = setNum },
                                    text = { 
                                        Text(
                                            text = if (setNum == 0) stringResource(id = R.string.stats_total) else stringResource(id = R.string.set_label, setNum),
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedSetTab == setNum) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        // Placar de Games e Sets no topo
                        ScoreSummary(state)
                        
                        val currentStats = if (selectedSetTab == 0) state.totalStats else state.setStats[selectedSetTab] ?: state.totalStats
                        StatisticsContent(currentStats, selectedTabIndex)
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
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        when (tabIndex) {
            0 -> essentialTab(stats)
            1 -> detailedTab(stats)
            2 -> byShotTab(stats)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.essentialTab(stats: MatchStats) {
    stickyHeader { GroupHeader(stringResource(id = R.string.group_service), stats) }
    item { StatRow(stringResource(id = R.string.stat_pct_1st_serve), formatPct(stats.p1.firstServePercentage, stats.p1.totalServes > 0), formatPct(stats.p2.firstServePercentage, stats.p2.totalServes > 0)) }
    item { StatRow(stringResource(id = R.string.stat_aces), formatVal(stats.p1.aces, stats.p1.aces > 0), formatVal(stats.p2.aces, stats.p2.aces > 0)) }
    item { StatRow(stringResource(id = R.string.stat_double_faults), formatVal(stats.p1.doubleFaults, stats.p1.doubleFaults > 0), formatVal(stats.p2.doubleFaults, stats.p2.doubleFaults > 0)) }

    stickyHeader { GroupHeader(stringResource(id = R.string.group_points), stats) }
    item { StatRow(stringResource(id = R.string.stat_total_pts_won), formatVal(stats.p1.totalPointsWon, stats.p1.totalPointsWon > 0), formatVal(stats.p2.totalPointsWon, stats.p2.totalPointsWon > 0)) }
    item { 
        ComplexStatRow(
            label = stringResource(id = R.string.stat_winners),
            p1Total = stats.p1.winnersBH + stats.p1.winnersFH + stats.p1.aces,
            p1BH = stats.p1.winnersBH, p1FH = stats.p1.winnersFH,
            p2Total = stats.p2.winnersBH + stats.p2.winnersFH + stats.p2.aces,
            p2BH = stats.p2.winnersBH, p2FH = stats.p2.winnersFH
        )
    }
    item { 
        ComplexStatRow(
            label = stringResource(id = R.string.stat_unforced_errors),
            p1Total = stats.p1.unforcedErrorsBH + stats.p1.unforcedErrorsFH + stats.p1.doubleFaults,
            p1BH = stats.p1.unforcedErrorsBH, p1FH = stats.p1.unforcedErrorsFH,
            p2Total = stats.p2.unforcedErrorsBH + stats.p2.unforcedErrorsFH + stats.p2.doubleFaults,
            p2BH = stats.p2.unforcedErrorsBH, p2FH = stats.p2.unforcedErrorsFH
        )
    }
    item { StatRow(stringResource(id = R.string.stat_aggressive_margin), formatVal(stats.p1.aggressiveMargin, stats.p1.aggressiveMargin != 0), formatVal(stats.p2.aggressiveMargin, stats.p2.aggressiveMargin != 0)) }

    stickyHeader { GroupHeader(stringResource(id = R.string.group_conversion), stats) }
    item { StatRow(stringResource(id = R.string.stat_receiving_pts_won), formatPct(stats.p1.receivingPointsWonPercentage, stats.p1.totalPointsReceived > 0), formatPct(stats.p2.receivingPointsWonPercentage, stats.p2.totalPointsReceived > 0)) }
    item { StatRow(stringResource(id = R.string.stat_break_points), "${formatVal(stats.p1.breakPointsWon, stats.p1.breakPointsTotal > 0)}/${formatVal(stats.p1.breakPointsTotal, stats.p1.breakPointsTotal > 0)}", "${formatVal(stats.p2.breakPointsWon, stats.p2.breakPointsTotal > 0)}/${formatVal(stats.p2.breakPointsTotal, stats.p2.breakPointsTotal > 0)}") }
    item { StatRow(stringResource(id = R.string.stat_1st_serve_pts_won), formatPct(stats.p1.firstServePointsWonPercentage, stats.p1.firstServesIn > 0), formatPct(stats.p2.firstServePointsWonPercentage, stats.p2.firstServesIn > 0)) }
    item { StatRow(stringResource(id = R.string.stat_net_points), "${formatVal(stats.p1.netPointsWon, stats.p1.netPointsTotal > 0)}/${formatVal(stats.p1.netPointsTotal, stats.p1.netPointsTotal > 0)}", "${formatVal(stats.p2.netPointsWon, stats.p2.netPointsTotal > 0)}/${formatVal(stats.p2.netPointsTotal, stats.p2.netPointsTotal > 0)}") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.detailedTab(stats: MatchStats) {
    stickyHeader { GroupHeader(stringResource(id = R.string.group_service), stats) }
    item { StatRow(stringResource(id = R.string.stat_total_services), formatVal(stats.p1.totalServes, stats.p1.totalServes > 0), formatVal(stats.p2.totalServes, stats.p2.totalServes > 0)) }
    item { StatRow(stringResource(id = R.string.stat_pct_1st_serve), formatPct(stats.p1.firstServePercentage, stats.p1.totalServes > 0), formatPct(stats.p2.firstServePercentage, stats.p2.totalServes > 0)) }
    item { StatRow(stringResource(id = R.string.stat_aces), formatVal(stats.p1.aces, stats.p1.aces > 0), formatVal(stats.p2.aces, stats.p2.aces > 0)) }
    item { StatRow(stringResource(id = R.string.stat_double_faults), formatVal(stats.p1.doubleFaults, stats.p1.doubleFaults > 0), formatVal(stats.p2.doubleFaults, stats.p2.doubleFaults > 0)) }
    item { StatRow(stringResource(id = R.string.stat_1st_serves_in), formatVal(stats.p1.firstServesIn, stats.p1.firstServesIn > 0), formatVal(stats.p2.firstServesIn, stats.p2.firstServesIn > 0)) }
    item { StatRow(stringResource(id = R.string.stat_2nd_serves), formatVal(stats.p1.secondServesIn, stats.p1.secondServesIn > 0), formatVal(stats.p2.secondServesIn, stats.p2.secondServesIn > 0)) }

    stickyHeader { GroupHeader(stringResource(id = R.string.group_return), stats) }
    item { 
        ComplexStatRow(
            label = stringResource(id = R.string.stat_return_errors),
            p1Total = stats.p1.returnErrorsBH + stats.p1.returnErrorsFH, p1BH = stats.p1.returnErrorsBH, p1FH = stats.p1.returnErrorsFH,
            p2Total = stats.p2.returnErrorsBH + stats.p2.returnErrorsFH, p2BH = stats.p2.returnErrorsBH, p2FH = stats.p2.returnErrorsFH
        )
    }
    item { 
        ComplexStatRow(
            label = stringResource(id = R.string.stat_return_winners),
            p1Total = stats.p1.returnWinnersBH + stats.p1.returnWinnersFH, p1BH = stats.p1.returnWinnersBH, p1FH = stats.p1.returnWinnersFH,
            p2Total = stats.p2.returnWinnersBH + stats.p2.returnWinnersFH, p2BH = stats.p2.returnWinnersBH, p2FH = stats.p2.returnWinnersFH
        )
    }
    item { StatRow(stringResource(id = R.string.stat_unreturned_1st), formatVal(stats.p1.unreturnedFirstServes, stats.p1.unreturnedFirstServes > 0), formatVal(stats.p2.unreturnedFirstServes, stats.p2.unreturnedFirstServes > 0)) }
    item { StatRow(stringResource(id = R.string.stat_unreturned_2nd), formatVal(stats.p1.unreturnedSecondServes, stats.p1.unreturnedSecondServes > 0), formatVal(stats.p2.unreturnedSecondServes, stats.p2.unreturnedSecondServes > 0)) }

    stickyHeader { GroupHeader(stringResource(id = R.string.group_points), stats) }
    item { StatRow(stringResource(id = R.string.stat_total_pts_won), formatVal(stats.p1.totalPointsWon, stats.p1.totalPointsWon > 0), formatVal(stats.p2.totalPointsWon, stats.p2.totalPointsWon > 0)) }
    item { 
        ComplexStatRow(
            label = stringResource(id = R.string.stat_winners),
            p1Total = stats.p1.winnersBH + stats.p1.winnersFH + stats.p1.aces, p1BH = stats.p1.winnersBH, p1FH = stats.p1.winnersFH,
            p2Total = stats.p2.winnersBH + stats.p2.winnersFH + stats.p2.aces, p2BH = stats.p2.winnersBH, p2FH = stats.p2.winnersFH
        )
    }
    item { 
        ComplexStatRow(
            label = stringResource(id = R.string.stat_unforced_errors),
            p1Total = stats.p1.unforcedErrorsBH + stats.p1.unforcedErrorsFH + stats.p1.doubleFaults, p1BH = stats.p1.unforcedErrorsBH, p1FH = stats.p1.unforcedErrorsFH,
            p2Total = stats.p2.unforcedErrorsBH + stats.p2.unforcedErrorsFH + stats.p2.doubleFaults, p2BH = stats.p2.unforcedErrorsBH, p2FH = stats.p2.unforcedErrorsFH
        )
    }
    item { 
        ComplexStatRow(
            label = stringResource(id = R.string.stat_forced_errors),
            p1Total = stats.p1.forcedErrorsBH + stats.p1.forcedErrorsFH, p1BH = stats.p1.forcedErrorsBH, p1FH = stats.p1.forcedErrorsFH,
            p2Total = stats.p2.forcedErrorsBH + stats.p2.forcedErrorsFH, p2BH = stats.p2.forcedErrorsBH, p2FH = stats.p2.forcedErrorsFH
        )
    }
    item { StatRow(stringResource(id = R.string.stat_aggressive_margin), formatVal(stats.p1.aggressiveMargin, stats.p1.aggressiveMargin != 0), formatVal(stats.p2.aggressiveMargin, stats.p2.aggressiveMargin != 0)) }

    stickyHeader { GroupHeader(stringResource(id = R.string.group_conversion), stats) }
    item { StatRow(stringResource(id = R.string.stat_2nd_serve_pts_won), formatPct(stats.p1.secondServePointsWonPercentage, stats.p1.totalServes - stats.p1.firstServesIn > 0), formatPct(stats.p2.secondServePointsWonPercentage, stats.p2.totalServes - stats.p2.firstServesIn > 0)) }
    item { StatRow(stringResource(id = R.string.stat_1st_serve_pts_won), formatPct(stats.p1.firstServePointsWonPercentage, stats.p1.firstServesIn > 0), formatPct(stats.p2.firstServePointsWonPercentage, stats.p2.firstServesIn > 0)) }
    item { StatRow(stringResource(id = R.string.stat_receiving_pts_won), formatPct(stats.p1.receivingPointsWonPercentage, stats.p1.totalPointsReceived > 0), formatPct(stats.p2.receivingPointsWonPercentage, stats.p2.totalPointsReceived > 0)) }
    item { StatRow(stringResource(id = R.string.stat_break_points), "${formatVal(stats.p1.breakPointsWon, stats.p1.breakPointsTotal > 0)}/${formatVal(stats.p1.breakPointsTotal, stats.p1.breakPointsTotal > 0)}", "${formatVal(stats.p2.breakPointsWon, stats.p2.breakPointsTotal > 0)}/${formatVal(stats.p2.breakPointsTotal, stats.p2.breakPointsTotal > 0)}") }
    item { StatRow(stringResource(id = R.string.stat_net_points), "${formatVal(stats.p1.netPointsWon, stats.p1.netPointsTotal > 0)}/${formatVal(stats.p1.netPointsTotal, stats.p1.netPointsTotal > 0)}", "${formatVal(stats.p2.netPointsWon, stats.p2.netPointsTotal > 0)}/${formatVal(stats.p2.netPointsTotal, stats.p2.netPointsTotal > 0)}") }
    item { StatRow(stringResource(id = R.string.stat_approach_points), "${formatVal(stats.p1.approachPointsWon, stats.p1.approachPointsTotal > 0)}/${formatVal(stats.p1.approachPointsTotal, stats.p1.approachPointsTotal > 0)}", "${formatVal(stats.p2.approachPointsWon, stats.p2.approachPointsTotal > 0)}/${formatVal(stats.p2.approachPointsTotal, stats.p2.approachPointsTotal > 0)}") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.byShotTab(stats: MatchStats) {
    val shotTypes = ShotType.entries
    
    shotTypes.forEachIndexed { index, type ->
        val labelRes = when(type) {
            ShotType.GROUND -> R.string.shot_ground
            ShotType.SLICE -> R.string.shot_slice
            ShotType.VOLLEY -> R.string.shot_volley
            ShotType.DROP -> R.string.shot_drop
            ShotType.SMASH -> R.string.shot_smash
            ShotType.LOB -> R.string.shot_lob
            ShotType.SWING -> R.string.shot_swing
        }
        
        stickyHeader { 
            val label = stringResource(id = labelRes)
            GroupHeader("${index + 1} - $label", stats) 
        }
        val s1 = stats.p1.shotStats[type] ?: ShotTypeStats()
        val s2 = stats.p2.shotStats[type] ?: ShotTypeStats()

        item { 
            ComplexStatRow(
                label = stringResource(id = R.string.stat_winners),
                p1Total = s1.winnersBH + s1.winnersFH, p1BH = s1.winnersBH, p1FH = s1.winnersFH,
                p2Total = s2.winnersBH + s2.winnersFH, p2BH = s2.winnersBH, p2FH = s2.winnersFH
            )
        }
        item { 
            ComplexStatRow(
                label = stringResource(id = R.string.stat_forced_errors),
                p1Total = s1.forcedErrorsBH + s1.forcedErrorsFH, p1BH = s1.forcedErrorsBH, p1FH = s1.forcedErrorsFH,
                p2Total = s2.forcedErrorsBH + s2.forcedErrorsFH, p2BH = s2.forcedErrorsBH, p2FH = s2.forcedErrorsFH
            )
        }
        item { 
            ComplexStatRow(
                label = stringResource(id = R.string.stat_unforced_errors),
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
                Text(text = stringResource(id = R.string.stats_score), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                
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
            PlayerScoreSummaryRow(state.totalStats.p1.playerName, state.completedSets, true, isInProgress, state)
            Spacer(modifier = Modifier.height(2.dp))
            PlayerScoreSummaryRow(state.totalStats.p2.playerName, state.completedSets, false, isInProgress, state)
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
private fun ComplexStatRow(label: String, p1Total: Int, p1BH: Int, p1FH: Int, p2Total: Int, p2BH: Int, p2FH: Int) {
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
            ComplexStatCell(total = p1Total, bh = p1BH, fh = p1FH, hasData = p1Total > 0)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ComplexStatCell(total = p2Total, bh = p2BH, fh = p2FH, hasData = p2Total > 0)
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
