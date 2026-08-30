package com.tennis.matchscore.ui.format

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tennis.matchscore.R
import com.tennis.matchscore.data.local.entity.MatchFormatEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchFormatScreen(
    viewModel: MatchFormatViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val formats by viewModel.formats.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf<MatchFormatEntity?>(null) }

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
                        Text("Regras e Formatos", fontWeight = FontWeight.Bold)
                    }
                },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedFormat = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Formato")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Formatos de Partida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(formats, key = { it.id }) { format ->
                    FormatItemCard(
                        format = format,
                        onEditClick = {
                            selectedFormat = format
                            showDialog = true
                        },
                        onDeleteClick = { 
                            viewModel.deleteFormat(format) { /* Result ignored */ } 
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        FormatFormDialog(
            format = selectedFormat,
            onDismiss = { showDialog = false },
            onSave = { entity ->
                if (selectedFormat == null) {
                    viewModel.saveFormat(
                        name = entity.name,
                        numberOfSets = entity.numberOfSets,
                        gamesPerSet = entity.gamesPerSet,
                        tieBreakAt = entity.tieBreakAt,
                        hasAdvantage = entity.hasAdvantage,
                        hasSuperTieBreakInFinalSet = entity.hasSuperTieBreakInFinalSet,
                        superTieBreakPoints = entity.superTieBreakPoints
                    )
                } else {
                    viewModel.updateFormat(entity) { /* Result ignored */ }
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun FormatItemCard(
    format: MatchFormatEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = format.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "${format.numberOfSets} sets, ${format.gamesPerSet} games por set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = if (format.hasAdvantage) "Com Vantagem (Ad)" else "Sem Vantagem (No-Ad)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatFormDialog(
    format: MatchFormatEntity?,
    onDismiss: () -> Unit,
    onSave: (MatchFormatEntity) -> Unit
) {
    var name by remember { mutableStateOf(format?.name ?: "") }
    var numSets by remember { mutableIntStateOf(format?.numberOfSets ?: 3) }
    var gamesPerSet by remember { mutableIntStateOf(format?.gamesPerSet ?: 6) }
    var tieBreakAt by remember { mutableIntStateOf(format?.tieBreakAt ?: 6) }
    var hasAdvantage by remember { mutableStateOf(format?.hasAdvantage ?: true) }
    var hasSuperTieBreak by remember { mutableStateOf(format?.hasSuperTieBreakInFinalSet ?: true) }
    var superTieBreakPoints by remember { mutableIntStateOf(format?.superTieBreakPoints ?: 10) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (format == null) "Novo Formato" else "Editar Formato") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Formato") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sets:", modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = numSets == 1, onClick = { numSets = 1 })
                        Text("1", modifier = Modifier.padding(end = 8.dp))
                        RadioButton(selected = numSets == 3, onClick = { numSets = 3 })
                        Text("3", modifier = Modifier.padding(end = 8.dp))
                        RadioButton(selected = numSets == 5, onClick = { numSets = 5 })
                        Text("5")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Games/Set:", modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = gamesPerSet == 4, onClick = { gamesPerSet = 4; tieBreakAt = 3 })
                        Text("4", modifier = Modifier.padding(end = 8.dp))
                        RadioButton(selected = gamesPerSet == 6, onClick = { gamesPerSet = 6; tieBreakAt = 6 })
                        Text("6", modifier = Modifier.padding(end = 8.dp))
                        RadioButton(selected = gamesPerSet == 8, onClick = { gamesPerSet = 8; tieBreakAt = 8 })
                        Text("8")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasAdvantage, onCheckedChange = { hasAdvantage = it })
                    Text("Com Vantagem (Ad)")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasSuperTieBreak, onCheckedChange = { hasSuperTieBreak = it })
                    Text("Super Tie-Break no set final")
                }

                if (hasSuperTieBreak) {
                    OutlinedTextField(
                        value = superTieBreakPoints.toString(),
                        onValueChange = { superTieBreakPoints = it.toIntOrNull() ?: 10 },
                        label = { Text("Pontos Super Tie-Break") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        MatchFormatEntity(
                            id = format?.id ?: 0,
                            name = name,
                            numberOfSets = numSets,
                            gamesPerSet = gamesPerSet,
                            tieBreakAt = tieBreakAt,
                            hasAdvantage = hasAdvantage,
                            hasSuperTieBreakInFinalSet = hasSuperTieBreak,
                            superTieBreakPoints = superTieBreakPoints,
                            isDefault = format?.isDefault ?: false
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
