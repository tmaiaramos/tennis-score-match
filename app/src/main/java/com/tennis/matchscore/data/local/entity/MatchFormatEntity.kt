package com.tennis.matchscore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val REGULAR_TIEBREAK_POINTS = 7

@Entity(tableName = "match_formats")
data class MatchFormatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val numberOfSets: Int = 3,
    val gamesPerSet: Int = 6,
    val tieBreakAt: Int = 6,
    val hasAdvantage: Boolean = true, // true = Com Vantagem (Ad), false = Sem Vantagem (No-Ad)
    val hasSuperTieBreakInFinalSet: Boolean = true,
    val superTieBreakPoints: Int = 10,
    val isDefault: Boolean = false
) {
    fun isTieBreakSet(p1Games: Int, p2Games: Int, setNumber: Int): Boolean {
        val isFinalSet = setNumber == numberOfSets
        if (isFinalSet && hasSuperTieBreakInFinalSet) {
            return true
        }
        val maxGames = maxOf(p1Games, p2Games)
        val minGames = minOf(p1Games, p2Games)
        return maxGames == tieBreakAt + 1 && minGames == tieBreakAt
    }
}