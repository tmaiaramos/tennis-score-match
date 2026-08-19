package com.tennis.matchscore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tennis.matchscore.domain.model.MatchEventType
import com.tennis.matchscore.domain.model.ServeState

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

    // Suporte ao modo Intermediário / Desfazer Falta
    val eventType: MatchEventType = MatchEventType.REGULAR_POINT,
    val serveStateBefore: ServeState = ServeState.FIRST_SERVE,

    // Placar antes deste evento (usado para desfazer jogadas)
    val scoreP1Before: String,
    val scoreP2Before: String,
    val gamesP1Before: Int,
    val gamesP2Before: Int,

    val timestamp: Long = System.currentTimeMillis()
)