package com.tennis.matchscore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_scores",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("matchId")]
)
data class SetScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val matchId: Long,
    val setNumber: Int, // 1, 2, 3, 4 ou 5
    val player1Games: Int,
    val player2Games: Int,
    val tieBreakPointsPlayer1: Int? = null, // Preenchido apenas se o set foi decidido no Tie-Break (ex: 7x6 (7-5))
    val tieBreakPointsPlayer2: Int? = null,
    val winnerPlayerId: Long? = null
)