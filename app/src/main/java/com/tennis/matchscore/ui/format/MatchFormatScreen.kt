package com.tennis.matchscore.ui.format

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tennis.matchscore.data.local.entity.MatchFormatEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchFormatScreen(
    viewModel: MatchFormatViewModel,
    onBackClick: () -> Unit
) {
    val formats by viewModel.formats.collectAsState()
    val context = LocalContext.current
    var showFormatDialog by remember { mutableStateOf(false) }
    var selectedFormatToEdit by remember { mutableStateOf<MatchFormatEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Regras & Formatos", fontWeight = FontWeight.Bold) },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedFormatToEdit = null
                    showFormatDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Nova Regra")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Formatos Disponíveis (${formats.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            if (formats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum formato cadastrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(formats, key = { it.id }) { format ->
                        FormatItemCard(
                            format = format,
                            onEdit = {
                                selectedFormatToEdit = format
                                showFormatDialog = true
                            },
                            onDelete = {
                                viewModel.deleteFormat(format) { canDelete ->
                                    if (!canDelete) {
                                        Toast.makeText(
                                            context,
                                            "Não é possível apagar: este formato está vinculado a partidas salvas.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showFormatDialog) {
            FormatFormDialog(
                initialFormat = selectedFormatToEdit,
                onDismiss = { showFormatDialog = false },
                onSave = { updatedOrNewFormat ->
                    if (updatedOrNewFormat.id == 0L) {
                        viewModel.saveFormat(
                            updatedOrNewFormat.name,
                            updatedOrNewFormat.numberOfSets,
                            updatedOrNewFormat.gamesPerSet,
                            updatedOrNewFormat.tieBreakAt,
                            updatedOrNewFormat.hasSuperTieBreakInFinalSet,
                            updatedOrNewFormat.superTieBreakPoints
                        )
                        showFormatDialog = false
                    } else {
                        viewModel.updateFormat(updatedOrNewFormat) { canUpdate ->
                            if (canUpdate) {
                                showFormatDialog = false
                            } else {
                                Toast.makeText(
                                    context,
                                    "Não é possível alterar: este formato está vinculado a partidas salvas.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FormatItemCard(
    format: MatchFormatEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = format.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (format.isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Padrão",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(text = "• Sets: ${format.numberOfSets} | Games por set: ${format.gamesPerSet}", fontSize = 13.sp)
            Text(text = "• Tie-Break em: ${format.tieBreakAt}-${format.tieBreakAt} (7 pts)", fontSize = 13.sp)

            if (format.hasSuperTieBreakInFinalSet) {
                Text(
                    text = "• Set Decisivo: Super Tie-Break de ${format.superTieBreakPoints} pontos",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun FormatFormDialog(
    initialFormat: MatchFormatEntity?,
    onDismiss: () -> Unit,
    onSave: (MatchFormatEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialFormat?.name ?: "") }
    var numberOfSets by remember { mutableStateOf(initialFormat?.numberOfSets?.toString() ?: "3") }
    var gamesPerSet by remember { mutableStateOf(initialFormat?.gamesPerSet?.toString() ?: "6") }
    var tieBreakAt by remember { mutableStateOf(initialFormat?.tieBreakAt?.toString() ?: "6") }
    var hasSuperTieBreak by remember { mutableStateOf(initialFormat?.hasSuperTieBreakInFinalSet ?: false) }
    var superTieBreakPoints by remember { mutableStateOf(initialFormat?.superTieBreakPoints?.toString() ?: "10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialFormat == null) "Novo Formato de Jogo" else "Editar Formato", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Regra *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numberOfSets,
                        onValueChange = { numberOfSets = it },
                        label = { Text("Qtd. Sets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = gamesPerSet,
                        onValueChange = {
                            gamesPerSet = it
                            // Sincroniza o gatilho padrão do Tie-break para bater com o valor dos games por set
                            if (initialFormat == null) {
                                tieBreakAt = it
                            }
                        },
                        label = { Text("Games/Set") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = tieBreakAt,
                    onValueChange = { tieBreakAt = it },
                    label = { Text("Tie-Break em") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = hasSuperTieBreak,
                        onCheckedChange = { hasSuperTieBreak = it }
                    )
                    Text("Super Tie-Break no set final", fontSize = 14.sp)
                }
                if (hasSuperTieBreak) {
                    OutlinedTextField(
                        value = superTieBreakPoints,
                        onValueChange = { superTieBreakPoints = it },
                        label = { Text("Pontos do Super Tie-Break") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sets = numberOfSets.toIntOrNull() ?: 3
                    val games = gamesPerSet.toIntOrNull() ?: 6
                    val tieAt = tieBreakAt.toIntOrNull() ?: games
                    val superPts = superTieBreakPoints.toIntOrNull() ?: 10

                    val targetFormat = initialFormat?.copy(
                        name = name.trim(),
                        numberOfSets = sets,
                        gamesPerSet = games,
                        tieBreakAt = tieAt,
                        hasSuperTieBreakInFinalSet = hasSuperTieBreak,
                        superTieBreakPoints = superPts
                    ) ?: MatchFormatEntity(
                        id = 0L,
                        name = name.trim(),
                        numberOfSets = sets,
                        gamesPerSet = games,
                        tieBreakAt = tieAt,
                        hasSuperTieBreakInFinalSet = hasSuperTieBreak,
                        superTieBreakPoints = superPts,
                        isDefault = false
                    )

                    onSave(targetFormat)
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