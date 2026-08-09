package com.tennis.matchscore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_formats")
data class MatchFormatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // Ex: "Set Único de 8 Games", "Melhor de 3 Sets (Padrão)", "Fast4"
    val numberOfSets: Int = 3, // Total máximo de sets (1, 3 ou 5)
    val gamesPerSet: Int = 6, // Games necessários para fechar o set (ex: 6 ou 8)
    val tieBreakAt: Int = 6, // Placar em que o Tie-Break é ativado (ex: 6x6 ou 8x8)
    val tieBreakPoints: Int = 7, // Pontos do Tie-Break normal (ex: 7 ou 10)
    val hasSuperTieBreakInFinalSet: Boolean = true, // Se o último set é substituído por Super Tie-Break
    val superTieBreakPoints: Int = 10, // Pontos do Super Tie-Break (geralmente 10)
    val isDefault: Boolean = false // Se é um formato padrão do sistema (não pode ser excluído)
)