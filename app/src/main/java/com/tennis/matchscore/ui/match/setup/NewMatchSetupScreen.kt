package com.tennis.matchscore.ui.match.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.mutableLongStateOf
import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import com.tennis.matchscore.data.local.entity.PlayerEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMatchSetupScreen(
    viewModel: NewMatchSetupViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onStartMatch: (player1Id: Long, player2Id: Long, formatId: Long, initialServer: Int, surface: String, scoringMode: String, dateTimestamp: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nova Partida") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
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

                PlayerDropdown(
                    label = "Jogador 1",
                    players = uiState.players,
                    selectedPlayer = uiState.player1,
                    onPlayerSelected = viewModel::onPlayer1Selected
                )

                PlayerDropdown(
                    label = "Jogador 2",
                    players = uiState.players.filter { it.id != uiState.player1?.id },
                    selectedPlayer = uiState.player2,
                    onPlayerSelected = viewModel::onPlayer2Selected
                )

                HorizontalDivider()

                Text("Sacador Inicial", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val p1Name = uiState.player1?.let { "${it.firstName} ${it.lastName}".trim() } ?: "Jogador 1"
                    val p2Name = uiState.player2?.let { "${it.firstName} ${it.lastName}".trim() } ?: "Jogador 2"

                    FilterChip(
                        selected = uiState.initialServer == 1,
                        onClick = { viewModel.onInitialServerChanged(1) },
                        label = { Text("🎾 $p1Name") },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = uiState.initialServer == 2,
                        onClick = { viewModel.onInitialServerChanged(2) },
                        label = { Text("🎾 $p2Name") },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                Text("Tipo de Marcação de Pontos", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScoringMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.scoringMode == mode,
                            onClick = { viewModel.onScoringModeChanged(mode) },
                            label = { Text(mode.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider()

                Text("Regras e Data da Partida", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = dateFormatter.format(Date(selectedDateMillis)),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Data da Partida") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Selecionar Data"
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                FormatDropdown(
                    formats = uiState.formats,
                    selectedFormat = uiState.selectedFormat,
                    onFormatSelected = viewModel::onFormatSelected
                )

                uiState.selectedFormat?.let { format ->
                    Text(
                        text = if (format.hasAdvantage) "• Modo: Com Vantagem (Ad)" else "• Modo: Sem Vantagem (No-Ad)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Text("Tipo de Quadra", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourtSurface.entries.forEach { surface ->
                        FilterChip(
                            selected = uiState.surface == surface,
                            onClick = { viewModel.onSurfaceChanged(surface) },
                            label = { Text(surface.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val p1 = uiState.player1 ?: return@Button
                        val p2 = uiState.player2 ?: return@Button
                        val fmt = uiState.selectedFormat ?: return@Button

                        onStartMatch(
                            p1.id,
                            p2.id,
                            fmt.id,
                            uiState.initialServer,
                            uiState.surface.name,
                            uiState.scoringMode.name,
                            selectedDateMillis
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isValid
                ) {
                    Text("Iniciar Partida")
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDateMillis
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { utcMillis ->
                                val zoneOffset = TimeZone.getDefault().getOffset(utcMillis)
                                selectedDateMillis = utcMillis - zoneOffset
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
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

    val selectedDisplayName = selectedPlayer?.let {
        "${it.firstName} ${it.lastName}".trim()
    } ?: "Selecione o jogador"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedDisplayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            players.forEach { player ->
                val fullPlayerName = "${player.firstName} ${player.lastName}".trim()
                DropdownMenuItem(
                    text = { Text(fullPlayerName) },
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

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedFormat?.name ?: "Selecione o formato",
            onValueChange = {},
            readOnly = true,
            label = { Text("Formato / Regra") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            formats.forEach { format ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(format.name, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (format.hasAdvantage) "Com Vantagem (Ad)" else "Sem Vantagem (No-Ad)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onFormatSelected(format)
                        expanded = false
                    }
                )
            }
        }
    }
}
