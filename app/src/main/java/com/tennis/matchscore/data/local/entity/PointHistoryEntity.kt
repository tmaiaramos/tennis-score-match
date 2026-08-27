package com.tennis.matchscore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tennis.matchscore.domain.model.CourtPosition
import com.tennis.matchscore.domain.model.HitHand
import com.tennis.matchscore.domain.model.MatchEventType
import com.tennis.matchscore.domain.model.ServeState
import com.tennis.matchscore.domain.model.ShotType

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

    val eventType: MatchEventType = MatchEventType.REGULAR_POINT,
    val serveStateBefore: ServeState = ServeState.FIRST_SERVE,

    val scoreP1Before: String,
    val scoreP2Before: String,
    val gamesP1Before: Int,
    val gamesP2Before: Int,

    // Detalhamento estatístico (Modo Avançado)
    val winnerPosition: CourtPosition? = null,
    val winnerHitHand: HitHand? = null,
    val winnerShotType: ShotType? = null,
    val loserPosition: CourtPosition? = null,
    val isReturnEvent: Boolean = false,

    val timestamp: Long = System.currentTimeMillis()
)
