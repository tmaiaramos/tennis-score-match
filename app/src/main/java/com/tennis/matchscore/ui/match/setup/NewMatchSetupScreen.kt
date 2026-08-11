package com.tennis.matchscore.ui.match.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import com.tennis.matchscore.data.local.entity.PlayerEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMatchSetupScreen(
    viewModel: NewMatchSetupViewModel = hiltViewModel(),
    onStartMatch: (player1Id: Long, player2Id: Long, formatId: Long, initialServer: Int, surface: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nova Partida") }) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Jogadores", style = MaterialTheme.typography.titleMedium)

                // Seletor Jogador 1
                PlayerDropdown(
                    label = "Jogador 1 (Sacador inicial por padrão)",
                    players = uiState.players,
                    selectedPlayer = uiState.player1,
                    onPlayerSelected = viewModel::onPlayer1Selected
                )

                // Seletor Jogador 2
                PlayerDropdown(
                    label = "Jogador 2",
                    players = uiState.players.filter { it.id != uiState.player1?.id },
                    selectedPlayer = uiState.player2,
                    onPlayerSelected = viewModel::onPlayer2Selected
                )

                Divider()

                Text("Regras da Partida", style = MaterialTheme.typography.titleMedium)

                // Seletor de Formato
                FormatDropdown(
                    formats = uiState.formats,
                    selectedFormat = uiState.selectedFormat,
                    onFormatSelected = viewModel::onFormatSelected
                )

                // Piso da Quadra
                Text("Tipo de Piso", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourtSurface.values().forEach { surface ->
                        FilterChip(
                            selected = uiState.surface == surface,
                            onClick = { viewModel.onSurfaceChanged(surface) },
                            label = { Text(surface.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botão Iniciar Partida
                Button(
                    onClick = {
                        val p1 = uiState.player1 ?: return@Button
                        val p2 = uiState.player2 ?: return@Button
                        val fmt = uiState.selectedFormat ?: return@Button
                        onStartMatch(p1.id, p2.id, fmt.id, uiState.initialServer, uiState.surface.name)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isValid
                ) {
                    Text("Iniciar Partida")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDropdown(
    label: String,
    players: List<PlayerEntity>,
    selectedPlayer: PlayerEntity?,
    onPlayerSelected: (PlayerEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedPlayer?.firstName ?: "Selecione o jogador",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { Text(player.firstName) },
                    onClick = {
                        onPlayerSelected(player)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatDropdown(
    formats: List<MatchFormatEntity>,
    selectedFormat: MatchFormatEntity?,
    onFormatSelected: (MatchFormatEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedFormat?.name ?: "Selecione o formato",
            onValueChange = {},
            readOnly = true,
            label = { Text("Formato / Regra") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            formats.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format.name) },
                    onClick = {
                        onFormatSelected(format)
                        expanded = false
                    }
                )
            }
        }
    }
}