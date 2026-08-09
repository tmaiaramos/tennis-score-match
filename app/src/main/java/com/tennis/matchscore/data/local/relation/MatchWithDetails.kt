package com.tennis.matchscore.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.tennis.matchscore.data.local.entity.MatchEntity
import com.tennis.matchscore.data.local.entity.MatchFormatEntity
import com.tennis.matchscore.data.local.entity.PlayerEntity
import com.tennis.matchscore.data.local.entity.SetScoreEntity

data class MatchWithDetails(
    @Embedded val match: MatchEntity,

    @Relation(
        parentColumn = "matchFormatId",
        entityColumn = "id"
    )
    val format: MatchFormatEntity,

    @Relation(
        parentColumn = "player1Id",
        entityColumn = "id"
    )
    val player1: PlayerEntity,

    @Relation(
        parentColumn = "player2Id",
        entityColumn = "id"
    )
    val player2: PlayerEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "matchId"
    )
    val sets: List<SetScoreEntity>
)