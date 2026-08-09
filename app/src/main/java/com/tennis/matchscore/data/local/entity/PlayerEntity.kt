package com.tennis.matchscore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tennis.matchscore.domain.model.DominantHand

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val dominantHand: DominantHand = DominantHand.RIGHT_HANDED
)