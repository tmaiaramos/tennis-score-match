package com.tennis.matchscore.ui.match.setup

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.mutableLongStateOf
import com.tennis.matchscore.R
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
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_app_logo_png),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(id = R.string.screen_new_match_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.btn_cancel_back)
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
                Text(stringResource(id = R.string.menu_players), style = MaterialTheme.typography.titleMedium)

                PlayerDropdown(
                    label = stringResource(id = R.string.new_match_player1),
                    players = uiState.players,
                    selectedPlayer = uiState.player1,
                    onPlayerSelected = viewModel::onPlayer1Selected
                )

                PlayerDropdown(
                    label = stringResource(id = R.string.new_match_player2),
                    players = uiState.players.filter { it.id != uiState.player1?.id },
                    selectedPlayer = uiState.player2,
                    onPlayerSelected = viewModel::onPlayer2Selected
                )

                HorizontalDivider()

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.new_match_server), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val p1Name = uiState.player1?.let { "${it.firstName} ${it.lastName}".trim() } ?: stringResource(id = R.string.new_match_player1)
                        val p2Name = uiState.player2?.let { "${it.firstName} ${it.lastName}".trim() } ?: stringResource(id = R.string.new_match_player2)

                        FilterChip(
                            selected = uiState.initialServer == 1,
                            onClick = { viewModel.onInitialServerChanged(1) },
                            label = { 
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Text("🎾 $p1Name", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 11.sp)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = uiState.initialServer == 2,
                            onClick = { viewModel.onInitialServerChanged(2) },
                            label = { 
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Text("🎾 $p2Name", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 11.sp)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.new_match_detail_level), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScoringMode.entries.forEach { mode ->
                            val labelRes = when(mode) {
                                ScoringMode.BASIC -> R.string.scoring_basic
                                ScoringMode.INTERMEDIATE -> R.string.scoring_intermediate
                                ScoringMode.ADVANCED -> R.string.scoring_advanced
                            }
                            FilterChip(
                                selected = uiState.scoringMode == mode,
                                onClick = { viewModel.onScoringModeChanged(mode) },
                                label = { 
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                        Text(stringResource(id = labelRes), fontSize = 11.sp) 
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                HorizontalDivider()

                FormatDropdown(
                    formats = uiState.formats,
                    selectedFormat = uiState.selectedFormat,
                    onFormatSelected = viewModel::onFormatSelected
                )

                uiState.selectedFormat?.let { format ->
                    Text(
                        text = if (format.hasAdvantage) stringResource(id = R.string.mode_advantage) else stringResource(id = R.string.mode_no_advantage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp), // Ajustado para 24dp
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        OutlinedTextField(
                            value = dateFormatter.format(Date(selectedDateMillis)),
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text(stringResource(id = R.string.new_match_date)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = stringResource(id = R.string.new_match_date),
                                    modifier = Modifier.size(18.dp)
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
                                .height(60.dp)
                                .clickable { showDatePicker = true },
                            textStyle = TextStyle(fontSize = 13.sp) // Diminuído (Item 11)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(0.7f),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(id = R.string.new_match_court_type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp)) // Aumentado para 8dp
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Aumentado para 8dp
                            CourtSurface.entries.forEach { surface ->
                                val labelRes = when(surface) {
                                    CourtSurface.HARD -> R.string.court_hard
                                    CourtSurface.CLAY -> R.string.court_clay
                                }
                                FilterChip(
                                    selected = uiState.surface == surface,
                                    onClick = { viewModel.onSurfaceChanged(surface) },
                                    label = { 
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                            Text(stringResource(id = labelRes), fontSize = 11.sp) 
                                        }
                                    },
                                    modifier = Modifier.width(110.dp).height(32.dp), // Largura reduzida para 110.dp
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
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
                    Text(stringResource(id = R.string.new_match_start))
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
                        Text(stringResource(id = R.string.btn_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(id = R.string.rule_cancel))
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
    } ?: stringResource(id = R.string.new_match_select_player)

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
            value = selectedFormat?.name ?: stringResource(id = R.string.new_match_select_player),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.new_match_rule)) },
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
                                text = if (format.hasAdvantage) stringResource(id = R.string.rule_advantage) else "Sem Vantagem (No-Ad)",
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
