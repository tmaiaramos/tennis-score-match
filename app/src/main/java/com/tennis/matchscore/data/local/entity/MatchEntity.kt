package com.tennis.matchscore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tennis.matchscore.domain.model.CourtType
import com.tennis.matchscore.domain.model.MatchStatus
import com.tennis.matchscore.domain.model.ServeState
import com.tennis.matchscore.domain.model.TrackingLevel

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player1Id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player2Id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["winnerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = MatchFormatEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchFormatId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("player1Id"),
        Index("player2Id"),
        Index("winnerId"),
        Index("matchFormatId")
    ]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val player1Id: Long,
    val player2Id: Long,
    val matchFormatId: Long,
    val trackingLevel: TrackingLevel = TrackingLevel.BASIC,
    val courtType: CourtType = CourtType.HARD,

    // Estado atual do placar no Game e Set ativos
    val currentSet: Int = 1,
    val player1GamesCurrentSet: Int = 0,
    val player2GamesCurrentSet: Int = 0,
    val player1PointsCurrentGame: String = "0",
    val player2PointsCurrentGame: String = "0",

    val player1SetsWon: Int = 0,
    val player2SetsWon: Int = 0,

    val currentServerId: Long,
    val serveState: ServeState = ServeState.FIRST_SERVE,
    val isTieBreak: Boolean = false,
    val isSuperTieBreak: Boolean = false,

    val status: MatchStatus = MatchStatus.IN_PROGRESS,
    val winnerId: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)
