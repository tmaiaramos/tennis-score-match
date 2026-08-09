package com.tennis.matchscore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "point_history",
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
data class PointHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val matchId: Long,
    val setNumber: Int,
    val gameNumber: Int,
    val pointWinnerId: Long,
    val serverId: Long,

    // Placar antes deste ponto (usado para desfazer jogadas)
    val scoreP1Before: String,
    val scoreP2Before: String,
    val gamesP1Before: Int,
    val gamesP2Before: Int,

    val timestamp: Long = System.currentTimeMillis()
)