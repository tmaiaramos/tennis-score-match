package com.tennis.matchscore

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tennis.matchscore.ui.format.MatchFormatScreen
import com.tennis.matchscore.ui.format.MatchFormatViewModel
import com.tennis.matchscore.ui.history.MatchHistoryScreen
import com.tennis.matchscore.ui.history.MatchHistoryViewModel
import com.tennis.matchscore.ui.home.HomeScreen
import com.tennis.matchscore.ui.match.MatchScreen
import com.tennis.matchscore.ui.match.MatchViewModel
import com.tennis.matchscore.ui.match.setup.NewMatchSetupScreen
import com.tennis.matchscore.ui.player.PlayerScreen
import com.tennis.matchscore.ui.player.PlayerViewModel
import com.tennis.matchscore.ui.theme.TennisMatchScoreTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val matchViewModel: MatchViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val matchFormatViewModel: MatchFormatViewModel by viewModels()
    private val matchHistoryViewModel: MatchHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TennisMatchScoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("home") }

                    when (currentScreen) {
                        "home" -> HomeScreen(
                            onNavigateToNewMatch = { currentScreen = "setup_match" },
                            onNavigateToPlayers = { currentScreen = "players" },
                            onNavigateToFormats = { currentScreen = "formats" },
                            onNavigateToHistory = { currentScreen = "history" }
                        )
                        "setup_match" -> NewMatchSetupScreen(
                            onStartMatch = { p1Id, p2Id, formatId, server, surface ->
                                matchViewModel.startNewMatch(
                                    player1Id = p1Id,
                                    player2Id = p2Id,
                                    formatId = formatId,
                                    initialServer = server,
                                    surface = surface,
                                    onMatchCreated = {
                                        currentScreen = "match"
                                    }
                                )
                            }
                        )
                        "match" -> MatchScreen(viewModel = matchViewModel)
                        "players" -> PlayerScreen(
                            viewModel = playerViewModel,
                            onBackClick = { currentScreen = "home" }
                        )
                        "formats" -> MatchFormatScreen(
                            viewModel = matchFormatViewModel,
                            onBackClick = { currentScreen = "home" }
                        )
                        "history" -> MatchHistoryScreen(
                            viewModel = matchHistoryViewModel,
                            onBackClick = { currentScreen = "home" }
                        )
                    }
                }
            }
        }
    }
}